package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.DefaultGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Key;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;

import java.util.*;

public class AutoGenModule extends ConfigModule {
    private final List<AutoGenTemplate> templates = new ArrayList<>();
    private final Group rootGroup = new DefaultGroup("", null);
    private AdvancedConfig config;

    public void addTemplate(AutoGenTemplate template) {
        templates.add(template);
        template.applyTo(rootGroup);
    }

    public Group getRootGroup() {
        return rootGroup;
    }

    public Key getKeyByPath(String path) {
        String[] parts = path.split("\\.");
        Group current = rootGroup;
        for (int i = 0; i < parts.length - 1; i++) {
            Group next = current.getGroup(parts[i]);
            if (next == null) return null;
            current = next;
        }
        return current.getKey(parts[parts.length - 1]);
    }

    public Group getGroupByPath(String path) {
        String[] parts = path.split("\\.");
        Group current = rootGroup;
        for (String part : parts) {
            Group next = current.getGroup(part);
            if (next == null) return null;
            current = next;
        }
        return current;
    }

    @Override
    public String getName() {
        return "AutoGenModule";
    }

    @Override
    public void enable() { /* No-op */ }

    @Override
    public void disable() { /* No-op */ }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
    }

    private void setAllKeysRecursive(Group group, AdvancedConfig config) {
        for (Key key : group.getKeys()) {
            config.setValue(group.getFullPath() + "." + key.getName(), key.getValue());
        }
        for (Group sub : group.getSubGroups()) {
            setAllKeysRecursive(sub, config);
        }
    }

    @Override
    public void onAttach(AdvancedConfig config) {
        this.config = config;
        // Generate config from templates
        for (AutoGenTemplate template : templates) {
            template.applyTo(rootGroup);
        }

        setAllKeysRecursive(rootGroup, config);
        config.save();
    }

    @Override
    public void onDetach() {
        this.config = null;
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return true;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("templateCount", templates.size());
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    public List<AutoGenTemplate> getTemplates() {
        return java.util.Collections.unmodifiableList(templates);
    }

    /**
     * Returns a merged map of all templates, with each template's map inserted at its base path.
     */
    public Map<String, Object> getMergedTemplateMap() {
        Map<String, Object> merged = new java.util.LinkedHashMap<>();
        for (AutoGenTemplate template : templates) {
            String base = template.getBasePath();
            Map<String, Object> map = template.toMap();
            if (base == null || base.isEmpty()) {
                mergeInto(merged, map);
            } else {
                setValueByPath(merged, base, map);
            }
        }
        return merged;
    }

    private void mergeInto(Map<String, Object> target, Map<String, Object> source) {
        for (Map.Entry<String, Object> entry : source.entrySet()) {
            if (entry.getValue() instanceof Map && target.get(entry.getKey()) instanceof Map) {
                mergeInto((Map<String, Object>) target.get(entry.getKey()), (Map<String, Object>) entry.getValue());
            } else {
                target.put(entry.getKey(), entry.getValue());
            }
        }
    }

    private void setValueByPath(Map<String, Object> map, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = map;
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new java.util.LinkedHashMap<>());
        }
        current.put(parts[parts.length - 1], value);
    }

    public void clearTemplates() {
        templates.clear();
    }
}
