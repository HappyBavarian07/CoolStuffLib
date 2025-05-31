package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.util.*;

public class PropertiesConfigFileHandler extends AbstractConfigFileHandler {
    private final ConfigTypeConverterRegistry converterRegistry;

    public PropertiesConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public PropertiesConfigFileHandler(ConfigTypeConverterRegistry registry) {
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", data);
            Properties properties = new Properties();
            for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                properties.setProperty(entry.getKey(), entry.getValue() == null ? "" : entry.getValue().toString());
            }
            properties.store(writer, null);
        }
    }

    @Override
    public Map<String, Object> doLoad(File file) throws IOException {
        Map<String, Object> map = new LinkedHashMap<>();
        if (!file.exists()) return map;
        Properties properties = new Properties();
        try (Reader reader = new FileReader(file)) {
            properties.load(reader);
        }
        Map<String, String> stringMap = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            stringMap.put(name, properties.getProperty(name));
        }
        return Utils.unflatten(converterRegistry, stringMap);
    }
}
