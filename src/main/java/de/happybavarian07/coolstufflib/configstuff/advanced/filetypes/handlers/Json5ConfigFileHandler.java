package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import com.google.gson.Gson;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

/**
 * Minimal JSON5 handler. For production, use a JSON5 parser (e.g. json5-java).
 * This implementation strips comments and parses as normal JSON.
 */
public class Json5ConfigFileHandler extends AbstractConfigFileHandler {
    private final Gson gson;
    private final ConfigTypeConverterRegistry converterRegistry;
    private static final Type MAP_TYPE = new com.google.gson.reflect.TypeToken<Map<String, Object>>() {
    }.getType();

    public Json5ConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public Json5ConfigFileHandler(ConfigTypeConverterRegistry registry) {
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

    @Override
    public Map<String, Object> doLoad(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                int commentIdx = line.indexOf("//");
                if (commentIdx >= 0) line = line.substring(0, commentIdx);
                sb.append(line).append("\n");
            }
        }
        String json = sb.toString();
        try {
            Map<String, Object> map = gson.fromJson(json, MAP_TYPE);
            return (Map<String, Object>) Utils.recursiveConvertFromSerialization(map, converterRegistry);
        } catch (Exception e) {
            return new HashMap<>();
        }
    }
}
