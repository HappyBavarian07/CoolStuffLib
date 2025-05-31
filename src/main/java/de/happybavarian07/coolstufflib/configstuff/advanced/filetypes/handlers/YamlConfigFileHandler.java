package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;

public class YamlConfigFileHandler extends AbstractConfigFileHandler {
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
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Object nested = unflatten(data);
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

    @Override
    public Map<String, Object> doLoad(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            Object data = prettyYaml.load(reader);
            if (data instanceof Map map) {
                return (Map<String, Object>) Utils.recursiveConvertFromSerialization(map, converterRegistry);
            }
            return new HashMap<>();
        }
    }
}
