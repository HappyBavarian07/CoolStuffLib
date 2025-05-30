package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.HierarchicalConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.lang.reflect.ParameterizedType;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;

public class YamlConfigFileHandler implements HierarchicalConfigFileHandler {
    private final Yaml prettyYaml;
    private final ConfigTypeConverterRegistry converterRegistry;

    public YamlConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public YamlConfigFileHandler(ConfigTypeConverterRegistry registry) {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        prettyYaml = new Yaml(options);
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
            prettyYaml.dump(plain, writer);
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

    private Object convertForYaml(Object value) {
        return Utils.recursiveConvertForSerialization(value, converterRegistry);
    }

    private Object convertFromYaml(Object value) {
        return Utils.recursiveConvertFromSerialization(value, converterRegistry);
    }

    @Override
    public Map<String, Object> load(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            Object data = prettyYaml.load(reader);
            if (data instanceof Map map) {
                return (Map<String, Object>) convertFromYaml(map);
            }
            return new HashMap<>();
        }
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
