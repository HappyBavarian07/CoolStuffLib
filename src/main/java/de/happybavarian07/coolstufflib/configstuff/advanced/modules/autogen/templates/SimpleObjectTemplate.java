package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.*;

public class SimpleObjectTemplate implements AutoGenTemplate {
    private final String basePath;
    private final Object source;

    public SimpleObjectTemplate(String basePath, Object source) {
        this.basePath = basePath != null ? basePath : "";
        this.source = source;
    }

    @Override
    public String getBasePath() {
        return basePath;
    }

    @Override
    public Map<String, Object> toMap() {
        if (source instanceof Map) {
            return convertMapToSimpleMap((Map<?, ?>) source);
        }
        return convertObjectToMap(source);
    }

    @Override
    public void applyTo(Group root) {
        if (source instanceof Map) {
            applyMap(root, basePath, (Map<?, ?>) source);
        } else {
            applyObject(root, basePath, source);
        }
    }

    @Override
    public void writeToFile(File file) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("File cannot be null");
        }
        if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
            throw new IOException("Failed to create directories for file: " + file.getAbsolutePath());
        }

        try (FileWriter writer = new FileWriter(file)) {
            Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .disableHtmlEscaping()
                .create();

            writer.write(gson.toJson(toMap()));
        }
    }

    private Map<String, Object> convertMapToSimpleMap(Map<?, ?> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.put(key, convertMapToSimpleMap((Map<?, ?>) value));
            } else {
                result.put(key, value);
            }
        }
        return result;
    }

    private Map<String, Object> convertObjectToMap(Object obj) {
        if (obj == null) return Collections.emptyMap();

        Map<String, Object> result = new LinkedHashMap<>();
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);
                if (value != null) {
                    if (isComplexObject(value)) {
                        result.put(field.getName(), convertObjectToMap(value));
                    } else if (value instanceof Map) {
                        result.put(field.getName(), convertMapToSimpleMap((Map<?, ?>) value));
                    } else {
                        result.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException e) {
                // Skip fields that can't be accessed
            }
        }

        return result;
    }

    private boolean isComplexObject(Object obj) {
        if (obj == null) return false;

        Class<?> clazz = obj.getClass();
        String packageName = clazz.getPackage() != null ? clazz.getPackage().getName() : "";

        return !clazz.isPrimitive()
                && !packageName.startsWith("java.lang")
                && !packageName.startsWith("java.util")
                && !(obj instanceof Map)
                && !(obj instanceof Collection);
    }

    private void applyMap(Group root, String path, Map<?, ?> map) {
        Group group = path.isEmpty() ? root : AutoGenUtils.getOrCreateGroupByPath(root, path);

        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();

            String fullPath = path.isEmpty() ? key : path + "." + key;

            if (value instanceof Map) {
                applyMap(root, fullPath, (Map<?, ?>) value);
            } else {
                group.addKey(AutoGenUtils.createKey(
                    key,
                    value != null ? value.getClass() : Object.class,
                    value
                ));
            }
        }
    }

    private void applyObject(Group root, String path, Object obj) {
        if (obj == null) return;

        Group group = path.isEmpty() ? root : AutoGenUtils.getOrCreateGroupByPath(root, path);
        Class<?> clazz = obj.getClass();

        for (Field field : clazz.getDeclaredFields()) {
            try {
                field.setAccessible(true);
                Object value = field.get(obj);

                String key = field.getName();
                String fullPath = path.isEmpty() ? key : path + "." + key;

                if (value instanceof Map) {
                    applyMap(root, fullPath, (Map<?, ?>) value);
                } else if (isComplexObject(value)) {
                    applyObject(root, fullPath, value);
                } else {
                    group.addKey(AutoGenUtils.createKey(
                        key,
                        field.getType(),
                        value
                    ));
                }
            } catch (IllegalAccessException e) {
                // Skip fields that can't be accessed
            }
        }
    }
}
