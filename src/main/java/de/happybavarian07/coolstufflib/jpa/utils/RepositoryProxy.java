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
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class RepositoryProxy implements InvocationHandler {
    private final Class<?> repositoryInterface;
    private final SQLExecutor sqlExecutor;
    private final JavaPlugin plugin;
    private final TransactionManager transactionManager;
    private final EntityCache<Object, Object> entityCache;
    private String databasePrefix;
    private final EntityPersistenceHandler persistenceHandler;
    private final ElementCollectionHandler elementCollectionHandler;

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
            try {
                Method toStringMethod = proxy.getClass().getMethod("toString");
                if (!toStringMethod.getDeclaringClass().equals(Object.class)) {
                    return toStringMethod.invoke(proxy);
                }
            } catch (Exception ignored) {}
            return repositoryInterface.getName() + " Proxy for " + databasePrefix;
        }
        if (methodName.equals("hashCode") && (args == null || args.length == 0)) {
            try {
                Method hashCodeMethod = proxy.getClass().getMethod("hashCode");
                if (!hashCodeMethod.getDeclaringClass().equals(Object.class)) {
                    return hashCodeMethod.invoke(proxy);
                }
            } catch (Exception ignored) {}
            return System.identityHashCode(proxy);
        }
        if (methodName.equals("equals") && args != null && args.length == 1) {
            try {
                Method equalsMethod = proxy.getClass().getMethod("equals", Object.class);
                if (!equalsMethod.getDeclaringClass().equals(Object.class)) {
                    return equalsMethod.invoke(proxy, args[0]);
                }
            } catch (Exception ignored) {}
            return proxy == args[0];
        }
        if ("isDatabaseReady".equals(methodName)) {
            return sqlExecutor.getConnection(sqlExecutor.getDefaultConnection()) != null;
        }
        if (method.isAnnotationPresent(Transactional.class)) {
            return transactionManager.executeInTransaction(method, args, () -> invokeMethod(method, args));
        }
        return invokeMethod(method, args);
    }

    private Object invokeMethod(Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        if (methodName.endsWith("Async")) {
            return handleAsyncMethod(method, args);
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
        throw new UnsupportedOperationException("Method not implemented: " + methodName);
    }

    private CompletableFuture<?> handleAsyncMethod(Method method, Object[] args) {
        CompletableFuture<Object> future = new CompletableFuture<>();
        String syncMethodName = method.getName().replace("Async", "");
        Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
            try {
                Method syncMethod = findSyncMethod(syncMethodName, method.getParameterTypes());
                Object result = invoke(null, syncMethod, args);
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
            String tableName = getTableName(entityClass);
            if (methodName.startsWith("findBy") || methodName.startsWith("findAllBy")) {
                String fieldsPart = methodName.replaceFirst("find(All)?By", "");
                String[] fieldNames = fieldsPart.split("And");
                if (fieldNames.length == args.length) {
                    StringBuilder whereClause = new StringBuilder();
                    List<Object> queryArgs = new ArrayList<>();
                    for (int i = 0; i < fieldNames.length; i++) {
                        String javaFieldName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
                        Field field = null;
                        for (Field f : entityClass.getDeclaredFields()) {
                            if (f.getName().equalsIgnoreCase(javaFieldName)) {
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
                    if (method.getReturnType().isAssignableFrom(List.class)) {
                        return results;
                    } else if (!results.isEmpty()) {
                        return results.get(0);
                    } else {
                        return null;
                    }
                }
            }
            if ("findById".equals(methodName) && args.length == 1) {
                return findById(entityClass, args[0]);
            } else if ("findAll".equals(methodName) && (args == null || args.length == 0)) {
                return findAll(entityClass);
            } else if ("findAllById".equals(methodName) && args.length == 1) {
                return findAllById(entityClass, (Iterable<?>) args[0]);
            }
            throw new UnsupportedOperationException("Find method not implemented: " + methodName);
        } catch (Exception e) {
            throw new RuntimeException("Error in find method", e);
        }
    }

    private Object handleCountMethod(Method method, Object[] args) {
        try {
            Class<?> entityClass = getEntityClassFromRepository();
            String tableName = getTableName(entityClass);
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
        String tableName = getTableName(entityClass);
        String fieldsPart = method.getName().substring("countBy".length());
        String[] fieldNames = fieldsPart.split("And");
        if (args.length != fieldNames.length) {
            throw new UnsupportedOperationException("Argument count does not match field count for method: " + method.getName());
        }
        StringBuilder whereClause = new StringBuilder();
        for (int i = 0; i < fieldNames.length; i++) {
            String columnName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
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
        String tableName = getTableName(entityClass);
        String fieldsPart = method.getName().substring("countColumnsBy".length());
        String[] fieldNames = fieldsPart.split("And");
        if (args.length != fieldNames.length) {
            throw new UnsupportedOperationException("Argument count does not match field count for method: " + method.getName());
        }
        StringBuilder whereClause = new StringBuilder();
        List<Object> queryArgs = new ArrayList<>();
        for (int i = 0; i < fieldNames.length; i++) {
            String javaFieldName = Character.toLowerCase(fieldNames[i].charAt(0)) + fieldNames[i].substring(1);
            Field field = null;
            for (Field f : entityClass.getDeclaredFields()) {
                if (f.getName().equalsIgnoreCase(javaFieldName)) {
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
                return result != null;
            }
            return false;
        } catch (Exception e) {
            throw new RuntimeException("Error in exists method", e);
        }
    }

    private Object handleDeleteMethod(Method method, Object[] args) {
        Class<?> entityClass = getEntityClassFromRepository();
        if (args.length == 1) {
            return persistenceHandler.deleteEntity(entityClass, args[0]);
        }
        return null;
    }

    private Object handleSaveMethod(Method method, Object[] args) {
        try {
            if ("save".equals(method.getName()) && args.length == 1) {
                return save(getEntityClassFromRepository(), args[0]);
            } else if ("saveAll".equals(method.getName()) && args.length == 1) {
                return saveAll(getEntityClassFromRepository(), (Iterable<?>) args[0]);
            }
            throw new UnsupportedOperationException("Save method not implemented: " + method.getName());
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
        Type[] genericInterfaces = repositoryInterface.getGenericInterfaces();
        for (Type genericInterface : genericInterfaces) {
            if (genericInterface instanceof ParameterizedType paramType) {
                if (paramType.getRawType().equals(Repository.class)) {
                    return (Class<?>) paramType.getActualTypeArguments()[0];
                }
            }
        }
        throw new IllegalStateException("Cannot determine entity class from repository interface");
    }

    private String getTableName(Class<?> entityClass) {
        if (entityClass.isAnnotationPresent(Table.class)) {
            Table table = entityClass.getAnnotation(Table.class);
            String name = table != null ? table.name() : null;
            if (name != null && !name.isEmpty()) return name;
        }
        return entityClass.getSimpleName().toLowerCase();
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
                                updateEntity(entityClass, obj);
                                return obj;
                            } catch (Exception e) {
                                throw new RuntimeException("Error setting field value", e);
                            }
                        }
                    }
                }
            }
        }
        throw new UnsupportedOperationException("Set method not implemented: " + method.getName());
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
        throw new UnsupportedOperationException("Get method not implemented: " + method.getName());
    }

    private Object findById(Class<?> entityClass, Object id) {
        if (entityCache != null) {
            Optional<Object> cachedEntity = entityCache.getOptional(id);
            if (cachedEntity.isPresent()) {
                return cachedEntity;
            }
        }
        try {
            String tableName = getTableName(entityClass);
            String idColumn = getIdColumnName(entityClass);
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
            String tableName = getTableName(entityClass);
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

    private Object save(Class<?> entityClass, Object entity) {
        try {
            Object id = getEntityId(entity);
            Object savedEntity;
            if (id != null && findById(entityClass, id) instanceof Optional && ((Optional<?>) findById(entityClass, id)).isPresent()) {
                savedEntity = updateEntity(entityClass, entity);
            } else {
                savedEntity = insertEntity(entityClass, entity);
            }
            if (entityCache != null) {
                entityCache.put(getEntityId(savedEntity), savedEntity);
            }
            return savedEntity;
        } catch (Exception e) {
            throw new RuntimeException("Error saving entity", e);
        }
    }

    private Iterable<?> saveAll(Class<?> entityClass, Iterable<?> entities) {
        List<Object> savedEntities = new ArrayList<>();
        for (Object entity : entities) {
            savedEntities.add(save(entityClass, entity));
        }
        return savedEntities;
    }

    private void deleteById(Class<?> entityClass, Object id) {
        try {
            String tableName = getTableName(entityClass);
            String idColumn = getIdColumnName(entityClass);
            String sql = "DELETE FROM " + databasePrefix + tableName + " WHERE " + idColumn + " = ?";
            sqlExecutor.executeUpdate(sql, id);
            if (entityCache != null) {
                entityCache.remove(id);
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting entity by ID", e);
        }
    }

    private void delete(Class<?> entityClass, Object entity) {
        Object id = getEntityId(entity);
        if (id != null) {
            deleteById(entityClass, id);
        }
    }

    private void deleteAll(Class<?> entityClass) {
        try {
            String tableName = getTableName(entityClass);
            String sql = "DELETE FROM " + databasePrefix + tableName;
            sqlExecutor.executeUpdate(sql);
            if (entityCache != null) {
                entityCache.clear();
            }
        } catch (Exception e) {
            throw new RuntimeException("Error deleting all entities", e);
        }
    }

    private void deleteAll(Class<?> entityClass, Iterable<?> entities) {
        for (Object entity : entities) {
            delete(entityClass, entity);
        }
    }

    private void deleteAllById(Class<?> entityClass, Iterable<?> ids) {
        for (Object id : ids) {
            deleteById(entityClass, id);
        }
    }

    private Object insertEntity(Class<?> entityClass, Object entity) {
        try {
            invokeLifecycleMethod(entity, PrePersist.class);
            String tableName = getTableName(entityClass);
            List<String> columns = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    for (String columnName : getPossibleColumnNames(field)) {
                        columns.add(columnName);
                        values.add(field.get(entity));
                        break;
                    }
                }
            }
            String sql = "INSERT INTO " + databasePrefix + tableName + " (" +
                    String.join(", ", columns) + ") VALUES (" +
                    String.join(", ", Collections.nCopies(columns.size(), "?")) + ")";
            sqlExecutor.executeUpdate(sql, values.toArray());
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error inserting entity", e);
        }
    }

    private Object updateEntity(Class<?> entityClass, Object entity) {
        try {
            invokeLifecycleMethod(entity, PreUpdate.class);
            String tableName = getTableName(entityClass);
            String idColumn = getIdColumnName(entityClass);
            Object id = getEntityId(entity);
            List<String> setClauses = new ArrayList<>();
            List<Object> values = new ArrayList<>();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class) &&
                        !field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    for (String columnName : getPossibleColumnNames(field)) {
                        setClauses.add(columnName + " = ?");
                        values.add(field.get(entity));
                        break;
                    }
                }
            }
            values.add(id);
            String sql = "UPDATE " + databasePrefix + tableName + " SET " +
                    String.join(", ", setClauses) + " WHERE " + idColumn + " = ?";
            sqlExecutor.executeUpdate(sql, values.toArray());
            return entity;
        } catch (Exception e) {
            throw new RuntimeException("Error updating entity", e);
        }
    }

    private Object getEntityId(Object entity) {
        try {
            for (Field field : entity.getClass().getDeclaredFields()) {
                if (field.isAnnotationPresent(Id.class)) {
                    field.setAccessible(true);
                    return field.get(entity);
                }
            }
            return null;
        } catch (Exception e) {
            throw new RuntimeException("Error getting entity ID", e);
        }
    }

    private Object mapResultSetToEntity(ResultSet rs, Class<?> entityClass) {
        try {
            Object entity = entityClass.getDeclaredConstructor().newInstance();
            for (Field field : entityClass.getDeclaredFields()) {
                if (field.isAnnotationPresent(Column.class)) {
                    field.setAccessible(true);
                    Object value = null;
                    for (String columnName : getPossibleColumnNames(field)) {
                        try {
                            value = rs.getObject(columnName);
                            if (value != null) break;
                        } catch (SQLException ignored) {}
                    }
                    if (value != null) {
                        value = FieldTypeCaster.castToFieldType(field.getType(), value);
                        field.set(entity, value);
                    }
                }
            }
            loadRelationships(entity, entityClass);
            invokeLifecycleMethod(entity, PostLoad.class);
            if (entityCache != null) {
                Object id = getEntityId(entity);
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
        assert joinColumn != null;
        String joinColumnName = joinColumn.name();
        Class<?> relatedEntityClass = field.getType();
        String relatedTableName = getTableName(relatedEntityClass);
        String relatedIdColumn = getIdColumnName(relatedEntityClass);
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
        Class<?> relatedEntityClass = getGenericTypeFromField(field);
        String relatedTableName = getTableName(relatedEntityClass);
        String mappedByColumn = oneToMany.mappedBy();
        Object entityId = getEntityId(entity);
        if (entityId != null) {
            String sql = "SELECT * FROM " + databasePrefix + relatedTableName + " WHERE " + mappedByColumn + " = ?";
            addRelatedEntries(entity, field, entityId, relatedEntityClass, sql);
        }
    }

    private void loadManyToManyRelationship(Object entity, Field field) throws Exception {
        Object entityId = getEntityId(entity);
        if (entityId == null) return;
        Class<?> relatedEntityClass = getGenericTypeFromField(field);
        String entityTableName = getTableName(entity.getClass());
        String relatedTableName = getTableName(relatedEntityClass);
        String joinTableName = databasePrefix + entityTableName + "_" + relatedTableName;
        String entityIdColumn = entityTableName + "_id";
        String relatedIdColumn = relatedTableName + "_id";
        String relatedEntityIdColumn = getIdColumnName(relatedEntityClass);
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

    private Class<?> getGenericTypeFromField(Field field) {
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType paramType) {
            return (Class<?>) paramType.getActualTypeArguments()[0];
        }
        return Object.class;
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

    private String getIdColumnName(Class<?> entityClass) {
        for (Field field : entityClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Id.class)) {
                for (String name : getPossibleColumnNames(field)) {
                    return name;
                }
            }
        }
        throw new IllegalStateException("No @Id field found in entity class: " + entityClass.getName());
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
}
