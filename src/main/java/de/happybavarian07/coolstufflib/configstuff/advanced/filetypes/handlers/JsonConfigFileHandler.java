package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class JsonConfigFileHandler extends AbstractConfigFileHandler {
    private final Gson gson;
    private final ConfigTypeConverterRegistry converterRegistry;
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();

    public JsonConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public JsonConfigFileHandler(ConfigTypeConverterRegistry registry) {
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Object nested = unflatten(data);
            Object plain = Utils.recursiveConvertForSerialization(nested, converterRegistry);
            gson.toJson(plain, writer);
        }
    }

    @Override
    public Map<String, Object> doLoad(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int numRead;
            while ((numRead = reader.read(buf)) > 0) sb.append(buf, 0, numRead);
            String json = sb.toString();
            Map<String, Object> map = gson.fromJson(json, MAP_TYPE);
            return (Map<String, Object>) Utils.recursiveConvertFromSerialization(map, converterRegistry);
        }
    }

    private static Map<String, Object> unflatten(Map<String, Object> flat) {
        Map<String, Object> nested = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flat.entrySet()) {
            String[] parts = entry.getKey().split("\\.");
            Map<String, Object> current = nested;
            for (int i = 0; i < parts.length - 1; i++) {
                current = (Map<String, Object>) current.computeIfAbsent(parts[i], k -> new LinkedHashMap<>());
            }
            current.put(parts[parts.length - 1], entry.getValue());
        }
        return nested;
    }
}