package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

public class AdvancedPersistentConfig extends BaseAdvancedConfig {

    public AdvancedPersistentConfig(String name, File file, ConfigFileHandler configFileHandler) {
        super(name, file, configFileHandler);
        initConfig();
    }

    public AdvancedPersistentConfig(String name, File file, ConfigFileType configFileType) {
        super(name, file, configFileType);
        initConfig();
    }

    private void initConfig() {
        File file = getFile();
        if (file != null && file.exists()) {
            reload();
        } else {
            if (file != null && file.getParentFile() != null) {
                file.getParentFile().mkdirs();
            }
            save();
        }
    }

    @Override
    public void save() {
        lockValuesWrite();
        try {
            File file = getFile();
            if (file != null) {
                try {
                    ConfigFileHandler handler = getConfigFileHandler(file);

                    Map<String, Object> configMap = getRootSection().toSerializableMap();

                    if (!file.exists()) {
                        File parent = file.getParentFile();
                        if (parent != null) {
                            parent.mkdirs();
                        }
                        file.createNewFile();
                    }

                    handler.save(file, configMap, getCommentManager().getAllComments());

                    getEventBus().publish(ConfigLifecycleEvent.configSave(this));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to save config to " + file.getPath(), e);
                }
            }
        } finally {
            unlockValuesWrite();
        }
    }

    private @NotNull ConfigFileHandler getConfigFileHandler(File file) {
        ConfigFileHandler handler = getConfigFileHandler();

        if (!handler.canHandle(file)) {
            throw new IllegalArgumentException(
                String.format("File handler '%s' cannot handle file '%s'. Expected extension: '%s'",
                    handler.getClass().getSimpleName(),
                    file.getName(),
                    handler.getFileExtension())
            );
        }
        return handler;
    }

    @Override
    public void reload() {
        lockValuesWrite();
        try {
            File file = getFile();
            if (file != null && file.exists()) {
                try {
                    ConfigFileHandler handler = getConfigFileHandler(file);

                    Map<String, Object> configMap = handler.load(file);
                    getRootSection().clear();
                    loadRecursive(getRootSection(), configMap);
                    Map<String, String> comments = handler.loadComments(file);
                    getCommentManager().setBulkComments(comments);

                    getEventBus().publish(ConfigLifecycleEvent.configReload(this));
                } catch (IOException e) {
                    throw new RuntimeException("Failed to load config from " + file.getPath(), e);
                }
            }
        } finally {
            unlockValuesWrite();
        }
    }

    @SuppressWarnings("unchecked")
    private void loadRecursive(ConfigSection section, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

            if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                String type = (String) subMap.get("__type__");
                if ("ListSection".equals(type)) {
                    ListSection subSection = section.createCustomSection(key, ListSection.class);
                    Object items = subMap.get("__items");
                    if (items instanceof java.util.List) {
                        subSection.fromList((java.util.List<?>) items);
                    }
                } else if ("MapSection".equals(type)) {
                    MapSection subSection = section.createCustomSection(key, MapSection.class);
                    subMap.remove("__type__");
                    subSection.fromMap(subMap);
                } else if ("SetSection".equals(type)) {
                    SetSection subSection = section.createCustomSection(key, SetSection.class);
                    Object items = subMap.get("__items");
                    if (items instanceof java.util.List) {
                        subSection.fromSet((Set<Object>) items);
                    }
                } else {
                    ConfigSection subSection = section.createSection(key);
                    loadRecursive(subSection, subMap);
                }
            } else {
                section.set(key, value);
            }
        }
    }
}
