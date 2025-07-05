package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionFactory;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionHierarchyManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionSerializationHelper;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.meta.SectionCommentManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.meta.SectionMetadataManager;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.value.SectionValueStore;

import java.util.*;

public class BaseConfigSection implements ConfigSection {
    private final String name;
    private SectionValueStore valueStore;
    private final SectionSerializationHelper serializationHelper;
    private final SectionFactory sectionFactory;
    private SectionHierarchyManager sectionHierarchyManager;
    private SectionMetadataManager metadataManager;
    private SectionCommentManager commentManager;
    private ConfigSection parent;

    public BaseConfigSection(String name) {
        this(name, null);
    }

    public BaseConfigSection(String name, ConfigSection parent) {
        this.name = name;
        this.parent = parent;
        if (parent != null) {
            if (parent instanceof BaseConfigSection baseParent) {
                baseParent.getSectionHierarchyManager().getMutableSubSections().put(name, this);
            }
        }
        this.sectionHierarchyManager = new SectionHierarchyManager(this);
        this.valueStore = new SectionValueStore();
        this.metadataManager = new SectionMetadataManager();
        this.commentManager = new SectionCommentManager();
        this.serializationHelper = new SectionSerializationHelper(this);
        this.sectionFactory = new SectionFactory(this);
    }

