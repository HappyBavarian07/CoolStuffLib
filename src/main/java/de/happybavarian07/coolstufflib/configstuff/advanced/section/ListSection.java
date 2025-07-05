package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

import java.util.*;

public class ListSection extends BaseConfigSection {
    public ListSection(String name) {
        this(name, null);
    }

    public ListSection(String name, ConfigSection parent) {
        super(name, parent);
        if (parent instanceof BaseConfigSection) {
            ((BaseConfigSection) parent).getMutableSubSections().put(name, this);
        }
    }

    public void add(Object value) {
        if (value != null) {
            set(Integer.toString(size()), value);
        }
    }

    public void addAll(Collection<?> values) {
        if (values != null) {
            int start = size();
            int i = 0;
            for (Object v : values) {
                set(Integer.toString(start + i), v);
                i++;
            }
        }
    }

    public void set(int index, Object value) {
        if (index >= 0) {
            if (value == null) {
                remove(index);
            } else {
                super.set(Integer.toString(index), value);
            }
        }
    }

    public Object get(int index) {
        return get(Integer.toString(index));
    }

    public <T> T get(int index, Class<T> type) {
        Object value = get(index);
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return type.cast(value);
        }
        return null;
    }

    public <T> T get(int index, T defaultValue, Class<T> type) {
        T value = get(index, type);
        return value != null ? value : defaultValue;
    }

    public void remove(int index) {
        String key = Integer.toString(index);
        if (contains(key)) {
            remove(key);
            // Shift all subsequent elements down
            int sz = size();
            for (int i = index + 1; i <= sz; i++) {
                Object v = get(Integer.toString(i));
                set(Integer.toString(i - 1), v);
            }
            remove(Integer.toString(sz));
        }
    }

    public int size() {
        int max = -1;
        for (String key : getValueStore().keySet()) {
            try {
                int idx = Integer.parseInt(key);
                if (idx > max) max = idx;
            } catch (NumberFormatException ignored) {}
        }
        return max + 1;
    }

    public boolean isEmpty() {
        return size() == 0;
    }

    public void clear() {
        super.clear();
    }

    public List<Object> getItems() {
        List<Object> items = new ArrayList<>();
        int sz = size();
        for (int i = 0; i < sz; i++) {
            Object item = get(i);
            if (item != null) {
                items.add(item);
            }
        }
        return items;
    }

    public <T> List<T> getItemsAs(Class<T> type) {
        List<T> items = new ArrayList<>();
        int sz = size();
        for (int i = 0; i < sz; i++) {
            Object item = get(i);
            if (type.isInstance(item)) {
                items.add(type.cast(item));
            }
        }
        return items;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> map = super.toMap();
        map.put("__type__", "ListSection");
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

    @Override
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("__type__", "ListSection");
        map.put("__items", new ArrayList<>(getItems()));
        return map;
    }

    public void fromList(List<?> list) {
        clear();
        if (list != null) {
            int i = 0;
            for (Object v : list) {
                set(Integer.toString(i), v);
                i++;
            }
        }
    }

    @Override
    public void merge(ConfigSection other) {
        if (other == null) return;
        for (String key : other.getValueStore().keySet()) {
            try {
                Integer.parseInt(key);
            } catch (NumberFormatException e) {
                set(key, other.get(key));
            }
        }
        if (other instanceof ListSection otherList) {
            for (Object item : otherList.getItems()) {
                add(item);
            }
        }
    }

    @Override
    public List<?> getList(String path) {
        if (path == null || path.isEmpty() || "__items".equals(path)) {
            return getItems();
        }
        return super.getList(path);
    }

    @Override
    public void set(String path, Object value) {
        if (path == null || path.isEmpty()) {
            if (value == null) {
                clear();
                return;
            } else if (value instanceof List<?> l) {
                fromList(l);
                return;
            } else if (value instanceof BaseConfigSection section) {
                section.setParent(this);
                super.set(path, section);
                return;
            }
            return;
        }
        try {
            int index = Integer.parseInt(path);
            set(index, value);
        } catch (NumberFormatException e) {
            super.set(path, value);
        }
    }

    @Override
    public boolean contains(String pathOrValue) {
        for (Object item : getItems()) {
            if (item instanceof ConfigSection section) {
                if (section.contains(pathOrValue)) {
                    return true;
                }
            } else if (Objects.equals(item, pathOrValue)) {
                return true;
            }
        }
        return super.contains(pathOrValue);
    }
}
