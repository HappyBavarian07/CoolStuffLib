package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.util.*;

/**
 * TOML handler with hierarchical path and section support.
 * For production, use a TOML library like Toml4j.
 */
public class TomlConfigFileHandler extends AbstractConfigFileHandler {
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
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", data);
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
    public Map<String, Object> doLoad(File file) throws IOException {
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
}
