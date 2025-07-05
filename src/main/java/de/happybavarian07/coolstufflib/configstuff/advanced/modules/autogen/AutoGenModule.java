package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.AbstractBaseConfigModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.DefaultGroup;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Key;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;

import java.util.*;

public class AutoGenModule extends AbstractBaseConfigModule {
    private final Group rootGroup = new DefaultGroup("", null);

    private final Map<UUID, AutoGenTemplate> templateRegistry = new HashMap<>();
    private final Map<String, UUID> nameToTemplateIdMap = new HashMap<>();
    private final Map<String, UUID> fileToTemplateIdMap = new HashMap<>();

    public AutoGenModule() {
        super("AutoGenModule", "Automatically generates config structure from templates", "1.0.0");
    }

    public UUID registerTemplate(AutoGenTemplate template, String templateName) {
        UUID templateId = UUID.randomUUID();
        templateRegistry.put(templateId, template);
        nameToTemplateIdMap.put(templateName, templateId);
        template.applyTo(rootGroup);
        return templateId;
    }

    public void mapTemplateToFile(UUID templateId, String fileName) {
        if (templateRegistry.containsKey(templateId)) {
            fileToTemplateIdMap.put(fileName, templateId);
        } else {
            throw new IllegalArgumentException("Template with ID " + templateId + " is not registered");
        }
    }

    public void mapTemplateToFile(String templateName, String fileName) {
        UUID templateId = nameToTemplateIdMap.get(templateName);
        if (templateId != null) {
            fileToTemplateIdMap.put(fileName, templateId);
        } else {
            throw new IllegalArgumentException("Template with name " + templateName + " is not registered");
        }
    }

    public AutoGenTemplate getTemplateById(UUID templateId) {
        return templateRegistry.get(templateId);
    }

    public AutoGenTemplate getTemplateByName(String templateName) {
        UUID templateId = nameToTemplateIdMap.get(templateName);
        return templateId != null ? templateRegistry.get(templateId) : null;
    }

    public AutoGenTemplate getTemplateForFile(String fileName) {
        UUID templateId = fileToTemplateIdMap.get(fileName);
        return templateId != null ? templateRegistry.get(templateId) : null;
    }

    public Collection<AutoGenTemplate> getAllTemplates() {
        return templateRegistry.values();
    }

    public Set<UUID> getAllTemplateIds() {
        return templateRegistry.keySet();
    }

    public Map<String, UUID> getTemplateNameToIdMap() {
        return new HashMap<>(nameToTemplateIdMap);
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
    protected void onInitialize() {
        // Initialization logic
    }

    @Override
    protected void onEnable() {
        // Enable logic
    }

    @Override
    protected void onDisable() {
        // Disable logic
    }

    @Override
    protected void onCleanup() {
        templateRegistry.clear();
        nameToTemplateIdMap.clear();
        fileToTemplateIdMap.clear();
    }

    @Override
    public Set<String> getDependencies() {
        return dependencies;
    }

    @Override
    public boolean isConfigured() {
        return moduleConfiguration != null;
    }

    @Override
    public void configure(Map<String, Object> configuration) {
        this.moduleConfiguration = configuration;
        // Apply configuration
    }

    private void setAllKeysRecursive(Group group, AdvancedConfig config) {
        for (Key key : group.getKeys()) {
            String fullPath = group.getFullPath() + "." + key.getName();
            if (fullPath.startsWith(".")) {
                fullPath = fullPath.substring(1);
            }
            config.set(fullPath, key.getValue());
        }

        for (Group subGroup : group.getSubGroups()) {
            setAllKeysRecursive(subGroup, config);
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }

    public void applyTemplateToConfig(String templateName) {
        Group group = getGroupByPath(getTemplateByName(templateName).getBasePath());
        if (group != null) {
            setAllKeysRecursive(group, config);
        }
    }
}
