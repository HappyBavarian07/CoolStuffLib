package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class AdvancedConfigUtilsTest {
    @Test
    void testFlattenAndUnflattenSimple() {
        Map<String, Object> nested = new HashMap<>();
        nested.put("a", 1);
        nested.put("b", "str");
        Map<String, Object> sub = new HashMap<>();
        sub.put("x", 42);
        nested.put("sub", sub);
        ConfigTypeConverterRegistry reg = new ConfigTypeConverterRegistry();
        Map<String, Object> flat = Utils.flatten(reg, "", nested);
        assertEquals(3, flat.size());
        assertEquals(1, flat.get("a"));
        assertEquals("str", flat.get("b"));
        assertEquals(42, flat.get("sub.x"));
        // Unflatten
        Map<String, String> flatStr = new HashMap<>();
        flat.forEach((k, v) -> flatStr.put(k, v.toString()));
        Map<String, Object> rebuilt = (Map<String, Object>) Utils.unflatten(reg, flatStr);
        assertEquals(42, ((Map<?, ?>)rebuilt.get("sub")).get("x"));
        assertEquals(1, rebuilt.get("a"));
        assertEquals("str", rebuilt.get("b"));
    }

    @Test
    void testFlattenAndUnflattenList() {
        Map<String, Object> nested = new HashMap<>();
        List<String> list = Arrays.asList("a", "b", "c");
        nested.put("letters", list);
        ConfigTypeConverterRegistry reg = new ConfigTypeConverterRegistry();
        Map<String, Object> flat = Utils.flatten(reg, "", nested);
        assertEquals(3, flat.size());
        assertEquals("a", flat.get("letters.0"));
        assertEquals("b", flat.get("letters.1"));
        assertEquals("c", flat.get("letters.2"));
        // Unflatten
        Map<String, String> flatStr = new HashMap<>();
        flat.forEach((k, v) -> flatStr.put(k, v.toString()));
        Map<String, Object> rebuilt = (Map<String, Object>) Utils.unflatten(reg, flatStr);
        List<?> rebuiltList = (List<?>) rebuilt.get("letters");
        assertEquals(3, rebuiltList.size());
        assertEquals("a", rebuiltList.get(0));
        assertEquals("b", rebuiltList.get(1));
        assertEquals("c", rebuiltList.get(2));
    }

    @Test
    void testFlattenAndUnflattenMixed() {
        Map<String, Object> nested = new HashMap<>();
        Map<String, Object> sub = new HashMap<>();
        sub.put("foo", Arrays.asList(1, 2));
        nested.put("bar", sub);
        ConfigTypeConverterRegistry reg = new ConfigTypeConverterRegistry();
        Map<String, Object> flat = Utils.flatten(reg, "", nested);
        assertEquals(2, flat.size());
        assertEquals(1, flat.get("bar.foo.0"));
        assertEquals(2, flat.get("bar.foo.1"));
        // Unflatten
        Map<String, String> flatStr = new HashMap<>();
        flat.forEach((k, v) -> flatStr.put(k, v.toString()));
        Map<String, Object> rebuilt = (Map<String, Object>) Utils.unflatten(reg, flatStr);
        Map<?, ?> bar = (Map<?, ?>) rebuilt.get("bar");
        List<?> fooList = (List<?>) bar.get("foo");
        assertEquals(2, fooList.size());
        assertEquals("1", fooList.get(0).toString());
        assertEquals("2", fooList.get(1).toString());
    }

    @Test
    void testUnflattenInvalidKey() {
        Map<String, String> bad = Map.of("foo[notanumber]", "x");
        ConfigTypeConverterRegistry reg = new ConfigTypeConverterRegistry();
        Map<String, Object> rebuilt = (Map<String, Object>) Utils.unflatten(reg, bad);
        assertTrue(rebuilt.containsKey("foo[notanumber]"));
        assertEquals("x", rebuilt.get("foo[notanumber]"));
    }
}
