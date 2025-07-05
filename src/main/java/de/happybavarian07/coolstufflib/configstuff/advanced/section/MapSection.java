package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

import java.util.*;

public class MapSection extends BaseConfigSection {
    public MapSection(String name) {
        this(name, null);
    }

    public MapSection(String name, ConfigSection parent) {
        super(name, parent);
    }

    public void putAll(Map<String, ?> values) {
        if (values != null) {
            for (Map.Entry<String, ?> entry : values.entrySet()) {
                set(entry.getKey(), entry.getValue());
            }
        }
    }

    public void put(String key, Object value) {
        set(key, value);
    }

    public Object getValue(String key) {
        return get(key);
    }

    public <T> T getValue(String key, Class<T> type) {
        Object value = get(key);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public <T> T getValue(String key, T defaultValue, Class<T> type) {
        T value = getValue(key, type);
        return value != null ? value : defaultValue;
    }

    public boolean containsKey(String key) {
        return contains(key);
    }

    public boolean containsValue(Object value) {
        for (Object v : getValueStore().values()) {
            if (Objects.equals(v, value)) return true;
        }
        return false;
    }

    public void removeValue(String key) {
        remove(key);
    }

    public int size() {
        return getValueStore().size();
    }

    public boolean isEmpty() {
        return getValueStore().isEmpty();
    }

    @Override
    public void clear() {
        super.clear();
    }

    public Map<String, Object> getMapValues() {
        return Collections.unmodifiableMap(getValueStore().getValues());
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.putAll(getMapValues());
        return map;
    }

    @Override
    public Set<Object> toSet() {
        return new HashSet<>(getValueStore().values());
    }

    @Override
    public List<Object> toList() {
        return new ArrayList<>(getValueStore().values());
    }

    @Override
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("__type__", "MapSection");
        map.putAll(getMapValues());
        return map;
    }

    @Override
    public void fromMap(Map<String, Object> values) {
        clear();
        if (values == null) {
            return;
        }
        for (Map.Entry<String, ?> entry : values.entrySet()) {
            set(entry.getKey(), entry.getValue());
        }
    }

    @Override
    public void set(String path, Object value) {
        if (path == null || path.isEmpty()) {
            if (value == null) {
                clear();
                return;
            } else if (value instanceof Map<?, ?> m) {
                fromMap((Map<String, Object>) m);
                return;
            } else if (value instanceof BaseConfigSection section) {
                section.setParent(this);
                super.set(path, section);
                return;
            }
            return;
        }
        if (value == null) {
            remove(path);
            return;
        }
        super.set(path, value);
    }

    @Override
    public void merge(ConfigSection other) {
        super.merge(other);
        if (other instanceof MapSection otherMap) {
            putAll(otherMap.getMapValues());
        }
    }
}