    private static List<Object> deepCloneList(List<?> list) {
        List<Object> clonedList = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof BaseConfigSection sectionItem) {
                BaseConfigSection sectionClone = sectionItem.clone();
                sectionClone.parent = null;
                clonedList.add(sectionClone);
            } else if (item instanceof Map<?, ?> mapItem) {
                clonedList.add(deepCloneMap(mapItem));
            } else if (item instanceof List<?> listItem) {
                clonedList.add(deepCloneList(listItem));
            } else {
                clonedList.add(item);
            }
        }
        return clonedList;
    }

    private static Map<Object, Object> deepCloneMap(Map<?, ?> map) {
        Map<Object, Object> clonedMap = new HashMap<>();
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof BaseConfigSection section) {
                BaseConfigSection sectionClone = section.clone();
                sectionClone.parent = null;
                clonedMap.put(entry.getKey(), sectionClone);
            } else if (value instanceof Map<?, ?> mapItem) {
                Map<Object, Object> clonedMapItem = deepCloneMap(mapItem);
                clonedMap.put(entry.getKey(), clonedMapItem);
            } else if (value instanceof List<?> listItem) {
                List<Object> clonedListItem = deepCloneList(listItem);
                clonedMap.put(entry.getKey(), clonedListItem);
            } else {
                clonedMap.put(entry.getKey(), value);
            }
        }
        return clonedMap;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getFullPath() {
        if (parent == null) {
            return "root".equals(name) ? "" : name;
        }
        String parentPath = parent.getFullPath();
        return parentPath.isEmpty() ? name : parentPath + "." + name;
    }

    @Override
    public ConfigSection getParent() {
        return parent;
    }

    @Override
    public void setParent(ConfigSection parent) {
        this.parent = parent;
    }

    @Override
    public Map<String, ConfigSection> getSubSections() {
        return sectionHierarchyManager.getSubSections();
    }

    public Map<String, ConfigSection> getMutableSubSections() {
        return sectionHierarchyManager.getMutableSubSections();
    }

    @Override
    public ConfigSection getSection(String path) {
        return sectionHierarchyManager.getSection(path);
    }

    @Override
    public ConfigSection createSection(String path) {
        return sectionHierarchyManager.createSection(path);
    }

    @Override
    public boolean hasSection(String path) {
        return sectionHierarchyManager.hasSection(path);
    }

    @Override
    public void removeSection(String path) {
        sectionHierarchyManager.removeSection(path);
    }

    @Override
    public <T> T getValue(String path, Class<T> type) {
        return valueStore.getValue(path, type);
    }

    @Override
    public <T> T getValue(String path, T defaultValue, Class<T> type) {
        return valueStore.getValue(path, defaultValue, type);
    }

    @Override
    public <T> Optional<T> getOptionalValue(String path, Class<T> type) {
        return valueStore.getOptionalValue(path, type);
    }

    @Override
    public Object get(String path) {
        if (path == null || path.isEmpty()) return null;
        int idx = path.indexOf('.');
        if (idx < 0) {
            Object value = valueStore.get(path);
            if (value != null) return value;
            return getSection(path);
        }
        String first = path.substring(0, idx);
        String rest = path.substring(idx + 1);
        ConfigSection section = getSection(first);
        return section != null ? section.get(rest) : null;
    }

    @Override
    public Object get(String path, Object defaultValue) {
        Object value = get(path);
        return value != null ? value : defaultValue;
    }

    @Override
    public void set(String path, Object value) {
        if (path == null || path.isEmpty()) return;
        int idx = path.indexOf('.');
        if (idx < 0) {
            if (value instanceof Map<?, ?> mapValue) {
                Map<?, ?> clonedMap = deepCloneMap(mapValue);
                MapSection mapSection = createCustomSection(path, MapSection.class);
                mapSection.clear();
                mapSection.fromMap((Map<String, Object>) clonedMap);
            } else if (value instanceof ConfigSection sectionValue) {
                createCustomSection(path, sectionValue.getClass());
                ConfigSection createdSection = getSection(path);
                if (createdSection instanceof MapSection mapSection) {
                    mapSection.clear();
                    mapSection.fromMap(sectionValue.toMap());
                } else if (createdSection instanceof ListSection listSection) {
                    listSection.clear();
                    listSection.fromList(sectionValue.toList());
                } else if (createdSection instanceof SetSection setSection) {
                    setSection.clear();
                    setSection.fromSet(new HashSet<>(sectionValue.toSet()));
                }
            } else if (value instanceof SectionValueStore store) {
                for (Map.Entry<String, Object> entry : store.entrySet()) {
                    set(path + "." + entry.getKey(), entry.getValue());
                }
            } else if (value instanceof List<?> listValue) {
                List<Object> clonedList = deepCloneList(listValue);
                createCustomSection(path, ListSection.class);
                ListSection listSection = (ListSection) getSection(path);
                listSection.clear();
                listSection.fromList(clonedList);
            } else if (value instanceof Set<?> setValue) {
                Set<Object> clonedSet = new HashSet<>(setValue);
                createCustomSection(path, SetSection.class);
                SetSection setSection = (SetSection) getSection(path);
                setSection.clear();
                setSection.fromSet(clonedSet);
            } else {
                valueStore.set(path, value);
            }
            return;
        }
        String first = path.substring(0, idx);
        String rest = path.substring(idx + 1);
        ConfigSection section = getSection(first);
        if (section == null) section = createSection(first);
        section.set(rest, value);
    }

    @Override
    public void fromMap(Map<String, Object> values) {
        if (values == null) {
            return;
        }
        clear();
        for (Map.Entry<String, Object> entry : values.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> mapValue) {
                getOrCreateNestedSection(key).fromMap((Map<String, Object>) mapValue);
            } else if (value instanceof List<?> listValue) {
                ListSection section = createCustomSection(key, ListSection.class);
                section.fromList(listValue);
            } else if (value instanceof Set<?> setValue) {
                SetSection section = createCustomSection(key, SetSection.class);
                section.fromSet(new HashSet<>(setValue));
            } else {
                set(key, value);
            }
        }
    }

    private BaseConfigSection getOrCreateNestedSection(String path) {
        ConfigSection section = getSection(path);
        if (section instanceof BaseConfigSection baseSection) {
            return baseSection;
        }
        return createCustomSection(path, MapSection.class);
    }

    @Override
    public void remove(String path) {
        if (path == null || path.isEmpty()) return;
        int idx = path.indexOf('.');
        if (idx < 0) {
            valueStore.set(path, null);
            return;
        }
        String first = path.substring(0, idx);
        String rest = path.substring(idx + 1);
        ConfigSection section = getSection(first);
        if (section != null) section.remove(rest);
    }

    @Override
    public boolean contains(String path) {
        if (path == null || path.isEmpty()) return false;
        int idx = path.indexOf('.');
        if (idx < 0) return valueStore.contains(path);
        String first = path.substring(0, idx);
        String rest = path.substring(idx + 1);
        ConfigSection section = getSection(first);
        return section != null && section.contains(rest);
    }

    @Override
    public String getString(String path) {
        Object value = get(path);
        return value != null ? value.toString() : null;
    }

    @Override
    public String getString(String path, String defaultValue) {
        String value = getString(path);
        return value != null ? value : defaultValue;
    }

    @Override
    public boolean getBoolean(String path) {
        Object value = get(path);
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return Boolean.parseBoolean(value.toString());
        return false;
    }

    @Override
    public boolean getBoolean(String path, boolean defaultValue) {
        Object value = get(path);
        if (value instanceof Boolean) return (Boolean) value;
        if (value != null) return Boolean.parseBoolean(value.toString());
        return defaultValue;
    }

    @Override
    public int getInt(String path) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
        }
        return 0;
    }

    @Override
    public int getInt(String path, int defaultValue) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).intValue();
        if (value != null) try {
            return Integer.parseInt(value.toString());
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public long getLong(String path) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value != null) try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
        }
        return 0L;
    }

    @Override
    public long getLong(String path, long defaultValue) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).longValue();
        if (value != null) try {
            return Long.parseLong(value.toString());
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public double getDouble(String path) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value != null) try {
            return Double.parseDouble(value.toString());
        } catch (Exception ignored) {
        }
        return 0.0;
    }

    @Override
    public double getDouble(String path, double defaultValue) {
        Object value = get(path);
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value != null) try {
            return Double.parseDouble(value.toString());
        } catch (Exception ignored) {
        }
        return defaultValue;
    }

    @Override
    public List<?> getList(String path) {
        Object value = get(path);
        if (value instanceof List) return (List<?>) value;
        if (value instanceof ListSection listSection) {
            return listSection.toList();
        }
        if (value instanceof SetSection setSection) {
            return new ArrayList<>(setSection.toSet());
        }
        return null;
    }

    @Override
    public List<?> getList(String path, List<?> defaultValue) {
        List<?> value = getList(path);
        return value != null ? value : defaultValue;
    }

    @Override
    public List<String> getStringList(String path) {
        List<?> list = getList(path);
        if (list == null) return null;
        List<String> result = new ArrayList<>();
        for (Object obj : list) result.add(obj != null ? obj.toString() : "null");
        return result;
    }

    @Override
    public List<String> getStringList(String path, List<String> defaultValue) {
        List<String> value = getStringList(path);
        return value != null ? value : defaultValue;
    }

    @Override
    public Set<String> getKeys(boolean deep) {
        Set<String> keys = new HashSet<>(valueStore.keySet());
        if (!deep) {
            keys.addAll(sectionHierarchyManager.getMutableSubSections().keySet());
        } else {
            for (Map.Entry<String, ConfigSection> entry : sectionHierarchyManager.getMutableSubSections().entrySet()) {
                ConfigSection section = entry.getValue();
                String sectionKey = entry.getKey();
                if (!section.getKeys(false).isEmpty()) {
                    keys.add(sectionKey);
                }
                for (String key : section.getKeys(true)) {
                    keys.add(sectionKey + "." + key);
                }
            }
        }
        return keys;
    }

    @Override
    public Map<String, Object> getValues(boolean deep) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : valueStore.entrySet()) {
            result.put(entry.getKey(), entry.getValue());
        }
        if (deep) {
            for (Map.Entry<String, ConfigSection> entry : sectionHierarchyManager.getMutableSubSections().entrySet()) {
                String prefix = entry.getKey() + ".";
                for (Map.Entry<String, Object> valueEntry : entry.getValue().getValues(true).entrySet()) {
                    result.put(prefix + valueEntry.getKey(), valueEntry.getValue());
                }
            }
        } else {
            result.putAll(sectionHierarchyManager.getMutableSubSections());
        }
        return result;
    }

    @Override
    public Map<String, Object> toMap() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : valueStore.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof ConfigSection) {
                result.put(entry.getKey(), ((ConfigSection) value).toMap());
            } else if (value instanceof SectionValueStore) {
                result.put(entry.getKey(), ((SectionValueStore) value).getValues());
            } else {
                result.put(entry.getKey(), value);
            }
        }
        for (Map.Entry<String, ConfigSection> entry : sectionHierarchyManager.getMutableSubSections().entrySet()) {
            result.put(entry.getKey(), entry.getValue().toMap());
        }
        return result;
    }

    @Override
    public List<Object> toList() {
        return serializationHelper.toList();
    }

    @Override
    public Set<Object> toSet() {
        return serializationHelper.toSet();
    }

    @Override
    public int size() {
        int size = valueStore.size();
        for (ConfigSection section : sectionHierarchyManager.getMutableSubSections().values()) {
            size += section.size();
        }
        return size;
    }

    @Override
    public Map<String, Object> toSerializableMap() {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : valueStore.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof ConfigSection) {
                result.put(entry.getKey(), ((ConfigSection) value).toSerializableMap());
            } else if (value instanceof SectionValueStore) {
                result.put(entry.getKey(), ((SectionValueStore) value).getValues());
            } else {
                result.put(entry.getKey(), value);
            }
        }
        for (Map.Entry<String, ConfigSection> entry : sectionHierarchyManager.getMutableSubSections().entrySet()) {
            result.put(entry.getKey(), entry.getValue().toSerializableMap());
        }
        return result;
    }

    @Override
    public void clear() {
        valueStore.clear();
        sectionHierarchyManager.getMutableSubSections().clear();
    }

    @Override
    public void merge(ConfigSection other) {
        if (other == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : other.getValues(false).entrySet()) {
            if (entry.getValue() instanceof ConfigSection) {
                continue;
            }
            set(entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, ConfigSection> entry : other.getSubSections().entrySet()) {
            String key = entry.getKey();
            ConfigSection otherSection = entry.getValue();
            ConfigSection thisSection = getSection(key);
            if (thisSection == null) {
                thisSection = createSection(key);
            }
            thisSection.merge(otherSection);
        }
    }

    @Override
    public void validate() {
    }

    @Override
    public void copyFrom(ConfigSection rootSection) {
        if (rootSection == null) {
            return;
        }
        clear();
        if (rootSection instanceof BaseConfigSection baseRoot) {
            this.valueStore.clear();
            this.valueStore.copyFrom(baseRoot.valueStore);
            this.sectionHierarchyManager.getMutableSubSections().clear();
            this.sectionHierarchyManager.copyFrom(baseRoot.sectionHierarchyManager);
            this.metadataManager = baseRoot.metadataManager.deepClone();
            this.commentManager = baseRoot.commentManager.deepClone();
        } else {
            fromMap(rootSection.toMap());
        }
    }

    @Override
    public BaseConfigSection clone() {
        BaseConfigSection cloned;
        try {
            cloned = (BaseConfigSection) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        cloned.valueStore = this.valueStore.deepClone();
        cloned.sectionHierarchyManager = this.sectionHierarchyManager.deepClone(cloned);
        cloned.metadataManager = this.metadataManager.deepClone();
        cloned.commentManager = this.commentManager.deepClone();
        return cloned;
    }

    @Override
    public <T extends ConfigSection> T createCustomSection(String name, Class<T> clazz) {
        ConfigSection section = sectionFactory.createCustomSection(name, clazz);
        if (section == null) {
            throw new IllegalArgumentException("Could not create section of type " + clazz.getName() + " with name " + name);
        }
        if (section instanceof BaseConfigSection baseSection) {
            baseSection.parent = this;
        }
        sectionHierarchyManager.getMutableSubSections().put(name, section);
        return clazz.cast(section);
    }

    @Override
    public void addMetadata(String key, Object value) {
        metadataManager.addMetadata(key, value);
    }

    public SectionHierarchyManager getSectionHierarchyManager() {
        return sectionHierarchyManager;
    }

    public SectionFactory getSectionFactory() {
        return sectionFactory;
    }

    public SectionCommentManager getCommentManager() {
        return commentManager;
    }

    public SectionMetadataManager getMetadataManager() {
        return metadataManager;
    }

    public SectionSerializationHelper getSerializationHelper() {
        return serializationHelper;
    }

    public SectionValueStore getValueStore() {
        return valueStore;
    }

    @Override
    public <T> T getMetadata(String key, Class<T> type) {
        return metadataManager.getMetadata(key, type);
    }

    @Override
    public Map<String, Object> getMetadata() {
        return metadataManager.getMetadata();
    }

    @Override
    public boolean hasMetadata(String key) {
        return metadataManager.hasMetadata(key);
    }

    @Override
    public void removeMetadata(String key) {
        metadataManager.removeMetadata(key);
    }

    @Override
    public void clearMetadata() {
        metadataManager.clearMetadata();
    }

    @Override
    public void setComment(String path, String comment) {
        commentManager.setComment(path, comment);
    }

    @Override
    public String getComment(String path) {
        return commentManager.getComment(path);
    }

    @Override
    public boolean hasComment(String path) {
        return commentManager.hasComment(path);
    }

    @Override
    public void removeComment(String path) {
        commentManager.removeComment(path);
    }

    @Override
    public Map<String, String> getComments() {
        return commentManager.getComments();
    }

    @Override
    public void clearComments() {
        commentManager.clearComments();
    }
}
