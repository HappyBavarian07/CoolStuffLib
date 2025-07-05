package de.happybavarian07.coolstufflib.configstuff.advanced.section.value;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SectionValueStore {
    private final Map<String, Object> values;

    public SectionValueStore() {
        this.values = new ConcurrentHashMap<>();
    }

    public Object get(String key) {
        if (key == null || key.isEmpty()) return null;
        if (!key.contains(".")) return values.get(key);
        int idx = key.indexOf('.');
        String first = key.substring(0, idx);
        String rest = key.substring(idx + 1);
        Object sub = values.get(first);
        if (sub instanceof SectionValueStore) {
            return ((SectionValueStore) sub).get(rest);
        }
        return null;
    }

    public void set(String key, Object value) {
        if (key == null || key.isEmpty()) return;
        if (value instanceof SectionValueStore) return;
        if (!key.contains(".")) {
            if (value == null) values.remove(key);
            else values.put(key, value);
            return;
        }
        int idx = key.indexOf('.');
        String first = key.substring(0, idx);
        String rest = key.substring(idx + 1);
        Object sub = values.get(first);
        SectionValueStore store;
        if (sub instanceof SectionValueStore) {
            store = (SectionValueStore) sub;
        } else {
            store = new SectionValueStore();
            values.put(first, store);
        }
        store.set(rest, value);
    }

    public boolean contains(String key) {
        if (key == null || key.isEmpty()) return false;
        if (!key.contains(".")) return values.containsKey(key);
        int idx = key.indexOf('.');
        String first = key.substring(0, idx);
        String rest = key.substring(idx + 1);
        Object sub = values.get(first);
        if (sub instanceof SectionValueStore) {
            return ((SectionValueStore) sub).contains(rest);
        }
        return false;
    }

    public void remove(String key) {
        if (key == null || key.isEmpty()) return;
        if (!key.contains(".")) {
            values.remove(key);
            return;
        }
        int idx = key.indexOf('.');
        String first = key.substring(0, idx);
        String rest = key.substring(idx + 1);
        Object sub = values.get(first);
        if (sub instanceof SectionValueStore) {
            ((SectionValueStore) sub).remove(rest);
        }
    }

    public void clear() {
        values.clear();
    }

    public Map<String, Object> getValues() {
        return values;
    }

    public <T> T getValue(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        if (type == String.class) {
            return type.cast(value.toString());
        } else if (type == Integer.class || type == int.class) {
            if (value instanceof Number) {
                return type.cast(((Number) value).intValue());
            } else {
                return type.cast(Integer.valueOf(value.toString()));
            }
        } else if (type == Boolean.class || type == boolean.class) {
            if (value instanceof Boolean) {
                return type.cast(value);
            } else {
                return type.cast(Boolean.valueOf(value.toString()));
            }
        } else if (type == Double.class || type == double.class) {
            if (value instanceof Number) {
                return type.cast(((Number) value).doubleValue());
            } else {
                return type.cast(Double.valueOf(value.toString()));
            }
        } else if (type == Long.class || type == long.class) {
            if (value instanceof Number) {
                return type.cast(((Number) value).longValue());
            } else {
                return type.cast(Long.valueOf(value.toString()));
            }
        }
        return null;
    }

    public <T> T getValue(String key, T defaultValue, Class<T> type) {
        T value = getValue(key, type);
        return value != null ? value : defaultValue;
    }

    public <T> Optional<T> getOptionalValue(String key, Class<T> type) {
        return Optional.ofNullable(getValue(key, type));
    }

    public Set<String> keySet() {
        return values.keySet();
    }

    public Set<Map.Entry<String, Object>> entrySet() {
        return values.entrySet();
    }

    public Collection<Object> values() {
        return values.values();
    }

    public void put(String key, Object value) {
        values.put(key, value);
    }

    public int size() {
        return values.size();
    }

    public void copyFrom(SectionValueStore other) {
        if (other == null) return;
        clear();
        for (Map.Entry<String, Object> entry : other.values.entrySet()) {
            Object value = entry.getValue();
            Object copiedValue;
            if (value instanceof SectionValueStore store) {
                SectionValueStore newStore = new SectionValueStore();
                newStore.copyFrom(store);
                copiedValue = newStore;
            } else if (value instanceof BaseConfigSection section) {
                copiedValue = section.clone();
            } else if (value instanceof Map<?, ?> map) {
                copiedValue = new ConcurrentHashMap<>(map);
            } else if (value instanceof Collection<?> collection) {
                copiedValue = new java.util.ArrayList<>(collection);
            } else {
                copiedValue = value;
            }
            values.put(entry.getKey(), copiedValue);
        }
    }

    public SectionValueStore deepClone() {
        SectionValueStore clone = new SectionValueStore();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            Object value = entry.getValue();
            Object clonedValue;
            if (value instanceof SectionValueStore store) {
                clonedValue = store.deepClone();
            } else if (value instanceof Map<?, ?> map) {
                clonedValue = new ConcurrentHashMap<>(map);
            } else if (value instanceof Collection<?> collection) {
                clonedValue = new java.util.ArrayList<>(collection);
            } else {
                clonedValue = value;
            }
            clone.values.put(entry.getKey(), clonedValue);
        }
        return clone;
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }
}
