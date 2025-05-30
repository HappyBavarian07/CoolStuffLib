package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.HierarchicalConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class JsonConfigFileHandler implements HierarchicalConfigFileHandler {
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
    public void save(AdvancedConfig config, File file) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Object nested = unflatten(config.getValueMap());
            Object plain = Utils.recursiveConvertForSerialization(nested, converterRegistry);
            gson.toJson(plain, writer);
        }
    }

    @Override
    public Map<String, Object> load(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int numRead;
            while ((numRead = reader.read(buf)) > 0) sb.append(buf, 0, numRead);
            String json = sb.toString();
            Map<String, Object> map = gson.fromJson(json, MAP_TYPE);
            return (Map<String, Object>) convertFromJson(map);
        }
    }

    private Object convertForJson(Object value) {
        return Utils.recursiveConvertForSerialization(value, converterRegistry);
    }

    private Object convertFromJson(Object value) {
        return Utils.recursiveConvertFromSerialization(value, converterRegistry);
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