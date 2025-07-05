package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import com.google.gson.GsonBuilder;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import com.google.gson.Gson;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.utils.Utils;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;

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
        super(Pattern.compile(".*\\.json5$", Pattern.CASE_INSENSITIVE));
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.converterRegistry = registry;
    }

    public ConfigTypeConverterRegistry getConverterRegistry() {
        return converterRegistry;
    }

    @Override
    public void doSave(File file, Map<String, Object> data) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> nested = Utils.unflattenObjectMap(converterRegistry, data);
            Object plain = addSectionTypeFields(nested);
            gson.toJson(plain, writer);
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

    private Map<String, Object> convertSectionTypes(Object obj) {
        if (!(obj instanceof Map)) return new HashMap<>();
        Map<String, Object> map = (Map<String, Object>) obj;
        if (map.containsKey("__type__")) {
            String type = String.valueOf(map.get("__type__"));
            if ("MapSection".equals(type)) {
                MapSection section = new MapSection("");
                for (Map.Entry<String, Object> entry : map.entrySet()) {
                    if (!"__type__".equals(entry.getKey())) {
                        section.put(entry.getKey(), entry.getValue());
                    }
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("", section);
                return result;
            }
            if ("ListSection".equals(type)) {
                ListSection section = new ListSection("");
                Object values = map.get("values");
                if (values instanceof List<?>) {
                    for (Object v : (List<?>) values) section.add(v);
                }
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("", section);
                return result;
            }
            if ("SetSection".equals(type)) {
                SetSection section = new SetSection("");
                Object values = map.get("values");
                if (values instanceof List<?>) for (Object v : (List<?>) values) section.add(v);
                Map<String, Object> result = new LinkedHashMap<>();
                result.put("", section);
                return result;
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Map<String, Object> sub = convertSectionTypes(value);
                if (sub.size() == 1 && sub.containsKey("")) {
                    result.put(entry.getKey(), sub.get(""));
                } else {
                    result.put(entry.getKey(), sub);
                }
            } else if (value instanceof List<?> l) {
                List<Object> newList = new ArrayList<>();
                for (Object o : l) {
                    if (o instanceof Map) {
                        Map<String, Object> sub = convertSectionTypes(o);
                        if (sub.size() == 1 && sub.containsKey("")) newList.add(sub.get(""));
                        else newList.add(sub);
                    } else {
                        newList.add(o);
                    }
                }
                result.put(entry.getKey(), newList);
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
            Object loaded = gson.fromJson(json, Object.class);
            Object withTypes = ensureSectionType(loaded);
            return withTypes instanceof Map ? (Map<String, Object>) withTypes : new HashMap<>();
        } catch (Exception e) {
            ConfigLogger.error("Failed to parse JSON5 file: " + file.getPath(), e, "Json5ConfigFileHandler", true);
            return new HashMap<>();
        }
    }

    @Override
    public boolean supportsComments() {
        return false;
    }
}
