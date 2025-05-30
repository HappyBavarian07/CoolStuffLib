package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenUtils;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

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
        Map<String, Object> result = new LinkedHashMap<>();
        if (obj == null) return result;
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value != null) {
                    if (value instanceof Map) {
                        result.put(field.getName(), convertMapToSimpleMap((Map<?, ?>) value));
                    } else {
                        result.put(field.getName(), value);
                    }
                }
            } catch (IllegalAccessException ignored) {
            }
        }
        return result;
    }

    private void applyMap(Group root, String path, Map<?, ?> map) {
        Group group = AutoGenUtils.getOrCreateGroupByPath(root, path);
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            String key = entry.getKey().toString();
            Object value = entry.getValue();
            if (value instanceof Map) {
                applyMap(group, key, (Map<?, ?>) value);
            } else {
                group.addKey(AutoGenUtils.createKey(key, value != null ? value.getClass() : Object.class, value));
            }
        }
    }

    private void applyObject(Group root, String path, Object obj) {
        Group group = AutoGenUtils.getOrCreateGroupByPath(root, path);
        for (Field field : obj.getClass().getDeclaredFields()) {
            field.setAccessible(true);
            try {
                Object value = field.get(obj);
                if (value instanceof Map) {
                    applyMap(group, field.getName(), (Map<?, ?>) value);
                } else {
                    group.addKey(AutoGenUtils.createKey(field.getName(), value != null ? value.getClass() : Object.class, value));
                }
            } catch (IllegalAccessException ignored) {
            }
        }
    }
}
