package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.utils.Utils;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class IniConfigFileHandler extends AbstractConfigFileHandler {
    private final ConfigTypeConverterRegistry converterRegistry;
    private static final String COMMENT_MARKER = "#COMMENT:";

    public IniConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public IniConfigFileHandler(ConfigTypeConverterRegistry registry) {
        super(Pattern.compile(".*\\.(ini|cfg|conf)$", Pattern.CASE_INSENSITIVE));
        this.converterRegistry = registry;
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
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", (Map<String, Object>) addSectionTypeFields(nested));
            writer.write("; Configuration file managed by CoolStuffLib\n");
            writer.write("; DO NOT EDIT COMMENT LINES MANUALLY\n\n");
            Map<String, Map<String, Object>> sections = new LinkedHashMap<>();
            sections.put("", new LinkedHashMap<>());
            for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                String key = entry.getKey();
                int dotIndex = key.indexOf('.');
                if (dotIndex > 0) {
                    String section = key.substring(0, dotIndex);
                    String subKey = key.substring(dotIndex + 1);
                    Map<String, Object> sectionMap = sections.computeIfAbsent(section, k -> new LinkedHashMap<>());
                    sectionMap.put(subKey, entry.getValue());
                } else {
                    sections.get("").put(key, entry.getValue());
                }
            }
            Map<String, Object> defaultSection = sections.remove("");
            for (Map.Entry<String, Object> entry : defaultSection.entrySet()) {
                String fullKey = entry.getKey();
                if (comments.containsKey(fullKey)) {
                    writer.write("; " + comments.get(fullKey).replace("\n", "\n; ") + "\n");
                }
                writer.write(entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue().toString()) + "\n");
            }
            for (Map.Entry<String, Map<String, Object>> section : sections.entrySet()) {
                writer.write("\n[" + section.getKey() + "]\n");
                for (Map.Entry<String, Object> entry : section.getValue().entrySet()) {
                    String fullKey = section.getKey() + "." + entry.getKey();
                    if (comments.containsKey(fullKey)) {
                        writer.write("; " + comments.get(fullKey).replace("\n", "\n; ") + "\n");
                    }
                    writer.write(entry.getKey() + "=" + (entry.getValue() == null ? "" : entry.getValue().toString()) + "\n");
                }
            }
        } catch (Exception e) {
            ConfigLogger.error("Failed to save INI file: " + file.getPath(), e, "IniConfigFileHandler", true);
            throw e;
        }
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
        Map<String, Object> map = new LinkedHashMap<>();
        if (!file.exists()) return map;

        Map<String, String> stringMap = new LinkedHashMap<>();
        String currentSection = "";

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith(";") ||
                    (line.startsWith("#") && !line.startsWith(COMMENT_MARKER))) {
                    continue;
                }
                if (line.equals("[__COMMENTS__]")) {
                    while ((line = reader.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        if (line.startsWith("[") && !line.equals("[__COMMENTS__]")) break;
                    }
                    if (line == null) break;
                }
                if (line.startsWith("[") && line.endsWith("]")) {
                    currentSection = line.substring(1, line.length() - 1);
                    continue;
                }
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String val = line.substring(idx + 1).trim();
                    String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
                    stringMap.put(fullKey, val);
                }
            }
        } catch (Exception e) {
            ConfigLogger.error("Failed to load INI file: " + file.getPath(), e, "IniConfigFileHandler", true);
            return map;
        }
        Map<String, Object> nested = (Map<String, Object>) Utils.unflatten(converterRegistry, stringMap);
        Object withTypes = ensureSectionType(nested);
        return withTypes instanceof Map ? (Map<String, Object>) withTypes : new LinkedHashMap<>();
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
                if (line.startsWith(";")) {
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
            ConfigLogger.error("Error in file: " + file.getPath(), e, "IniConfigFileHandler", true);
        }
        return comments;
    }

    @Nullable
    static String getCommentString(Map<String, String> comments, String currentSection, String lastComment, String line) {
        int idx = line.indexOf('=');
        if (idx > 0) {
            String key = line.substring(0, idx).trim();
            String fullKey = currentSection.isEmpty() ? key : currentSection + "." + key;
            if (lastComment != null) {
                comments.put(fullKey, lastComment);
                lastComment = null;
            }
        }
        return lastComment;
    }

    @Override
    public boolean supportsComments() {
        return true;
    }
}
