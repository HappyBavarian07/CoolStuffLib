package de.happybavarian07.coolstufflib.service.util;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.service.annotation.ServiceComponent;
import de.happybavarian07.coolstufflib.service.api.Config;
import de.happybavarian07.coolstufflib.service.api.Service;
import de.happybavarian07.coolstufflib.service.api.ServiceRegistry;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.JarURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServiceComponentScanner {
    private static final Logger LOGGER = Logger.getLogger(ServiceComponentScanner.class.getName());

    /**
     * Scans with this library's class loader (inside a plugin that is the plugin's class loader) and the thread's
     * context class loader.
     */
    public static List<Class<?>> findAnnotatedServices(String packageName) {
        Set<ClassLoader> loaders = new LinkedHashSet<>();
        loaders.add(ServiceComponentScanner.class.getClassLoader());
        ClassLoader context = Thread.currentThread().getContextClassLoader();
        if (context != null) loaders.add(context);
        Set<String> seenClassNames = new HashSet<>();
        List<Class<?>> result = new ArrayList<>();
        for (ClassLoader loader : loaders) {
            scan(loader, packageName, result, seenClassNames);
        }
        return result;
    }

    private static void scan(ClassLoader loader, String packageName, List<Class<?>> result, Set<String> seenClassNames) {
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = loader.getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                    File dir = new File(decodedPath);
                    if (dir.exists() && dir.isDirectory()) {
                        scanDirectoryForClasses(loader, dir, packageName, result, seenClassNames);
                    }
                } else if ("jar".equals(protocol)) {
                    try {
                        JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                        JarFile jarFile = jarConn.getJarFile();
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(path + "/") && name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.replace('/', '.').substring(0, name.length() - 6);
                                if (seenClassNames.add(className)) {
                                    addIfAnnotated(loader, className, result);
                                }
                            }
                        }
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Could not scan " + resource + " for services", e);
                    }
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Could not scan package " + packageName + " for services", e);
        }
    }

    private static void addIfAnnotated(ClassLoader loader, String className, List<Class<?>> result) {
        try {
            Class<?> clazz = Class.forName(className, false, loader);
            if (clazz.isAnnotationPresent(ServiceComponent.class)) {
                result.add(clazz);
            }
        } catch (ClassNotFoundException | LinkageError e) {
            LOGGER.log(Level.FINE, "Skipping " + className + " while scanning for services", e);
        }
    }

    private static void scanDirectoryForClasses(ClassLoader loader, File dir, String packageName, List<Class<?>> result, Set<String> seenClassNames) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectoryForClasses(loader, file, packageName + "." + file.getName(), result, seenClassNames);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                if (seenClassNames.add(className)) {
                    addIfAnnotated(loader, className, result);
                }
            }
        }
    }

    /**
     * <p>Resolves the ID of an annotated service. An explicit {@link ServiceComponent#uuid()} wins, otherwise the ID is
     * derived from the service name, so the same annotation always yields the same ID. This is the same derivation
     * {@link de.happybavarian07.coolstufflib.service.api.ServiceDescriptor#of(String)} uses, which keeps the annotated
     * path and the descriptor path on one ID per service name.</p>
     *
     * @param meta the annotation of the service class
     * @return the service ID
     */
    public static UUID resolveServiceId(ServiceComponent meta) {
        if (meta == null) throw new IllegalArgumentException("ServiceComponent annotation cannot be null");
        if (!meta.uuid().isEmpty()) return UUID.fromString(meta.uuid());
        return UUID.nameUUIDFromBytes(meta.serviceName().getBytes(StandardCharsets.UTF_8));
    }

    public static Service createInstance(Class<?> clazz, ServiceRegistry registry, Config config) {
        Constructor<?>[] constructors = clazz.getConstructors();
        for (Constructor<?> ctor : constructors) {
            Class<?>[] paramTypes = ctor.getParameterTypes();
            Object[] params = new Object[paramTypes.length];
            boolean canInject = true;
            for (int i = 0; i < paramTypes.length; i++) {
                if (Config.class.isAssignableFrom(paramTypes[i])) {
                    params[i] = config;
                } else {
                    Service dep = findDependency(paramTypes[i], registry);
                    if (dep == null) {
                        canInject = false;
                        break;
                    }
                    params[i] = dep;
                }
            }
            if (canInject) {
                try {
                    Service instance = (Service) ctor.newInstance(params);
                    injectSetters(instance, registry, config);
                    return instance;
                } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        try {
            Service instance = (Service) clazz.getDeclaredConstructor().newInstance();
            injectSetters(instance, registry, config);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void injectSetters(Service instance, ServiceRegistry registry, Config config) {
        Method[] methods = instance.getClass().getMethods();
        for (Method m : methods) {
            if (m.getName().startsWith("set") && m.getParameterCount() == 1) {
                Class<?> paramType = m.getParameterTypes()[0];
                try {
                    if (Config.class.isAssignableFrom(paramType)) {
                        m.invoke(instance, config);
                    } else if (Service.class.isAssignableFrom(paramType)) {
                        Service dep = findDependency(paramType, registry);
                        if (dep != null) m.invoke(instance, dep);
                    }
                } catch (Exception e) {
                    CoolStuffLib.logError("Failed to inject " + m.getName() + " into service "
                            + instance.getClass().getName(), e);
                }
            }
        }
    }

    private static Service findDependency(Class<?> type, ServiceRegistry registry) {
        for (UUID id : registry.snapshotStates().keySet()) {
            Service s = registry.get(id).orElse(null);
            if (s != null && type.isInstance(s)) return s;
        }
        return null;
    }
}
