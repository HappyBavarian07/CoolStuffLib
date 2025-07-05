package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.MalformedJsonException;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;
import java.util.regex.Pattern;

public class JsonConfigFileHandler extends AbstractConfigFileHandler {
    private static final Type MAP_TYPE = new TypeToken<Map<String, Object>>() {
    }.getType();
    private static final String COMMENTS_KEY = "__comments";
    private final Gson gson;
    private final ConfigTypeConverterRegistry converterRegistry;

    public JsonConfigFileHandler() {
        this(ConfigTypeConverterRegistry.defaultRegistry());
    }

    public JsonConfigFileHandler(ConfigTypeConverterRegistry registry) {
        super(Pattern.compile(".*\\.json$", Pattern.CASE_INSENSITIVE));
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.converterRegistry = registry;
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

    @Override
    protected void doSave(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        try (Writer writer = new FileWriter(file)) {
            Map<String, Object> nested = Utils.unflattenObjectMap(converterRegistry, data);
            Object withTypes = addSectionTypeFields(nested);
            if (!comments.isEmpty()) {
                Map<String, Object> commentsMap = new LinkedHashMap<>(comments);
                Map<String, Object> root = new LinkedHashMap<>();
                root.put(COMMENTS_KEY, commentsMap);
                root.putAll(withTypes instanceof Map ? (Map<String, Object>) withTypes : Collections.singletonMap("", withTypes));
                withTypes = root;
            }
            gson.toJson(withTypes, writer);
        }
    }

    @Override
    public Map<String, Object> doLoad(File file) {
        if (!file.exists()) return new HashMap<>();
        try (Reader reader = new FileReader(file)) {
            StringBuilder sb = new StringBuilder();
            char[] buf = new char[1024];
            int numRead;
            while ((numRead = reader.read(buf)) > 0) sb.append(buf, 0, numRead);
            String json = sb.toString();
            if (json.isEmpty()) return new HashMap<>();
            Object loaded = gson.fromJson(json, Object.class);
            Object withTypes = ensureSectionType(loaded);
            return withTypes instanceof Map ? (Map<String, Object>) withTypes : new HashMap<>();
        } catch (IOException | JsonSyntaxException e) {
            ConfigLogger.error("Error loading JSON file " + file.getName(), e, "JsonConfigFileHandler", true);
            return new HashMap<>();
        }
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
    public Map<String, String> loadComments(File file) {
        if (!file.exists()) return Collections.emptyMap();

        try (Reader reader = new FileReader(file)) {
            Map<String, Object> data = gson.fromJson(reader, MAP_TYPE);
            if (data == null) return Collections.emptyMap();

            // Extract comments
            if (data.containsKey(COMMENTS_KEY) && data.get(COMMENTS_KEY) instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, String> commentsMap = (Map<String, String>) data.get(COMMENTS_KEY);
                return expandComments(commentsMap);
            }

            return Collections.emptyMap();
        } catch (Exception e) {
            System.err.println("Error loading comments from JSON file " + file.getName() + ": " + e.getMessage());
            return Collections.emptyMap();
        }
    }

    private Map<String, String> flattenComments(Map<String, String> comments) {
        return new HashMap<>(comments);
    }

    private Map<String, String> expandComments(Map<String, String> flatComments) {
        return new HashMap<>(flatComments);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> flatten(String prefix, Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            if (key.equals(COMMENTS_KEY)) continue;
            Object value = entry.getValue();
            String newKey = prefix.isEmpty() ? key : prefix + "." + key;
            if (value instanceof Map) {
                result.putAll(flatten(newKey, (Map<String, Object>) value));
            } else if (value instanceof List) {
                result.put(newKey, unflattenList((List<Object>) value));
            } else {
                result.put(newKey, value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Object unflatten(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (key.contains(".")) {
                String[] parts = key.split("\\.", 2);
                Map<String, Object> nested = (Map<String, Object>) result.computeIfAbsent(parts[0], k -> new HashMap<>());
                Map<String, Object> subMap = new HashMap<>();
                subMap.put(parts[1], value);
                Object subUnflattened = unflatten(subMap);
                if (subUnflattened instanceof Map) {
                    nested.putAll((Map<String, Object>) subUnflattened);
                } else {
                    nested.put(parts[1], subUnflattened);
                }
            } else {
                result.put(key, value);
            }
        }
        boolean allIntKeys = result.keySet().stream().allMatch(k -> k != null && k.matches("\\d+"));
        if (allIntKeys && !result.isEmpty()) {
            int max = result.keySet().stream().mapToInt(k -> Integer.parseInt((String) k)).max().orElse(-1);
            List<Object> list = new ArrayList<>();
            for (int i = 0; i <= max; i++) {
                list.add(result.get(String.valueOf(i)));
            }
            return list;
        }
        return result;
    }

    private List<Object> unflattenList(List<Object> list) {
        List<Object> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof Map) {
                result.add(unflatten((Map<String, Object>) item));
            } else if (item instanceof List) {
                result.add(unflattenList((List<Object>) item));
            } else {
                result.add(item);
            }
        }
        return result;
    }

    @Override
    public boolean supportsComments() {
        return false;
    }
}