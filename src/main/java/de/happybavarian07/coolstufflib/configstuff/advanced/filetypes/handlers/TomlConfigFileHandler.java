package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.HierarchicalConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.util.*;

/**
 * TOML handler with hierarchical path and section support.
 * For production, use a TOML library like Toml4j.
 */
public class TomlConfigFileHandler implements HierarchicalConfigFileHandler {
    private final ConfigTypeConverterRegistry converterRegistry;

    public TomlConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }
    public TomlConfigFileHandler(ConfigTypeConverterRegistry registry) {
        this.converterRegistry = registry;
    }
    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void save(AdvancedConfig config, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> valueMap = config.getValueMap();
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", valueMap);
            writeToml(writer, flatMap, "");
        }
    }

    private void writeToml(Writer writer, Map<String, Object> map, String prefix) throws IOException {
        Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.contains(".")) {
                int idx = key.indexOf('.');
                String section = key.substring(0, idx);
                String rest = key.substring(idx + 1);
                sections.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(rest, value);
            } else {
                writer.write(prefix + key + " = \"" + (value == null ? "" : converterRegistry.tryToSerialized(value)) + "\"\n");
            }
        }
        for (Map.Entry<String, Map<String, Object>> section : sections.entrySet()) {
            writer.write("\n[" + prefix + section.getKey() + "]\n");
            writeToml(writer, section.getValue(), prefix + section.getKey() + ".");
        }
    }

    @Override
    public Map<String, Object> load(File file) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!file.exists()) return map;
        String currentSection = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1).trim();
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String val = line.substring(idx + 1).trim().replaceAll("^\"|\"$", "");
                    String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    map.put(fullKey, converterRegistry.tryFromSerialized(val));
                }
            }
        }
        return map;
    }

    @Override
    public void setValueByPath(Map<String, Object> root, String path, Object value) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
        }
        current.put(parts[parts.length - 1], value);
    }

    @Override
    public Object getValueByPath(Map<String, Object> root, String path) {
        String[] parts = path.split("\\.");
        Map<String, Object> current = root;
        for (int i = 0; i < parts.length - 1; i++) {
            Object next = current.get(parts[i]);
            if (!(next instanceof Map)) return null;
            current = (Map<String, Object>) next;
        }
        return current.get(parts[parts.length - 1]);
    }
}
