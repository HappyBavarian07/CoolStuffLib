package de.happybavarian07.coolstufflib.service.util;

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

public class ServiceComponentScanner {
    public static List<Class<?>> findAnnotatedServices(String packageName) {
        Set<String> seenClassNames = new HashSet<>();
        List<Class<?>> result = new ArrayList<>();
        String path = packageName.replace('.', '/');
        try {
            Enumeration<URL> resources = Thread.currentThread().getContextClassLoader().getResources(path);
            while (resources.hasMoreElements()) {
                URL resource = resources.nextElement();
                String protocol = resource.getProtocol();
                if ("file".equals(protocol)) {
                    String decodedPath = URLDecoder.decode(resource.getFile(), StandardCharsets.UTF_8);
                    File dir = new File(decodedPath);
                    if (dir.exists() && dir.isDirectory()) {
                        scanDirectoryForClasses(dir, packageName, result, seenClassNames);
                    }
                } else if ("jar".equals(protocol)) {
                    try {
                        JarURLConnection jarConn = (JarURLConnection) resource.openConnection();
                        JarFile jarFile = jarConn.getJarFile();
                        Enumeration<JarEntry> entries = jarFile.entries();
                        while (entries.hasMoreElements()) {
                            JarEntry entry = entries.nextElement();
                            String name = entry.getName();
                            if (name.startsWith(path) && name.endsWith(".class") && !entry.isDirectory()) {
                                String className = name.replace('/', '.').substring(0, name.length() - 6);
                                if (seenClassNames.add(className)) {
                                    try {
                                        Class<?> clazz = Class.forName(className);
                                        if (clazz.isAnnotationPresent(ServiceComponent.class)) {
                                            result.add(clazz);
                                        }
                                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                                    }
                                }
                            }
                        }
                    } catch (IOException ignored) {
                    }
                }
            }
        } catch (IOException ignored) {
        }
        return result;
    }

    private static void scanDirectoryForClasses(File dir, String packageName, List<Class<?>> result, Set<String> seenClassNames) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File file : files) {
            if (file.isDirectory()) {
                scanDirectoryForClasses(file, packageName + "." + file.getName(), result, seenClassNames);
            } else if (file.getName().endsWith(".class")) {
                String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
                if (seenClassNames.add(className)) {
                    try {
                        Class<?> clazz = Class.forName(className);
                        if (clazz.isAnnotationPresent(ServiceComponent.class)) {
                            result.add(clazz);
                        }
                    } catch (ClassNotFoundException | NoClassDefFoundError ignored) {
                    }
                }
            }
        }
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
                } catch (Exception ignored) {
                }
            }
        }
    }

    private static Service findDependency(Class<?> type, ServiceRegistry registry) {
        for (String id : registry.snapshotStates().keySet()) {
            Service s = registry.get(id).orElse(null);
            if (s != null && type.isInstance(s)) return s;
        }
        return null;
    }
}
