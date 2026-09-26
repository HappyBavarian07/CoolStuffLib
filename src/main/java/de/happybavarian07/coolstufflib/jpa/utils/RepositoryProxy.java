package de.happybavarian07.coolstufflib.jpa.utils;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;
import de.happybavarian07.coolstufflib.jpa.annotations.*;
import de.happybavarian07.coolstufflib.jpa.cache.EntityCache;
import de.happybavarian07.coolstufflib.jpa.repository.Repository;
import de.happybavarian07.coolstufflib.jpa.transaction.TransactionManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepositoryProxy implements InvocationHandler {
    private final Class<?> repositoryInterface;
    private final SQLExecutor sqlExecutor;
    private final JavaPlugin plugin;
    private final TransactionManager transactionManager;
    private final EntityCache<Object, Object> entityCache;
    private final EntityPersistenceHandler persistenceHandler;
    private final ElementCollectionHandler elementCollectionHandler;
    private String databasePrefix;

    private RepositoryProxy(Class<?> repositoryInterface, String databasePrefix, SQLExecutor sqlExecutor, JavaPlugin plugin) {
        this.repositoryInterface = repositoryInterface;
        this.databasePrefix = databasePrefix;
        this.sqlExecutor = sqlExecutor;
        this.plugin = plugin;
        this.transactionManager = new TransactionManager(sqlExecutor);
        Class<?> entityClass = getEntityClassFromRepository();
        CacheConfig cacheConfig = entityClass.getAnnotation(CacheConfig.class);
        if (cacheConfig != null && cacheConfig.enabled()) {
            this.entityCache = new EntityCache<>(cacheConfig.maxSize());
        } else {
            this.entityCache = null;
        }
        this.elementCollectionHandler = new ElementCollectionHandler(sqlExecutor, databasePrefix);
        this.persistenceHandler = new EntityPersistenceHandler(sqlExecutor, databasePrefix, elementCollectionHandler);
    }

    @SuppressWarnings("unchecked")
    public static <T extends Repository<?, ?>> T create(Class<T> repositoryInterface, String databasePrefix, SQLExecutor sqlExecutor, JavaPlugin plugin) {
        return (T) Proxy.newProxyInstance(
                repositoryInterface.getClassLoader(),
                new Class[]{repositoryInterface},
                new RepositoryProxy(repositoryInterface, databasePrefix, sqlExecutor, plugin)
        );
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.equals("toString") && (args == null || args.length == 0)) {
            return repositoryInterface.getName() + " Proxy for " + databasePrefix;
        }
        if (methodName.equals("hashCode") && (args == null || args.length == 0)) {
            return System.identityHashCode(proxy);
        }
        if (methodName.equals("equals") && args != null && args.length == 1) {
            return proxy == args[0];
        }
        if ("isDatabaseReady".equals(methodName)) {
            Connection conn = null;
            try {
                conn = sqlExecutor.getConnection(sqlExecutor.getDefaultConnection());
                return conn != null && !conn.isClosed();
            } catch (SQLException e) {
                return false;
            } finally {
                if (conn != null) {
                    sqlExecutor.releaseConnection(sqlExecutor.getDefaultConnection(), conn);
                }
            }
        }
        if (method.isAnnotationPresent(Transactional.class)) {
            return transactionManager.executeInTransaction(method, args, () -> invokeMethod(proxy, method, args));
        }
        return invokeMethod(proxy, method, args);
    }

    private Object invokeMethod(Object proxy, Method method, Object[] args) throws Throwable {
        if (method.isDefault()) {
            return InvocationHandler.invokeDefault(proxy, method, args);
        }
        Query query = method.getAnnotation(Query.class);
        if (query != null) {
            return handleAnnotatedQuery(method, query, args);
        }
        String methodName = method.getName();
        if (methodName.endsWith("Async")) {
            return handleAsyncMethod(proxy, method, args);
        }
        if (methodName.startsWith("find")) {
            return handleFindMethod(method, args);
        } else if (methodName.startsWith("countBy")) {
            return handleCountByMethod(method, args);
        } else if (methodName.startsWith("countColumnsBy")) {
            return handleCountColumnsMethod(method, args);
        } else if (methodName.startsWith("count")) {
            return handleCountMethod(method, args);
        } else if (methodName.startsWith("exists")) {
            return handleExistsMethod(method, args);
        } else if (methodName.startsWith("get")) {
            return handleGetMethod(method, args);
        } else if (methodName.startsWith("set")) {
            return handleSetMethod(method, args);
        } else if (methodName.startsWith("update")) {
            return handleUpdateMethod(method, args);
        } else if (methodName.startsWith("insert")) {
            return handleInsertMethod(method, args);
        } else if (methodName.startsWith("delete")) {
            return handleDeleteMethod(method, args);
        } else if ("save".equals(methodName) || "saveAll".equals(methodName)) {
            return handleSaveMethod(method, args);
        } else if ("query".equals(methodName)) {
            return handleQueryMethod(method, args);
        }
        throw new UnsupportedOperationException("Repository method " + repositoryInterface.getSimpleName() + "." + methodName
                + " matches no supported name pattern (find, count, exists, get, set, update, insert, delete, save, saveAll, query)");
    }

    private static final Pattern QUERY_PARAMETER = Pattern.compile("\\?(\\d+)?");

    /** Runs the SQL of an {@link Query} method; see that annotation for the supported forms. */
    private Object handleAnnotatedQuery(Method method, Query query, Object[] args) throws Exception {
        Class<?> entityClass = getEntityClassFromRepository();
        Object[] callArgs = args == null ? new Object[0] : args;
        String template = query.value().replace("{table}", databasePrefix + EntityReflectionUtil.getTableName(entityClass));
        Matcher matcher = QUERY_PARAMETER.matcher(template);
        StringBuilder sql = new StringBuilder();
        List<Object> params = new ArrayList<>();
        int nextArgument = 0;
        while (matcher.find()) {
            int index = matcher.group(1) == null ? nextArgument++ : Integer.parseInt(matcher.group(1)) - 1;
            if (index < 0 || index >= callArgs.length) {
                throw new IllegalArgumentException("@Query on " + method.getName() + " uses parameter " + (index + 1)
                        + " but the method has " + callArgs.length);
            }
            params.add(callArgs[index]);
            matcher.appendReplacement(sql, "?");
        }
        matcher.appendTail(sql);

        Class<?> returnType = method.getReturnType();
        String statement = sql.toString().trim().toLowerCase(Locale.ROOT);
        if (!statement.startsWith("select") && !statement.startsWith("with")) {
            int changed = sqlExecutor.executeUpdate(sql.toString(), params.toArray());
            if (entityCache != null) entityCache.clear();
            if (returnType == int.class || returnType == Integer.class) return changed;
            if (returnType == long.class || returnType == Long.class) return (long) changed;
            if (returnType == boolean.class || returnType == Boolean.class) return changed > 0;
            return null;
        }

        boolean many = Iterable.class.isAssignableFrom(returnType);
        boolean optional = returnType == Optional.class;
        Class<?> element = returnType;
        if ((many || optional) && method.getGenericReturnType() instanceof ParameterizedType type
                && type.getActualTypeArguments()[0] instanceof Class<?> argument) {
            element = argument;
        } else if (many || optional) {
            element = entityClass;
        }
        List<Object> rows = new ArrayList<>();
        try (ResultSet rs = sqlExecutor.executeQuery(sql.toString(), params.toArray())) {
            while (rs.next()) {
                rows.add(element == entityClass ? mapResultSetToEntity(rs, entityClass)
                        : FieldTypeCaster.castToFieldType(element, rs.getObject(1)));
                if (!many) break;
            }
        }
        if (many) return rows;
        Object first = rows.isEmpty() ? null : rows.get(0);
        if (optional) return Optional.ofNullable(first);
        if (first == null && returnType.isPrimitive()) {
            throw new IllegalStateException("@Query on " + method.getName() + " returned no row for a primitive result");
        }
        return first;
    }

    private CompletableFuture<?> handleAsyncMethod(Object proxy, Method method, Object[] args) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        String syncMethodName = method.getName().replace("Async", "");
        org.bukkit.Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Method syncMethod = findSyncMethod(syncMethodName, method.getParameterTypes());
                Object result = invoke(proxy, syncMethod, args);
                future.complete(result);
            } catch (Throwable e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private Method findSyncMethod(String methodName, Class<?>[] asyncParamTypes) throws NoSuchMethodException {
        Class<?>[] syncParamTypes = new Class<?>[asyncParamTypes.length];
        for (int i = 0; i < asyncParamTypes.length; i++) {
            if (asyncParamTypes[i] == CompletableFuture.class) {
                syncParamTypes[i] = Object.class;
            } else {
                syncParamTypes[i] = asyncParamTypes[i];
            }
        }
        return repositoryInterface.getMethod(methodName, syncParamTypes);
    }

    private Object handleFindMethod(Method method, Object[] args) {
        try {
            String methodName = method.getName();
            Class<?> entityClass = getEntityClassFromRepository();
            String tableName = EntityReflectionUtil.getTableName(entityClass);
            if ("findById".equals(methodName) && args.length == 1) {
                return findById(entityClass, args[0]);
            } else if ("findAll".equals(methodName) && (args == null || args.length == 0)) {
                return findAll(entityClass);
            } else if ("findAllById".equals(methodName) && args.length == 1) {
                return findAllById(entityClass, (Iterable<?>) args[0]);
            }
            if (methodName.startsWith("findBy") || methodName.startsWith("findAllBy")) {
                List<Object> results = findByFields(entityClass, methodName.replaceFirst("find(All)?By", ""), args);
                if (results == null) return null;
                if (method.getReturnType().isAssignableFrom(List.class)) return results;
                return results.isEmpty() ? null : results.get(0);
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error in find method", e);
        }
    }

    /** Entities whose fields (e.g. {@code NameAndCoins}) equal the arguments, or null if the argument count differs. */
    private List<Object> findByFields(Class<?> entityClass, String fieldsPart, Object[] args) throws Exception {
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        String[] fieldNames = fieldsPart.split("And");
        if (fieldNames.length != (args == null ? 0 : args.length)) return null;
        StringBuilder whereClause = new StringBuilder();
        List<Object> queryArgs = new ArrayList<>();
        for (int i = 0; i < fieldNames.length; i++) {
            String javaFieldName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
            if (javaFieldName.equals("id")) {
                javaFieldName = EntityReflectionUtil.getIdColumnName(entityClass);
            }
            Field field = null;
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(javaFieldName)) {
                    field = f;
                    break;
                }
                Column col = f.getAnnotation(Column.class);
                if (col != null && !col.name().isEmpty() && col.name().equalsIgnoreCase(javaFieldName)) {
                    field = f;
                    break;
                }
            }
            if (field == null) throw new RuntimeException("Field not found: " + javaFieldName);
            List<String> possibleNames = getPossibleColumnNames(field);
            String columnName = possibleNames.get(0);
            if (i > 0) whereClause.append(" AND ");
            whereClause.append(columnName).append(" = ?");
            queryArgs.add(args[i]);
        }
        String sql = "SELECT * FROM " + databasePrefix + tableName + " WHERE " + whereClause;
        List<Object> results = new ArrayList<>();
        try (ResultSet rs = sqlExecutor.executeQuery(sql, queryArgs.toArray())) {
            while (rs.next()) {
                Object entity = mapResultSetToEntity(rs, entityClass);
                results.add(entity);
            }
        }
        return results;
    }

    private Object handleCountMethod(Method method, Object[] args) {
        try {
            Class<?> entityClass = getEntityClassFromRepository();
            String tableName = EntityReflectionUtil.getTableName(entityClass);
            String sql = "SELECT COUNT(*) FROM " + databasePrefix + tableName;
            try (ResultSet rs = sqlExecutor.executeQuery(sql)) {
                if (rs.next()) {
                    Object count = rs.getLong(1);
                    return FieldTypeCaster.castToFieldType(method.getReturnType(), count);
                }
                return FieldTypeCaster.castToFieldType(method.getReturnType(), 0L);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error in count method", e);
        }
    }

    private Object handleCountByMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        String fieldsPart = method.getName().substring("countBy".length());
        String[] fieldNames = fieldsPart.split("And");
        if (args.length != fieldNames.length) {
            throw new IllegalArgumentException("Argument count does not match field count for method: " + method.getName());
        }
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < fieldNames.length; i++) {
            String columnName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
            if (columnName.equals("id")) {
                columnName = EntityReflectionUtil.getIdColumnName(entityClass);
            }
            if (i > 0) whereClause.append(" AND ");
            whereClause.append(columnName).append(" = ?");
        }
        String sql = "SELECT COUNT(*) FROM " + databasePrefix + tableName + " WHERE " + whereClause;
        try (ResultSet rs = sqlExecutor.executeQuery(sql, args)) {
            if (rs.next()) {
                Object count = rs.getLong(1);
                return FieldTypeCaster.castToFieldType(method.getReturnType(), count);
            }
            return FieldTypeCaster.castToFieldType(method.getReturnType(), 0L);
        } catch (Exception e) {
            throw new RuntimeException("Error in countBy method", e);
        }
    }

    private Object handleCountColumnsMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        String tableName = EntityReflectionUtil.getTableName(entityClass);
        String fieldsPart = method.getName().substring("countColumnsBy".length());
        String[] fieldNames = fieldsPart.split("And");
        if (args.length != fieldNames.length) {
            throw new IllegalArgumentException("Argument count does not match field count for method: " + method.getName());
        }
        StringBuilder whereClause = new StringBuilder();
        List<Object> queryArgs = new ArrayList<>();
        for (int i = 0; i < fieldNames.length; i++) {
            String javaFieldName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
            if (javaFieldName.equals("id")) {
                javaFieldName = EntityReflectionUtil.getIdColumnName(entityClass);
            }
            Field field = null;
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(javaFieldName)) {
                    field = f;
                    break;
                }
                Column col = f.getAnnotation(Column.class);
                if (col != null && !col.name().isEmpty() && col.name().equalsIgnoreCase(javaFieldName)) {
                    field = f;
                    break;
                }
            }
            if (field == null) throw new RuntimeException("Field not found: " + javaFieldName);
            List<String> possibleNames = getPossibleColumnNames(field);
            String columnName = possibleNames.get(0);
            if (i > 0) whereClause.append(" AND ");
            whereClause.append(columnName).append(" = ?");
            queryArgs.add(args[i]);
        }
        String sql = "SELECT COUNT(*) FROM " + databasePrefix + tableName + " WHERE " + whereClause;
        try (ResultSet rs = sqlExecutor.executeQuery(sql, queryArgs.toArray())) {
            if (rs.next()) {
                Object count = rs.getLong(1);
                return FieldTypeCaster.castToFieldType(method.getReturnType(), count);
            }
            return FieldTypeCaster.castToFieldType(method.getReturnType(), 0L);
        } catch (Exception e) {
            throw new RuntimeException("Error in countColumns method", e);
        }
    }

    private Object handleExistsMethod(Method method, Object[] args) {
        try {
            if ("existsById".equals(method.getName()) && args.length == 1) {
                Object result = findById(getEntityClassFromRepository(), args[0]);
                return result != null && (result instanceof Optional<?> opt ? opt.isPresent() : true);
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error in exists method", e);
        }
    }

    private Object handleDeleteMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        String methodName = method.getName();
        int argCount = args == null ? 0 : args.length;
        try {
            if ("delete".equals(methodName) && argCount == 1) {
                deleteEntity(entityClass, args[0]);
            } else if ("deleteById".equals(methodName) && argCount == 1) {
                findEntityById(entityClass, args[0]).ifPresent(entity -> deleteEntity(entityClass, entity));
            } else if ("deleteAllById".equals(methodName) && argCount == 1) {
                for (Object id : (Iterable<?>) args[0]) {
                    findEntityById(entityClass, id).ifPresent(entity -> deleteEntity(entityClass, entity));
                }
            } else if ("deleteAll".equals(methodName) && argCount <= 1) {
                Iterable<?> entities = argCount == 0 ? findAll(entityClass) : (Iterable<?>) args[0];
                for (Object entity : entities) deleteEntity(entityClass, entity);
            } else if (methodName.startsWith("deleteBy") || methodName.startsWith("deleteAllBy")) {
                List<Object> matches = findByFields(entityClass, methodName.replaceFirst("delete(All)?By", ""), args);
                if (matches == null) {
                    throw new UnsupportedOperationException("Repository method " + methodName + " has " + argCount
                            + " parameters but names a different number of fields");
                }
                for (Object entity : matches) deleteEntity(entityClass, entity);
            } else {
                throw new UnsupportedOperationException("Repository method " + repositoryInterface.getSimpleName() + "."
                        + methodName + " is not a supported delete method");
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Error in delete method " + methodName, e);
        }
        return null;
    }

    private void deleteEntity(Class<?> entityClass, Object entity) {
        persistenceHandler.deleteEntity(entityClass, entity);
        if (entityCache != null) {
            entityCache.remove(EntityReflectionUtil.getEntityId(entity));
        }
    }

    private Optional<?> findEntityById(Class<?> entityClass, Object id) {
        Object result = findById(entityClass, id);
        return result instanceof Optional<?> optional ? optional : Optional.ofNullable(result);
    }

    private Object handleSaveMethod(Method method, Object[] args) {
        try {
            if ("save".equals(method.getName()) && args.length == 1) {
                Class<?> entityClass = getEntityClassFromRepository();
                Object entity = args[0];
                Object id = EntityReflectionUtil.getEntityId(entity);
                boolean exists = false;
                if (id != null) {
                    Object existing = findById(entityClass, id);
                    exists = (existing instanceof Optional<?> opt) && opt.isPresent();
                }
                Object savedEntity = exists ? persistenceHandler.updateEntity(entityClass, entity) : persistenceHandler.insertEntity(entityClass, entity);
                if (entityCache != null) {
                    entityCache.put(EntityReflectionUtil.getEntityId(savedEntity), savedEntity);
                }
                return savedEntity;
            } else if ("saveAll".equals(method.getName()) && args.length == 1) {
                Class<?> entityClass = getEntityClassFromRepository();
                List<Object> savedEntities = new ArrayList<>();
                for (Object entity : (Iterable<?>) args[0]) {
                    Object id = EntityReflectionUtil.getEntityId(entity);
                    boolean exists = false;
                    if (id != null) {
                        Object existing = findById(entityClass, id);
                        exists = (existing instanceof Optional<?> opt) && opt.isPresent();
                    }
                    Object saved = exists ? persistenceHandler.updateEntity(entityClass, entity) : persistenceHandler.insertEntity(entityClass, entity);
                    if (entityCache != null) {
                        entityCache.put(EntityReflectionUtil.getEntityId(saved), saved);
                    }
                    savedEntities.add(saved);
                }
                return savedEntities;
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error in save method", e);
        }
    }

    private Object handleQueryMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        return new EntityQueryBuilder<>(entityClass, sqlExecutor, databasePrefix);
    }

    public void setDatabasePrefix(String prefix) {
        this.databasePrefix = prefix;
    }

    private Class<?> getEntityClassFromRepository() {
        Class<?> entityClass = findEntityClass(repositoryInterface);
        if (entityClass == null) {
            throw new IllegalStateException("Cannot determine entity class from repository interface");
        }
        return entityClass;
    }

    /** Walks the interface hierarchy, so {@code AsyncRepository<E, ID>} and deeper sub-interfaces work too. */
    private static Class<?> findEntityClass(Class<?> type) {
        for (Type genericInterface : type.getGenericInterfaces()) {
            Class<?> raw = genericInterface instanceof ParameterizedType paramType
                    ? (Class<?>) paramType.getRawType() : (Class<?>) genericInterface;
            if (!Repository.class.isAssignableFrom(raw)) continue;
            if (genericInterface instanceof ParameterizedType paramType
                    && paramType.getActualTypeArguments()[0] instanceof Class<?> entityClass) {
                return entityClass;
            }
            Class<?> found = findEntityClass(raw);
            if (found != null) return found;
        }
        return null;
    }

    private Object handleInsertMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        if (args.length == 1) {
            return persistenceHandler.insertEntity(entityClass, args[0]);
        }
        return null;
    }

    private Object handleUpdateMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        if (args.length == 1) {
            return persistenceHandler.updateEntity(entityClass, args[0]);
        }
        return null;
    }

    private Object handleSetMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        if (args.length == 2) {
            Object entity = findById(entityClass, args[0]);
            if (entity instanceof Optional<?> opt && opt.isPresent()) {
                Object obj = opt.get();
                String fieldName = method.getName().substring(3);
                for (Field field : entityClass.getDeclaredFields()) {
                    for (String name : getPossibleColumnNames(field)) {
                        if (name.equalsIgnoreCase(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1))) {
                            field.setAccessible(true);
                            try {
                                field.set(obj, args[1]);
                                persistenceHandler.updateEntity(entityClass, obj);
                                return obj;
                            } catch (Exception e) {
                                throw new RuntimeException("Error setting field value", e);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Object handleGetMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        if (args.length == 1) {
            Object entity = findById(entityClass, args[0]);
            if (entity instanceof Optional<?> opt && opt.isPresent()) {
                Object obj = opt.get();
                String fieldName = method.getName().substring(3);
                for (Field field : entityClass.getDeclaredFields()) {
                    for (String name : getPossibleColumnNames(field)) {
                        if (name.equalsIgnoreCase(Character.toLowerCase(fieldName.charAt(0)) + fieldName.substring(1))) {
                            field.setAccessible(true);
                            try {
                                return field.get(obj);
                            } catch (Exception e) {
                                throw new RuntimeException("Error getting field value", e);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private Object findById(Class<?> entityClass, Object id) {
        if (entityCache != null) {
            Optional<Object> cachedEntity = entityCache.getOptional(id);
            if (cachedEntity.isPresent()) {
                return cachedEntity;
            }
        }
        try {
            String tableName = EntityReflectionUtil.getTableName(entityClass);
            String idColumn = EntityReflectionUtil.getIdColumnName(entityClass);
            String sql = "SELECT * FROM " + databasePrefix + tableName + " WHERE " + idColumn + " = ?";
            try (ResultSet rs = sqlExecutor.executeQuery(sql, id)) {
                if (rs.next()) {
                    Object entity = mapResultSetToEntity(rs, entityClass);
                    if (entityCache != null) {
                        entityCache.put(id, entity);
                    }
                    return Optional.of(entity);
                }
                return Optional.empty();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error finding entity by ID", e);
        }
    }

    private Iterable<?> findAll(Class<?> entityClass) {
        try {
            String tableName = EntityReflectionUtil.getTableName(entityClass);
            String sql = "SELECT * FROM " + databasePrefix + tableName;
            List<Object> results = new ArrayList<>();
            try (ResultSet rs = sqlExecutor.executeQuery(sql)) {
                while (rs.next()) {
                    results.add(mapResultSetToEntity(rs, entityClass));
                }
            }
            return results;
        } catch (Exception e) {
            throw new RuntimeException("Error finding all entities", e);
        }
    }

    private Iterable<?> findAllById(Class<?> entityClass, Iterable<?> ids) {
        List<Object> results = new ArrayList<>();
        for (Object id : ids) {
            Object entity = findById(entityClass, id);
            if (entity instanceof Optional && ((Optional<?>) entity).isPresent()) {
                results.add(((Optional<?>) entity).get());
            } else if (!(entity instanceof Optional)) {
                results.add(entity);
            }
        }
        return results;
    }

    private Object mapResultSetToEntity(ResultSet rs, Class<?> entityClass) {
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class) || field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    Object value = null;
                    for (String columnName : getPossibleColumnNames(field)) {
                        try {
                            String col = columnName;
                            if (field.isAnnotationPresent(Column.class)) {
                                String ann = field.getAnnotation(Column.class).name();
                                if (ann != null && !ann.isEmpty()) col = ann;
                            }
                            value = rs.getObject(col);
                            if (value != null) break;
                        } catch (SQLException ignored) {
                        }
                    }
                    if (value != null) {
                        value = FieldTypeCaster.castToFieldType(field.getType(), value);
                        field.set(entity, value);
                    }
                }
            }
            elementCollectionHandler.loadCollections(entity, entityClass);
            loadRelationships(entity, entityClass);
            invokeLifecycleMethod(entity, PostLoad.class);
            if (entityCache != null) {
                Object id = EntityReflectionUtil.getEntityId(entity);
                entityCache.put(id, entity);
            }
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error mapping ResultSet to entity", e);
        }
    }

    private void invokeLifecycleMethod(Object entity, Class<?> lifecycleAnnotation) {
        for (Method method : entity.getClass().getDeclaredMethods()) {
            if (method.isAnnotationPresent((Class<? extends Annotation>) lifecycleAnnotation)) {
                try {
                    method.setAccessible(true);
                    method.invoke(entity);
                } catch (Exception e) {
                    throw new RuntimeException("Error invoking lifecycle method: " + method.getName(), e);
                }
            }
        }
    }

    private void loadRelationships(Object entity, Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                if (field.isAnnotationPresent(ManyToOne.class)) {
                    loadManyToOneRelationship(entity, field);
                } else if (field.isAnnotationPresent(OneToMany.class)) {
                    loadOneToManyRelationship(entity, field);
                } else if (field.isAnnotationPresent(ManyToMany.class)) {
                    loadManyToManyRelationship(entity, field);
                }
            } catch (Exception e) {
                throw new RuntimeException("Error loading relationship for field: " + field.getName(), e);
            }
        }
    }

    private void loadManyToOneRelationship(Object entity, Field field) throws Exception {
        if (!field.isAnnotationPresent(JoinColumn.class)) {
            return;
        }
        JoinColumn joinColumn = field.getAnnotation(JoinColumn.class);
        String joinColumnName = joinColumn.name();
        Class<?> relatedEntityClass = field.getType();
        String relatedTableName = EntityReflectionUtil.getTableName(relatedEntityClass);
        String relatedIdColumn = EntityReflectionUtil.getIdColumnName(relatedEntityClass);
        Object foreignKeyValue = getFieldValue(entity, joinColumnName);
        if (foreignKeyValue != null) {
            String sql = "SELECT * FROM " + databasePrefix + relatedTableName + " WHERE " + relatedIdColumn + " = ?";
            try (ResultSet rs = sqlExecutor.executeQuery(sql, foreignKeyValue)) {
                if (rs.next()) {
                    Object relatedEntity = mapResultSetToEntity(rs, relatedEntityClass);
                    field.set(entity, relatedEntity);
                }
            }
        }
    }

    private void loadOneToManyRelationship(Object entity, Field field) throws Exception {
        OneToMany oneToMany = field.getAnnotation(OneToMany.class);
        if (oneToMany == null || oneToMany.mappedBy() == null || oneToMany.mappedBy().isEmpty()) {
            return;
        }
        Class<?> relatedEntityClass = EntityReflectionUtil.getGenericTypeFromField(field);
        String relatedTableName = EntityReflectionUtil.getTableName(relatedEntityClass);
        String mappedByColumn = oneToMany.mappedBy();
        Object entityId = EntityReflectionUtil.getEntityId(entity);
        if (entityId != null) {
            String sql = "SELECT * FROM " + databasePrefix + relatedTableName + " WHERE " + mappedByColumn + " = ?";
            addRelatedEntries(entity, field, entityId, relatedEntityClass, sql);
        }
    }

    private void loadManyToManyRelationship(Object entity, Field field) throws Exception {
        Object entityId = EntityReflectionUtil.getEntityId(entity);
        if (entityId == null) return;
        Class<?> relatedEntityClass = EntityReflectionUtil.getGenericTypeFromField(field);
        String entityTableName = EntityReflectionUtil.getTableName(entity.getClass());
        String relatedTableName = EntityReflectionUtil.getTableName(relatedEntityClass);
        String joinTableName = databasePrefix + entityTableName + "_" + relatedTableName;
        String entityIdColumn = entityTableName + "_id";
        String relatedIdColumn = relatedTableName + "_id";
        String relatedEntityIdColumn = EntityReflectionUtil.getIdColumnName(relatedEntityClass);
        String sql = "SELECT r.* FROM " + databasePrefix + relatedTableName + " r " +
                "INNER JOIN " + joinTableName + " j ON r." + relatedEntityIdColumn + " = j." + relatedIdColumn + " " +
                "WHERE j." + entityIdColumn + " = ?";
        addRelatedEntries(entity, field, entityId, relatedEntityClass, sql);
    }

    private void addRelatedEntries(Object entity, Field field, Object entityId, Class<?> relatedEntityClass, String sql) throws SQLException, IllegalAccessException {
        List<Object> relatedEntities = new ArrayList<>();
        try (ResultSet rs = sqlExecutor.executeQuery(sql, entityId)) {
            while (rs.next()) {
                Object relatedEntity = mapResultSetToEntity(rs, relatedEntityClass);
                relatedEntities.add(relatedEntity);
            }
        }
        field.set(entity, relatedEntities);
    }

    private Object getFieldValue(Object entity, String fieldName) throws Exception {
        for (Field field : entity.getClass().getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                for (String name : getPossibleColumnNames(field)) {
                    if (name.equals(fieldName)) {
                        field.setAccessible(true);
                        return field.get(entity);
                    }
                }
            }
        }
        return null;
    }

    private List<String> getPossibleColumnNames(Field field) {
        List<String> names = new ArrayList<>();
        if (field.isAnnotationPresent(Column.class)) {
            String annotated = Objects.requireNonNull(field.getAnnotation(Column.class)).name();
            if (!annotated.isEmpty()) names.add(annotated);
        }
        String camel = field.getName();
        names.add(camel);
        StringBuilder snake = new StringBuilder();
        for (int i = 0; i < camel.length(); i++) {
            char c = camel.charAt(i);
            if (Character.isUpperCase(c)) {
                snake.append('_').append(Character.toLowerCase(c));
            } else {
                snake.append(c);
            }
        }
        String snakeStr = snake.toString();
        if (!snakeStr.equals(camel)) names.add(snakeStr);
        return names;
    }


    public void initializeSchema() {
        Class<?> entityClass = getEntityClassFromRepository();
        try {
            sqlExecutor.generateSchema(entityClass);
            elementCollectionHandler.createCollectionTables(entityClass);
        } catch (Exception e) {
            throw new RuntimeException("Error initializing schema for entity: " + entityClass.getName(), e);
        }
    }

    /**
     * <p>Retrieves the transaction manager instance.</p>
     *
     * @return The {@link TransactionManager} instance
     */
    public TransactionManager getTransactionManager() {
        return transactionManager;
    }
}
