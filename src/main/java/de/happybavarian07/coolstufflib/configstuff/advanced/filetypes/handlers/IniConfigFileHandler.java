package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

public class IniConfigFileHandler extends AbstractConfigFileHandler {
    private final ConfigTypeConverterRegistry converterRegistry;

    public IniConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public IniConfigFileHandler(ConfigTypeConverterRegistry registry) {
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", data);
            for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                writer.write(entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue().toString()) + "\n");
            }
        }
    }

    @Override
    public Map<String, Object> doLoad(File file) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!file.exists()) return map;
        Map<String, String> stringMap = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith(";")) continue;
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String val = line.substring(idx + 1).trim();
                    stringMap.put(key, val);
                }
            }
        }
        return Utils.unflatten(converterRegistry, stringMap);
    }
}
