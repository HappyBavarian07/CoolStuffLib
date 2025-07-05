package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers;

import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.AbstractConfigFileHandler;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ArrayList;

public class InMemoryConfigFileHandler extends AbstractConfigFileHandler {
    public InMemoryConfigFileHandler() {
        super(Pattern.compile(".*", Pattern.CASE_INSENSITIVE));
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
    public void save(File file, Map<String, Object> data) throws IOException {
        ConfigLogger.warn("InMemoryConfigFileHandler: save() called but this handler does not persist data to files.", "InMemoryConfigFileHandler", true);
    }

    @Override
    public void save(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        ConfigLogger.warn("InMemoryConfigFileHandler: save() with comments called but this handler does not persist data to files.", "InMemoryConfigFileHandler", true);
    }

    @Override
    public Map<String, Object> load(File file) throws IOException {
        ConfigLogger.warn("InMemoryConfigFileHandler: load() called but this handler does not load data from files.", "InMemoryConfigFileHandler", true);
        return Map.of();
    }

    @Override
    public Map<String, String> loadComments(File file) throws IOException {
        ConfigLogger.warn("InMemoryConfigFileHandler: loadComments() called but this handler does not load comments from files.", "InMemoryConfigFileHandler", true);
        return Map.of();
    }

    @Override
    protected void doSave(File file, Map<String, Object> data) throws IOException {
    }

    @Override
    protected Map<String, Object> doLoad(File file) throws IOException {
        return Map.of();
    }

    @Override
    public boolean supportsComments() {
        return false;
    }

    @Override
    public boolean canHandle(File file) {
        return false;
    }
}
