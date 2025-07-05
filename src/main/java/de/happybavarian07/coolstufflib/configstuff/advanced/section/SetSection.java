package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

import java.util.*;

public class SetSection extends BaseConfigSection {
    public SetSection(String name) {
        this(name, null);
    }

    public SetSection(String name, ConfigSection parent) {
        super(name, parent);
        if (parent instanceof BaseConfigSection) {
            ((BaseConfigSection) parent).getMutableSubSections().put(name, this);
        }
    }

    public void add(Object value) {
        if (value != null) {
            set(value.toString(), value);
        }
    }

    public void remove(Object value) {
        remove(value.toString());
    }

    public boolean contains(Object value) {
        return contains(value.toString());
    }

    public int size() {
        return getValueStore().size();
    }

    public boolean isEmpty() {
        return getValueStore().isEmpty();
    }

    public void clear() {
        super.clear();
    }

    public Set<Object> getItems() {
        return new HashSet<>(getValueStore().values());
    }

    @Override
    public void set(String path, Object value) {
        if (path == null || path.isEmpty()) {
            if (value == null) {
                clear();
                return;
            } else if (value instanceof Set<?> s) {
                fromSet(new HashSet<>(s));
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
        if (other instanceof SetSection otherSet) {
            for (Object item : otherSet.getItems()) {
                add(item);
            }
        }
    }

    @Override
    public List<?> getList(String path) {
        if (path == null || path.isEmpty()) {
            return new ArrayList<>(getItems());
        }
        return super.getList(path);
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("__type__", "SetSection");
        map.put("__items", new ArrayList<>(getItems()));
        return map;
    }

    @Override
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("__type__", "SetSection");
        map.put("__items", new ArrayList<>(getItems()));
        return map;
    }

    @Override
    public List<Object> toList() {
        return new ArrayList<>(getItems());
    }

    @Override
    public Set<Object> toSet() {
        return new HashSet<>(getItems());
    }

    public void fromSet(Set<Object> items) {
        clear();
        if (items != null) {
            for (Object v : items) {
                add(v);
            }
        }
    }
}
