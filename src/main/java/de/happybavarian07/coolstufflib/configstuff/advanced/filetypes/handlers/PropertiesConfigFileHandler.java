package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.utils.Utils;

import java.io.*;
import java.util.*;
import java.util.regex.Pattern;

public class PropertiesConfigFileHandler extends AbstractConfigFileHandler {
    private final ConfigTypeConverterRegistry converterRegistry;
    private static final String COMMENTS_PREFIX = "# ";

    public PropertiesConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public PropertiesConfigFileHandler(ConfigTypeConverterRegistry registry) {
        super(Pattern.compile(".*\\.(properties|props)$", Pattern.CASE_INSENSITIVE));
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) {
        doSave(file, data, Collections.emptyMap());
    }

    @Override
    protected void doSave(File file, Map<String, Object> data, Map<String, String> comments) {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> flatMap = Utils.flatten(converterRegistry, "", addSectionTypeFields(data));
            for (Map.Entry<String, Object> entry : flatMap.entrySet()) {
                String key = entry.getKey();
                if (comments.containsKey(key)) {
                    writer.write(COMMENTS_PREFIX + comments.get(key).replace("\n", "\n" + COMMENTS_PREFIX) + "\n");
                }
                writer.write(key + "=" + (entry.getValue() == null ? "" : entry.getValue().toString()) + "\n");
            }
        } catch (IOException e) {
            ConfigLogger.error("I/O Error in file: " + file.getPath(), e, "PropertiesConfigFileHandler", true);
        } catch (Exception e) {
            ConfigLogger.error("Unexpected error in file: " + file.getPath(), e, "PropertiesConfigFileHandler", true);
        }
    }

    private Object addSectionTypeFields(Object obj) {
        if (obj instanceof MapSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("__type__", "MapSection");
            Map<String, Object> values = ((MapSection) obj).getMapValues();
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                map.put(entry.getKey(), addSectionTypeFields(entry.getValue()));
            }
            return map;
        }
        if (obj instanceof ListSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("__type__", "ListSection");
            List<?> values = ((ListSection) obj).toList();
            for (int i = 0; i < values.size(); i++) {
                map.put("items." + i, addSectionTypeFields(values.get(i)));
            }
            return map;
        }
        if (obj instanceof SetSection) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("__type__", "SetSection");
            int i = 0;
            for (Object v : ((SetSection) obj).toSet()) {
                map.put("items." + (i++), addSectionTypeFields(v));
            }
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
                    TreeMap<Integer, Object> ordered = new TreeMap<>();
                    for (Map.Entry<?, ?> entry : map.entrySet()) {
                        String k = String.valueOf(entry.getKey());
                        if (k.startsWith("__items.")) {
                            String idxStr = k.substring("__items.".length());
                            try {
                                int idx = Integer.parseInt(idxStr);
                                ordered.put(idx, ensureSectionType(entry.getValue()));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    for (Object v : ordered.values()) section.add(v);
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
        Map<String, Object> map = new LinkedHashMap<>();
        if (!file.exists()) return map;
        Properties properties = new Properties();
        try (Reader reader = new FileReader(file)) {
            properties.load(reader);
        } catch (Exception e) {
            ConfigLogger.error("Error in file: " + file.getPath(), e, "PropertiesConfigFileHandler", true);
            return map;
        }
        Map<String, Object> objectMap = new HashMap<>();
        for (String name : properties.stringPropertyNames()) {
            if (name.startsWith(COMMENTS_PREFIX)) continue;
            objectMap.put(name, properties.getProperty(name));
        }
        Map<String, Object> nested = Utils.unflattenObjectMap(converterRegistry, objectMap);
        Object withTypes = ensureSectionType(nested);
        return withTypes instanceof Map ? (Map<String, Object>) withTypes : new HashMap<>();
    }

    @Override
    protected Map<String, String> doLoadComments(File file) {
        Map<String, String> comments = new LinkedHashMap<>();
        if (!file.exists()) return comments;
        StringBuilder lastComment = null;
        int lineNum = 0;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                String trimmed = line.trim();
                if (trimmed.isEmpty()) continue;
                if (trimmed.startsWith("#")) {
                    String comment = trimmed.substring(1).trim();
                    if (lastComment == null) lastComment = new StringBuilder(comment);
                    else lastComment.append("\n").append(comment);
                    continue;
                }
                int idx = trimmed.indexOf('=');
                if (idx > 0) {
                    String key = trimmed.substring(0, idx).trim();
                    if (lastComment != null) {
                        comments.put(key, lastComment.toString());
                        lastComment = null;
                    }
                } else if (lastComment != null) {
                    Utils.logMalformedLine(file, lineNum, trimmed, "Expected key=value after comment");
                    lastComment = null;
                }
            }
        } catch (IOException e) {
            ConfigLogger.error("Error in file: " + file.getPath(), e, "PropertiesConfigFileHandler", true);
        }
        return comments;
    }

    @Override
    public boolean supportsComments() {
        return true;
    }
}
