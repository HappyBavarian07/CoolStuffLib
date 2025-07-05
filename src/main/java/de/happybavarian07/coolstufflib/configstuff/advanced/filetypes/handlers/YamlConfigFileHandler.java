package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.jetbrains.annotations.NotNull;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class YamlConfigFileHandler extends AbstractConfigFileHandler {
    private static final String COMMENTS_KEY = "__comments";
    private final Yaml prettyYaml;
    private final ConfigTypeConverterRegistry converterRegistry;

    public YamlConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public YamlConfigFileHandler(ConfigTypeConverterRegistry registry) {
        super(Pattern.compile(".*\\.(yml|yaml)$", Pattern.CASE_INSENSITIVE));
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        prettyYaml = new Yaml(options);
        this.converterRegistry = registry;
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

    @NotNull
    static Map<String, Object> getStringObjectMap(Map<String, Object> map, SetSection section) {
        Object values = map.get("values");
        if (values instanceof List<?>) {
            for (Object v : (List<?>) values) section.add(v);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("", section);
        return result;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) throws IOException {
        doSave(file, data, Collections.emptyMap());
    }

    @Override
    protected void doSave(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> nested = Utils.unflattenObjectMap(converterRegistry, data);
            Object withTypes = addSectionTypeFields(nested);
            if (withTypes instanceof Map) {
                writeYamlWithComments(writer, (Map<String, Object>) withTypes, comments, "", 0);
            } else {
                prettyYaml.dump(withTypes, writer);
            }
        }
    }

    private void writeYamlWithComments(Writer writer, Map<String, Object> map, Map<String, String> comments, String path, int indent) throws IOException {
        String indentStr = new String(new char[indent]).replace('\0', ' ');
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            String fullPath = path.isEmpty() ? key : path + "." + key;
            if (comments != null && comments.containsKey(fullPath)) {
                String comment = comments.get(fullPath);
                for (String line : comment.split("\n")) {
                    writer.write(indentStr + "# " + line + System.lineSeparator());
                }
            }
            Object value = entry.getValue();
            if (value instanceof Map) {
                writer.write(indentStr + key + ":" + System.lineSeparator());
                writeYamlWithComments(writer, (Map<String, Object>) value, comments, fullPath, indent + 2);
            } else if (value instanceof List) {
                writer.write(indentStr + key + ":" + System.lineSeparator());
                List<?> list = (List<?>) value;
                for (Object v : list) {
                    writer.write(indentStr + "- " + formatYamlValue(v) + System.lineSeparator());
                }
            } else {
                writer.write(indentStr + key + ": " + formatYamlValue(value) + System.lineSeparator());
            }
        }
    }

    private String formatYamlValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) return '"' + value.toString().replace("\"", "\\\"") + '"';
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        return '"' + value.toString().replace("\"", "\\\"") + '"';
    }

    @Override
    protected Map<String, String> doLoadComments(File file) throws IOException {
        Map<String, String> comments = new LinkedHashMap<>();
        if (!file.exists()) return comments;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            StringBuilder lastComment = null;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.startsWith("#")) {
                    String comment = trimmed.substring(1).trim();
                    if (lastComment == null) lastComment = new StringBuilder(comment);
                    else lastComment.append("\n").append(comment);
                } else if (trimmed.contains(":")) {
                    String key = trimmed.split(":")[0].trim();
                    if (lastComment != null) {
                        comments.put(key, lastComment.toString());
                        lastComment = null;
                    }
                }
            }
        }
        return comments;
    }

    private Object addSectionTypeFields(Object obj) {
        if (obj instanceof MapSection) {
            Map<String, Object> map = new LinkedHashMap<>(((MapSection) obj).toSerializableMap());
            map.put("__type__", "MapSection");
            return map;
        }
        if (obj instanceof ListSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("__type__", "ListSection");
            map.put("values", new ArrayList<>(((ListSection) obj).toList()));
            return map;
        }
        if (obj instanceof SetSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("__type__", "SetSection");
            map.put("values", new ArrayList<>(((SetSection) obj).toSet()));
            return map;
        }
        if (obj instanceof Map<?, ?> m) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : m.entrySet()) {
                result.put(String.valueOf(entry.getKey()), addSectionTypeFields(entry.getValue()));
            }
            return result;
        }
        if (obj instanceof List<?> l) {
            List<Object> result = new ArrayList<>();
            for (Object o : l) result.add(addSectionTypeFields(o));
            return result;
        }
        return obj;
    }

    private Object ensureSectionType(Object obj) {
        if (obj instanceof Map<?, ?> map) {
            if (map.containsKey("__type__")) {
                String type = String.valueOf(map.get("__type__"));
                if ("MapSection".equals(type)) {
                    MapSection section = new MapSection("");
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        if (!"__type__".equals(entry.getKey())) {
                            section.put(String.valueOf(entry.getKey()), ensureSectionType(entry.getValue()));
                        }
                    }
                    return section;
                }
                if ("ListSection".equals(type)) {
                    ListSection section = new ListSection("");
                    Object values = map.get("values");
                    if (values instanceof List<?>) {
                        for (Object v : (List<?>) values) section.add(ensureSectionType(v));
                    }
                    return section;
                }
                if ("SetSection".equals(type)) {
                    SetSection section = new SetSection("");
                    Object values = map.get("values");
                    if (values instanceof List<?>) {
                        for (Object v : (List<?>) values) section.add(ensureSectionType(v));
                    }
                    return section;
                }
            }
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), ensureSectionType(entry.getValue()));
            }
            return result;
        }
        if (obj instanceof List<?>) {
            List<Object> result = new ArrayList<>();
            for (Object o : (List<?>) obj) result.add(ensureSectionType(o));
            return result;
        }
        return obj;
    }

    @Override
    public Map<String, Object> doLoad(File file) {
        if (!file.exists()) {
            return new HashMap<>();
        }
        try (FileInputStream inputStream = new FileInputStream(file)) {
            Object yamlObj = prettyYaml.load(inputStream);
            Object withTypes = ensureSectionType(yamlObj);
            return withTypes instanceof Map ? (Map<String, Object>) withTypes : new HashMap<>();
        } catch (Exception e) {
            ConfigLogger.error("Error loading YAML file: " + file.getPath(), e, "YamlConfigFileHandler", true);
            return new HashMap<>();
        }
    }

    @Override
    public boolean supportsComments() {
        return true;
    }
}
