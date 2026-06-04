package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigFileType;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionKind;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
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


            if (value instanceof ListSection ls) {
                ConfigSection sub = section.createSection(key, SectionKind.LIST, true);
                ((ListSection) sub).fromList(ls.toList());
            } else if (value instanceof SetSection ss) {
                ConfigSection sub = section.createSection(key, SectionKind.SET, true);
                ((SetSection) sub).fromSet(ss.toSet());
            } else if (value instanceof MapSection ms) {
                ConfigSection sub = section.createSection(key, SectionKind.MAP, true);
                ((MapSection) sub).fromMap(ms.getMapValues());
            } else if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                String type = (String) subMap.get("__type__");
                SectionKind kind = parseSectionType(type);
                ConfigSection subSection;
                if (kind != null) {
                    subSection = section.createSection(key, kind, true);
                    loadSerializedSection(subSection, key, subMap);
                } else {
                    subSection = section.createSection(key);
                    loadRecursive(subSection, subMap);
                }
            } else {
                section.set(key, value);
            }
        }
    }

    private SectionKind parseSectionType(String typeStr) {
        if ("ListSection".equals(typeStr)) return SectionKind.LIST;
        if ("MapSection".equals(typeStr)) return SectionKind.MAP;
        if ("SetSection".equals(typeStr)) return SectionKind.SET;
        return null;
    }

    private void loadSerializedSection(ConfigSection section, String key, Map<String, Object> subMap) {
        List<?> items = subMap.containsKey("__values__") ? (List<?>) subMap.get("__values__") :
                        subMap.containsKey("__items") ? (List<?>) subMap.get("__items__") : null;
        if (section instanceof ListSection && items != null) {
            ((ListSection) section).fromList(items);
        } else if (section instanceof SetSection && items != null) {
            ((SetSection) section).fromSet(new HashSet<>(items));
        } else if (section instanceof MapSection) {
            subMap.remove("__type__");
            section.fromMap(subMap);
        }
    }
}
