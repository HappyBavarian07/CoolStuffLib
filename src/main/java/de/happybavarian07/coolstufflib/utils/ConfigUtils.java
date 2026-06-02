package de.happybavarian07.coolstufflib.utils;

import com.google.common.base.Strings;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.*;

/**
 * <p>Utilities for configuration flattening, serialization, and parsing.</p>
 */
public final class ConfigUtils {

    private ConfigUtils() {}

    public static Map<String, Object> flatten(ConfigTypeConverterRegistry registry, String prefix, Object value) {
        Map<String, Object> map = new HashMap<>();
        if (value instanceof Map m && m.containsKey("__type__")) {
            for (Object k : m.keySet()) {
                String key = String.valueOf(k);
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, m.get(key)));
            }
            return map;
        } 
        
        if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                String key = entry.getKey();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, entry.getValue()));
            }
            return map;
        } 
        
        if (value instanceof Collection) {
            int index = 0;
            for (Object item : (Collection<?>) value) {
                map.putAll(flatten(registry, prefix + "." + index++, item));
            }
            return map;
        } 
        
        if (value instanceof ConfigSection section) {
            for (Map.Entry<String, Object> entry : section.toSerializableMap().entrySet()) {
                String key = entry.getKey();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, entry.getValue()));
            }
            return map;
        } 
        
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            map.put(prefix, registry.tryToSerialized(value));
            return map;
        } 
        
        if (value == null || Strings.isNullOrEmpty(value.toString())) {
            map.put(prefix, null);
            return map;
        } 
        
        map.put(prefix, registry.tryToSerialized(value));
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Object unflatten(ConfigTypeConverterRegistry registry, Map<String, String> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            insertUnflattened(result, entry.getKey().split("\\."), 0, entry.getValue(), registry);
        }
        Object converted = convertMapsToLists(result);
        if (converted instanceof Map) return converted;
        
        Map<String, Object> fallback = new HashMap<>();
        fallback.put("root", converted);
        return fallback;
    }

    private static Object convertMapsToLists(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            List<Integer> intKeys = new ArrayList<>();
            for (Object k : m.keySet()) {
                if (k instanceof String s && s.matches("\\d+")) {
                    intKeys.add(Integer.parseInt(s));
                } else {
                    Map<String, Object> newMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        newMap.put(String.valueOf(e.getKey()), convertMapsToLists(e.getValue()));
                    }
                    return newMap;
                }
            }
            List<Object> list = new ArrayList<>();
            int max = intKeys.stream().max(Integer::compareTo).orElse(-1);
            for (int i = 0; i <= max; i++) {
                list.add(convertMapsToLists(m.get(String.valueOf(i))));
            }
            return list;
        } 
        
        if (obj instanceof List<?> l) {
            List<Object> newList = new ArrayList<>();
            for (Object o : l) newList.add(convertMapsToLists(o));
            return newList;
        }
        
        return obj;
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> unflattenObjectMap(ConfigTypeConverterRegistry registry, Map<String, Object> map) {
        boolean isFlat = map.keySet().stream().noneMatch(k -> k.contains("."));
        Map<String, Object> nested = isFlat ? (Map<String, Object>) unflatten(registry, toStringMapIfNeeded(map)) : map;
        processSectionsRecursive(nested);
        return nested;
    }

    @SuppressWarnings("unchecked")
    public static List<Object> unflattenObjectList(ConfigTypeConverterRegistry registry, Map<String, Object> map) {
        Object converted = convertMapsToLists(unflatten(registry, toStringMapIfNeeded(map)));
        if (converted instanceof List) return (List<Object>) converted;
        
        List<Object> fallback = new ArrayList<>();
        fallback.add(converted);
        return fallback;
    }

    public static Map<String, Object> convertMapsToListsMap(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                result.put(entry.getKey(), convertMapsToLists(value));
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void processSectionsRecursive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> m && m.containsKey("__type__")) {
                String type = String.valueOf(m.get("__type__"));
                if ("ListSection".equals(type)) {
                    ListSection section = new ListSection("");
                    Object items = m.get("__items");
                    if (items instanceof List<?>) {
                        section.fromList((List<?>) items);
                    } else if (items instanceof Map<?, ?> itemMap) {
                        TreeMap<Integer, Object> ordered = new TreeMap<>();
                        for (Map.Entry<?, ?> e : itemMap.entrySet()) {
                            String k = String.valueOf(e.getKey());
                            if (k.matches("\\d+")) ordered.put(Integer.parseInt(k), e.getValue());
                        }
                        section.fromList(new ArrayList<>(ordered.values()));
                    } else {
                        TreeMap<Integer, Object> ordered = new TreeMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                            String k = String.valueOf(e.getKey());
                            if (k.startsWith("__items.")) {
                                try {
                                    ordered.put(Integer.parseInt(k.substring(8)), e.getValue());
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                        if (!ordered.isEmpty()) section.fromList(new ArrayList<>(ordered.values()));
                    }
                    entry.setValue(section);
                } else if ("MapSection".equals(type)) {
                    MapSection section = new MapSection("");
                    Map<String, Object> subMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        String k = String.valueOf(e.getKey());
                        if (!"__type__".equals(k)) subMap.put(k, e.getValue());
                    }
                    processSectionsRecursive(subMap);
                    section.fromMap(subMap);
                    entry.setValue(section);
                } else if ("SetSection".equals(type)) {
                    SetSection section = new SetSection("");
                    Object items = m.get("__items");
                    if (items instanceof List<?>) section.fromSet(new HashSet<>((List<?>) items));
                    entry.setValue(section);
                }
            } else if (value instanceof Map<?, ?> subMap) {
                processSectionsRecursive((Map<String, Object>) subMap);
            }
        }
    }

    private static Map<String, String> toStringMapIfNeeded(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            result.put(entry.getKey(), v == null ? null : v instanceof String ? (String) v : v.toString());
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static void insertUnflattened(Map<String, Object> current, String[] parts, int index, Object value, ConfigTypeConverterRegistry registry) {
        String part = parts[index];
        int last = parts.length - 1;
        int dotIdx = part.indexOf('[');
        if (dotIdx != -1 && part.endsWith("]")) {
            String key = part.substring(0, dotIdx);
            String idxStr = part.substring(dotIdx + 1, part.length() - 1);
            int idx;
            try {
                idx = Integer.parseInt(idxStr);
            } catch (NumberFormatException e) {
                current.put(part, convertValue(registry, value));
                return;
            }
            List<Object> list = (List<Object>) current.computeIfAbsent(key, k -> new ArrayList<>());
            while (list.size() <= idx) list.add(null);
            
            if (index == last) {
                list.set(idx, convertValue(registry, value));
            } else {
                Object next = list.get(idx);
                if (!(next instanceof Map)) {
                    next = new HashMap<String, Object>();
                    list.set(idx, next);
                }
                insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
            }
            return;
        }
        
        if (index == last) {
            current.put(part, convertValue(registry, value));
            return;
        }
        
        Object next = current.computeIfAbsent(part, k -> new HashMap<String, Object>());
        if (!(next instanceof Map)) {
            next = new HashMap<String, Object>();
            current.put(part, next);
        }
        insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
    }

    public static Object convertValue(ConfigTypeConverterRegistry registry, Object value) {
        if (!(value instanceof String stringValue)) return registry.tryFromSerialized(value);
        
        try {
            if (stringValue.matches("^-?\\d+$")) return Integer.parseInt(stringValue);
            if (stringValue.matches("^-?\\d+\\.\\d+$")) return Double.parseDouble(stringValue);
            if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) return Boolean.parseBoolean(stringValue);
            if (stringValue.matches("^-?\\d+L$")) return Long.parseLong(stringValue.substring(0, stringValue.length() - 1));
            if (stringValue.matches("^-?\\d+\\.\\d+F$")) return Float.parseFloat(stringValue.substring(0, stringValue.length() - 1));
            
            if (stringValue.matches("^-?\\d+[SCB]$")) {
                char typeChar = Character.toUpperCase(stringValue.charAt(stringValue.length() - 1));
                String num = stringValue.substring(0, stringValue.length() - 1);
                switch (typeChar) {
                    case 'S': return Short.parseShort(num);
                    case 'C': return stringValue.charAt(0);
                    case 'B': return Byte.parseByte(num);
                }
            }
        } catch (NumberFormatException ignored) {}
        
        Object converted = registry.tryFromSerialized(stringValue);
        return Objects.requireNonNullElse(converted, stringValue);
    }

    public static Map<String, String> parseMap(String mapStr) {
        Map<String, String> map = new HashMap<>();
        String[] entries = mapStr.split(",");
        for (String entry : entries) {
            String[] keyValue = entry.split("=");
            map.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return map;
    }

    public static Object recursiveConvertForSerialization(Object value, ConfigTypeConverterRegistry registry) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey(), recursiveConvertForSerialization(entry.getValue(), registry));
            }
            return result;
        }
        if (value instanceof Iterable<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object v : list) result.add(recursiveConvertForSerialization(v, registry));
            return result;
        }
        Object serialized = registry.tryToSerialized(value);
        if (serialized != null && serialized != value) {
            if (serialized instanceof Map<?, ?> || serialized instanceof Iterable<?>) {
                return recursiveConvertForSerialization(serialized, registry);
            }
            return serialized;
        }
        return value;
    }

    public static Object recursiveConvertFromSerialization(Object value, ConfigTypeConverterRegistry registry) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey(), recursiveConvertFromSerialization(entry.getValue(), registry));
            }
            return result;
        }
        if (value instanceof Iterable<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object v : list) result.add(recursiveConvertFromSerialization(v, registry));
            return result;
        }
        Object converted = registry.tryFromSerialized(value);
        return converted != null ? converted : value;
    }

    public static boolean parseBoolean(String val) {
        if (val == null || val.isEmpty()) return false;
        String lower = val.toLowerCase(Locale.ROOT);
        return lower.equals("true") || lower.equals("yes") || lower.equals("1") || lower.equals("on") || lower.equals("enabled");
    }

    public static Number parseNumber(String number) {
        if (number == null || number.isEmpty()) return null;
        try {
            if (number.matches("^-?\\d+$")) return Integer.parseInt(number);
            if (number.matches("^-?\\d+\\.\\d+$")) return Double.parseDouble(number);
            if (number.matches("^-?\\d+L$")) return Long.parseLong(number.substring(0, number.length() - 1));
            if (number.matches("^-?\\d+\\.\\d+F$")) return Float.parseFloat(number.substring(0, number.length() - 1));
            if (number.matches("^-?\\d+[SCB]$")) {
                char typeChar = Character.toUpperCase(number.charAt(number.length() - 1));
                String num = number.substring(0, number.length() - 1);
                switch (typeChar) {
                    case 'S': return Short.parseShort(num);
                    case 'B': return Byte.parseByte(num);
                }
            }
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void logMalformedLine(File file, int lineNum, String line, String message) {
        StringBuilder context = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int current = 1;
            String l;
            while ((l = reader.readLine()) != null) {
                if (current >= lineNum - 2 && current <= lineNum + 2) {
                    context.append(current).append(": ").append(l).append("\n");
                }
                if (current > lineNum + 2) break;
                current++;
            }
        } catch (Exception ignored) {}
        ConfigLogger.error("Malformed line in file: " + file.getPath() + " at line " + lineNum + ": " + message + "\nContext:\n" + context, null, "TomlConfigFileHandler", true);
    }

    public static void logError(String message, Exception e, String source, boolean console) {
        ConfigLogger.error(message, e, source, console);
    }
}
