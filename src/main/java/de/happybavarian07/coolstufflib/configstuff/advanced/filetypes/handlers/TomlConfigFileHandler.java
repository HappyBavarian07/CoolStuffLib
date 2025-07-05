package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.utils.Utils;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

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
        super(Pattern.compile(".*\\.toml$", Pattern.CASE_INSENSITIVE));
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    protected void doSave(File file, Map<String, Object> data) throws IOException {
        doSave(file, data, Collections.emptyMap());
    }

    @Override
    protected void doSave(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> flatMap = new LinkedHashMap<>();
            flattenTomlMap(flatMap, "", Utils.unflattenObjectMap(converterRegistry, data));
            writeTomlFromFlat(writer, flatMap, comments);
        }
    }

    private void flattenTomlMap(Map<String, Object> flat, String prefix, Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> m) {
                flattenTomlMap(flat, key, (Map<String, Object>) m);
            } else if (value instanceof BaseConfigSection section) {
                flat.put(key, section.toSerializableMap());
            } else if (value instanceof List<?> l) {
                flat.put(key, l);
            } else {
                flat.put(key, value);
            }
        }
    }

    private void writeTomlFromFlat(Writer writer, Map<String, Object> flatMap, Map<String, String> comments) throws IOException {
        Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
        Map<String, Object> root = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            int dotIdx = key.lastIndexOf('.');
            if (dotIdx > 0) {
                String section = key.substring(0, dotIdx);
                String subKey = key.substring(dotIdx + 1);
                sections.computeIfAbsent(section, k -> new LinkedHashMap<>()).put(subKey, value);
            } else {
                root.put(key, value);
            }
        }
        for (Map.Entry<String, Object> entry : root.entrySet()) {
            writeTomlKeyValue(writer, entry.getKey(), entry.getValue(), comments);
        }
        for (Map.Entry<String, Map<String, Object>> section : sections.entrySet()) {
            writer.write("[" + section.getKey() + "]\n");
            for (Map.Entry<String, Object> entry : section.getValue().entrySet()) {
                writeTomlKeyValue(writer, entry.getKey(), entry.getValue(), comments);
            }
        }
    }

    private void writeTomlKeyValue(Writer writer, String key, Object value, Map<String, String> comments) throws IOException {
        if (comments != null && comments.containsKey(key)) {
            writer.write("# " + comments.get(key).replace("\n", "\n# ") + "\n");
        }
        if (value instanceof List<?>) {
            writer.write(key + " = [");
            List<?> list = (List<?>) value;
            for (int i = 0; i < list.size(); i++) {
                writer.write(formatTomlValue(list.get(i)));
                if (i < list.size() - 1) writer.write(", ");
            }
            writer.write("]\n");
        } else {
            writer.write(key + " = " + formatTomlValue(value) + "\n");
        }
    }

    private String formatTomlValue(Object value) {
        if (value == null) return "";
        if (value instanceof String) return "\"" + value.toString().replace("\"", "\\\"") + "\"";
        if (value instanceof Boolean || value instanceof Number) return value.toString();
        return "\"" + value.toString().replace("\"", "\\\"") + "\"";
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

    private Map<String, Object> convertSectionTypes(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> subMap = (Map<String, Object>) value;
                if (subMap.containsKey("__type__")) {
                    String type = String.valueOf(subMap.get("__type__"));
                    if ("MapSection".equals(type)) {
                        MapSection section = new MapSection("");
                        for (Map.Entry<String, Object> e : subMap.entrySet()) {
                            if (!"__type__".equals(e.getKey())) section.put(e.getKey(), e.getValue());
                        }
                        result.put(entry.getKey(), section);
                        continue;
                    }
                    if ("ListSection".equals(type)) {
                        ListSection section = new ListSection("");
                        Object values = subMap.get("values");
                        if (values instanceof List<?>) for (Object v : (List<?>) values) section.add(v);
                        result.put(entry.getKey(), section);
                        continue;
                    }
                    if ("SetSection".equals(type)) {
                        SetSection section = new SetSection("");
                        Object values = subMap.get("values");
                        if (values instanceof List<?>) for (Object v : (List<?>) values) section.add(v);
                        result.put(entry.getKey(), section);
                        continue;
                    }
                }
                result.put(entry.getKey(), convertSectionTypes(subMap));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private Object ensureSectionType(Object obj) {
        if (obj instanceof Map) {
            Map<?, ?> map = (Map<?, ?>) obj;
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
    public Map<String, Object> doLoad(File file) throws IOException {
        if (!file.exists()) return new HashMap<>();
        Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> currentSection = root;
        String currentSectionPath = "";
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSectionPath = line.substring(1, line.length() - 1).trim();
                    currentSection = getOrCreateSection(root, currentSectionPath);
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String val = line.substring(idx + 1).trim();
                    Object parsedVal = parseTomlValue(val);
                    insertNestedKey(currentSection, key, parsedVal);
                }
            }
        }
        return root;
    }

    private Map<String, Object> getOrCreateSection(Map<String, Object> root, String sectionPath) {
        String[] parts = sectionPath.split("\\.");
        Map<String, Object> section = root;
        for (String part : parts) {
            Object next = section.get(part);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                section.put(part, next);
            }
            section = (Map<String, Object>) next;
        }
        return section;
    }

    private void insertNestedKey(Map<String, Object> section, String key, Object value) {
        String[] parts = key.split("\\.");
        Map<String, Object> current = section;
        for (int i = 0; i < parts.length - 1; i++) {
            String part = parts[i];
            Object next = current.get(part);
            if (!(next instanceof Map)) {
                next = new LinkedHashMap<String, Object>();
                current.put(part, next);
            }
            current = (Map<String, Object>) next;
        }
        current.put(parts[parts.length - 1], value);
    }

    private Object parseTomlValue(String val) {
        if (val.startsWith("[") && val.endsWith("]")) {
            String inner = val.substring(1, val.length() - 1).trim();
            if (inner.isEmpty()) return new ListSection("");
            ListSection list = new ListSection("");
            for (String item : inner.split(",")) {
                String trimmed = item.trim();
                list.add(parseTomlPrimitive(trimmed));
            }
            return list;
        }
        return parseTomlPrimitive(val);
    }

    private Object parseTomlPrimitive(String val) {
        if (val.startsWith("\"") && val.endsWith("\"")) return val.substring(1, val.length() - 1);
        if (val.equalsIgnoreCase("true") || val.equalsIgnoreCase("false")) return Boolean.parseBoolean(val);
        try { return Integer.parseInt(val); } catch (Exception ignored) {}
        try { return Double.parseDouble(val); } catch (Exception ignored) {}
        return val;
    }

    private Map<String, Object> convertSectionsRecursive(Map<String, Object> map) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.put(entry.getKey(), convertSectionsRecursive((Map<String, Object>) value));
            } else if (value instanceof BaseConfigSection section) {
                result.put(entry.getKey(), section.toSerializableMap());
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @Override
    protected Map<String, String> doLoadComments(File file) throws IOException {
        Map<String, String> comments = new LinkedHashMap<>();
        if (!file.exists()) return comments;
        String currentSection = "";
        StringBuilder lastComment = null;
        int lineNum = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                line = line.trim();
                if (line.isEmpty()) continue;
                if (line.startsWith("[")) {
                    if (line.endsWith("]")) {
                        currentSection = line.substring(1, line.length() - 1).trim();
                    }
                    continue;
                }
                if (line.startsWith("#")) {
                    String comment = line.substring(1).trim();
                    if (lastComment == null) lastComment = new StringBuilder(comment);
                    else lastComment.append("\n").append(comment);
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    if (lastComment != null) {
                        comments.put(fullKey, lastComment.toString());
                        lastComment = null;
                    }
                } else if (lastComment != null) {
                    Utils.logMalformedLine(file, lineNum, line, "Expected key=value after comment");
                    lastComment = null;
                }
            }
        } catch (IOException e) {
            ConfigLogger.error("Error in file: " + file.getPath(), e, "TomlConfigFileHandler", true);
        }
        return comments;
    }

    @Override
    public boolean supportsComments() {
        return true;
    }
}
