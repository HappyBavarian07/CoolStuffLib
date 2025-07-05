package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class MapSectionTest {

    @Test
    void testBasicOperations() {
        ConfigLogger.info("Running testBasicOperations", "MapSectionTest", true);

        MapSection section = new MapSection("testMap");

        // Add entries
        section.put("key1", "value1");
        section.put("key2", 42);
        section.put("key3", true);

        // Test size and retrieval
        assertEquals(3, section.size());
        assertFalse(section.isEmpty());
        assertEquals("value1", section.getValue("key1"));
        assertEquals(42, section.getValue("key2"));
        assertEquals(true, section.getValue("key3"));

        // Test typed retrieval
        assertEquals("value1", section.getValue("key1", String.class));
        assertEquals(Integer.valueOf(42), section.getValue("key2", Integer.class));
        assertEquals(Boolean.TRUE, section.getValue("key3", Boolean.class));

        // Test default value
        assertEquals("default", section.getValue("nonexistent", "default", String.class));

        // Test containsKey and containsValue
        assertTrue(section.containsKey("key1"));
        assertTrue(section.containsValue("value1"));
        assertFalse(section.containsKey("nonexistent"));

        // Test removal
        section.removeValue("key1");
        assertEquals(2, section.size());
        assertFalse(section.containsKey("key1"));

        // Test clear
        section.clear();
        assertEquals(0, section.size());
        assertTrue(section.isEmpty());
    }

    @Test
    void testBulkOperations() {
        ConfigLogger.info("Running testBulkOperations", "MapSectionTest", true);

        MapSection section = new MapSection("bulkMap");

        // Test putAll
        Map<String, Object> entries = new HashMap<>();
        entries.put("keyA", "valueA");
        entries.put("keyB", "valueB");
        entries.put("keyC", "valueC");
        section.putAll(entries);

        assertEquals(3, section.size());
        assertEquals("valueA", section.getValue("keyA"));
        assertEquals("valueB", section.getValue("keyB"));
        assertEquals("valueC", section.getValue("keyC"));

        // Test getMapValues
        Map<String, Object> retrievedEntries = section.getMapValues();
        assertEquals(3, retrievedEntries.size());
        assertEquals("valueA", retrievedEntries.get("keyA"));

        // Test fromMap
        section.clear();
        Map<String, Object> newEntries = new HashMap<>();
        newEntries.put("x", 1);
        newEntries.put("y", 2);
        section.fromMap(newEntries);
        assertEquals(2, section.size());
        assertEquals(1, section.getValue("x"));
        assertEquals(2, section.getValue("y"));
    }

    @Test
    void testDualNature() {
        ConfigLogger.info("Running testDualNature", "MapSectionTest", true);

        MapSection section = new MapSection("dualNature");

        // Add map entries
        section.put("mapKey1", "mapValue1");
        section.put("mapKey2", "mapValue2");

        // We can also use the ConfigSection interface methods
        section.set("configKey1", "configValue1");
        section.set("configKey2", "configValue2");

        // Both types of values should be accessible via either interface
        assertEquals("mapValue1", section.getValue("mapKey1"));
        assertEquals("mapValue1", section.get("mapKey1"));
        assertEquals("configValue1", section.getValue("configKey1"));
        assertEquals("configValue1", section.get("configKey1"));

        // Size should reflect all entries
        assertEquals(4, section.size());
    }

    @Test
    void testToMap() {
        ConfigLogger.info("Running testToMap", "MapSectionTest", true);

        MapSection section = new MapSection("mapConversion");

        // Add map entries
        section.put("mapKey1", "mapValue1");
        section.put("mapKey2", "mapValue2");

        // Add via ConfigSection interface
        section.set("sectionKey1", "sectionValue1");

        // Create a nested section
        ConfigSection nested = section.createSection("nested");
        nested.set("nestedKey", "nestedValue");

        // Convert to map
        Map<String, Object> map = section.toMap();

        // Verify map entries are present
        assertEquals("mapValue1", map.get("mapKey1"));
        assertEquals("mapValue2", map.get("mapKey2"));
        assertEquals("sectionValue1", map.get("sectionKey1"));

        // Verify nested structure
        assertTrue(map.containsKey("nested"));
        Object nestedObj = map.get("nested");
        assertTrue(nestedObj instanceof Map);
        Map<?, ?> nestedMap = (Map<?, ?>) nestedObj;
        assertEquals("nestedValue", nestedMap.get("nestedKey"));
    }

    @Test
    void testMerge() {
        ConfigLogger.info("Running testMerge", "MapSectionTest", true);

        MapSection section1 = new MapSection("map1");
        section1.put("key1", "val1");
        section1.put("key2", "val2");
        section1.set("config1", "configVal1");

        MapSection section2 = new MapSection("map2");
        section2.put("key2", "newVal2");  // Should override
        section2.put("key3", "val3");    // Should add
        section2.set("config2", "configVal2");

        // Merge section2 into section1
        section1.merge(section2);

        // Verify map values are merged correctly
        assertEquals("val1", section1.getValue("key1"));      // Unchanged
        assertEquals("newVal2", section1.getValue("key2"));   // Overridden
        assertEquals("val3", section1.getValue("key3"));      // Added

        // Verify config values are merged
        assertEquals("configVal1", section1.get("config1"));  // Unchanged
        assertEquals("configVal2", section1.get("config2"));  // Added
    }

    @Test
    void testHierarchy() {
        ConfigLogger.info("Running testHierarchy", "MapSectionTest", true);

        // Create a parent section
        BaseConfigSection parent = new BaseConfigSection("parent");

        // Create a MapSection with the parent
        MapSection mapSection = new MapSection("map", parent);

        // Verify parent relationship
        assertEquals("parent", mapSection.getParent().getName());
        assertEquals("parent.map", mapSection.getFullPath());

        // Add entries to the map
        mapSection.put("mapKey", "mapValue");

        // Verify the parent has access to the child section
        assertTrue(parent.hasSection("map"));
        ConfigSection retrievedMap = parent.getSection("map");
        assertTrue(retrievedMap instanceof MapSection);

        // Add the map section to the parent's sections map
        parent.createSection("map");

        // Access map entries via parent
        assertEquals("mapValue", ((MapSection)parent.getSection("map")).getValue("mapKey"));
    }

    @Test
    void testNestedMapStructure() {
        ConfigLogger.info("Running testNestedMapStructure", "MapSectionTest", true);

        BaseConfigSection root = new BaseConfigSection("root");

        // Create a complex nested structure using maps
        MapSection user = root.createCustomSection("userInfo", MapSection.class);

        user.put("name", "John Doe");
        user.put("age", 30);

        MapSection address = user.createCustomSection("location", MapSection.class);

        address.put("street", "123 Main St");
        address.put("city", "Anytown");
        address.put("zipcode", "12345");

        // Test accessing nested values through map keys
        Object userObj = root.getSection("userInfo");
        assertInstanceOf(MapSection.class, userObj);
        MapSection retrievedUser = (MapSection) userObj;
        ConfigLogger.info("Retrieved user: " + retrievedUser, "MapSectionTest", true);
        ConfigLogger.info("User name: " + retrievedUser.toMap(), "MapSectionTest", true);

        assertEquals("John Doe", retrievedUser.getValue("name"));

        Object addressObj = retrievedUser.getValue("location");
        assertInstanceOf(MapSection.class, addressObj);
        MapSection retrievedAddress = (MapSection) addressObj;
        assertEquals("Anytown", retrievedAddress.getValue("city"));

        // Convert full structure to Map
        Map<String, Object> fullMap = root.toMap();
        assertInstanceOf(Map.class, fullMap.get("userInfo"));
        Map<?, ?> userMap = (Map<?, ?>) fullMap.get("userInfo");
        assertEquals("John Doe", userMap.get("name"));

        assertInstanceOf(Map.class, userMap.get("location"));
        Map<?, ?> addressMap = (Map<?, ?>) userMap.get("location");
        assertEquals("Anytown", addressMap.get("city"));
    }
}
