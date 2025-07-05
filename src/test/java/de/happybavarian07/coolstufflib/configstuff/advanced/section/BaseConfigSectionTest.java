package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BaseConfigSectionTest {

    @BeforeEach
    void setUp() {
        ConfigLogger.initialize(new File("test.log"));
    }

    @Test
    void testBasicOperations() {
        ConfigLogger.info("Running testBasicOperations", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("root");
        assertEquals("root", section.getName());
        assertEquals("", section.getFullPath());

        section.set("key1", "value1");
        section.set("key2", 42);
        section.set("key3", true);

        assertEquals("value1", section.get("key1"));
        assertEquals(42, section.get("key2"));
        assertEquals(true, section.get("key3"));

        assertTrue(section.contains("key1"));
        assertFalse(section.contains("nonexistent"));

        section.remove("key1");
        assertFalse(section.contains("key1"));

        Set<String> keys = section.getKeys(false);
        assertEquals(2, keys.size());
        assertTrue(keys.contains("key2"));
        assertTrue(keys.contains("key3"));
    }

    @Test
    void testNestedSections() {
        ConfigLogger.info("Running testNestedSections", "BaseConfigSectionTest", true);
        BaseConfigSection root = new BaseConfigSection("root");

        // Create nested sections
        ConfigSection user = root.createSection("user");
        user.set("name", "John");
        user.set("age", 30);

        ConfigSection address = user.createSection("address");
        address.set("city", "New York");
        address.set("zipcode", "10001");

        // Verify nested paths
        assertEquals("user", user.getFullPath());
        assertEquals("user.address", address.getFullPath());

        // Access values via parent
        assertEquals("John", root.get("user.name"));
        assertEquals(30, root.getInt("user.age"));
        assertEquals("New York", root.getString("user.address.city"));
        assertEquals("10001", root.getString("user.address.zipcode"));

        // Verify sections
        assertTrue(root.hasSection("user"));
        assertTrue(root.hasSection("user.address"));
        assertFalse(root.hasSection("nonexistent"));

        // Access sections
        ConfigSection retrievedUser = root.getSection("user");
        assertEquals("John", retrievedUser.get("name"));

        ConfigSection retrievedAddress = root.getSection("user.address");
        assertEquals("New York", retrievedAddress.get("city"));

        // Remove section
        root.removeSection("user.address");
        assertTrue(root.hasSection("user"));
        assertFalse(root.hasSection("user.address"));

        root.removeSection("user");
        assertFalse(root.hasSection("user"));
    }

    @Test
    void testTypedAccess() {
        ConfigLogger.info("Running testTypedAccess", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("config");

        section.set("string", "hello");
        section.set("integer", 42);
        section.set("double", 3.14);
        section.set("boolean", true);

        assertEquals("hello", section.getString("string"));
        assertEquals(42, section.getInt("integer"));
        assertEquals(3.14, section.getDouble("double"), 0.001);
        assertTrue(section.getBoolean("boolean"));

        // Test default values
        assertEquals("default", section.getString("nonexistent", "default"));
        assertEquals(100, section.getInt("nonexistent", 100));
        assertEquals(2.5, section.getDouble("nonexistent", 2.5), 0.001);
        assertFalse(section.getBoolean("nonexistent", false));

        // Test generic typed access
        assertEquals(Integer.valueOf(42), section.getValue("integer", Integer.class));
        assertEquals(Double.valueOf(3.14), section.getValue("double", Double.class));
        assertEquals(Boolean.TRUE, section.getValue("boolean", Boolean.class));

        // Test with Optional
        Optional<String> optString = section.getOptionalValue("string", String.class);
        Optional<String> optMissing = section.getOptionalValue("missing", String.class);

        assertTrue(optString.isPresent());
        assertEquals("hello", optString.get());
        assertFalse(optMissing.isPresent());
    }

    @Test
    void testCollectionTypes() {
        ConfigLogger.info("Running testCollectionTypes", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("data");

        // Test List
        List<String> stringList = Arrays.asList("one", "two", "three");
        section.set("strings", stringList);

        List<?> retrievedList = section.getList("strings");
        assertEquals(3, retrievedList.size());
        assertEquals("one", retrievedList.get(0));

        List<String> stringListWithDefault = section.getStringList("nonexistent", List.of("default"));
        assertEquals(1, stringListWithDefault.size());
        assertEquals("default", stringListWithDefault.get(0));

        // Test Map
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "value1");
        map.put("key2", 42);
        section.set("map", map);

        ConfigSection retrievedMap = section.getSection("map");
        assertEquals(2, retrievedMap.size());
        assertEquals("value1", retrievedMap.get("key1"));
        assertEquals(42, retrievedMap.get("key2"));
    }

    @Test
    void testDeepKeys() {
        ConfigLogger.info("Running testDeepKeys", "BaseConfigSectionTest", true);
        BaseConfigSection root = new BaseConfigSection("root");

        // Create a deep structure
        root.set("a", 1);

        ConfigSection b = root.createSection("b");
        b.set("c", 2);

        ConfigSection d = b.createSection("d");
        d.set("e", 3);

        ConfigSection f = root.createSection("f");
        f.set("g", 4);

        // Test non-recursive keys
        Set<String> shallowKeys = root.getKeys(false);
        assertEquals(3, shallowKeys.size());
        assertTrue(shallowKeys.contains("a"));
        assertTrue(shallowKeys.contains("b"));
        assertTrue(shallowKeys.contains("f"));

        // Test recursive keys
        Set<String> deepKeys = root.getKeys(true);
        assertEquals(7, deepKeys.size());
        assertTrue(deepKeys.contains("a"));
        assertTrue(deepKeys.contains("b"));
        assertTrue(deepKeys.contains("b.c"));
        assertTrue(deepKeys.contains("b.d"));
        assertTrue(deepKeys.contains("b.d.e"));
        assertTrue(deepKeys.contains("f.g"));
    }

    @Test
    void testMetadata() {
        ConfigLogger.info("Running testMetadata", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("settings");

        // Add metadata
        section.addMetadata("created", System.currentTimeMillis());
        section.addMetadata("author", "system");
        section.addMetadata("readonly", true);

        // Retrieve metadata
        assertTrue(section.hasMetadata("created"));
        assertEquals("system", section.getMetadata("author", String.class));
        assertTrue(section.getMetadata("readonly", Boolean.class));

        // Test metadata access doesn't affect values
        assertNull(section.get("created"));
        assertNull(section.get("author"));

        // Remove metadata
        section.removeMetadata("author");
        assertFalse(section.hasMetadata("author"));

        // Get all metadata
        Map<String, Object> allMeta = section.getMetadata();
        assertEquals(2, allMeta.size());
        assertTrue(allMeta.containsKey("created"));
        assertTrue(allMeta.containsKey("readonly"));

        // Clear all metadata
        section.clearMetadata();
        assertTrue(section.getMetadata().isEmpty());
    }

    @Test
    void testComments() {
        ConfigLogger.info("Running testComments", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("config");

        // Add comments
        section.setComment("key1", "This is the first key");
        section.set("key1", "value1");

        section.setComment("section1", "This is a section");
        ConfigSection subSection = section.createSection("section1");
        subSection.set("subkey", "subvalue");

        // Get comments
        assertEquals("This is the first key", section.getComment("key1"));
        assertEquals("This is a section", section.getComment("section1"));

        // Remove comment
        section.removeComment("key1");
        assertNull(section.getComment("key1"));

        // Get all comments
        section.setComment("key2", "Another comment");
        Map<String, String> comments = section.getComments();
        assertEquals(2, comments.size());
        assertEquals("This is a section", comments.get("section1"));
        assertEquals("Another comment", comments.get("key2"));
    }

    @Test
    void testMerge() {
        ConfigLogger.info("Running testMerge", "BaseConfigSectionTest", true);
        BaseConfigSection section1 = new BaseConfigSection("section1");
        section1.set("key1", "value1");
        section1.set("key2", "value2");
        ConfigSection sub1 = section1.createSection("sub");
        sub1.set("subkey1", "subvalue1");

        BaseConfigSection section2 = new BaseConfigSection("section2");
        section2.set("key2", "newvalue2");  // Should override
        section2.set("key3", "value3");     // Should be added
        ConfigSection sub2 = section2.createSection("sub");
        sub2.set("subkey1", "newsubvalue1"); // Should override
        sub2.set("subkey2", "subvalue2");    // Should be added

        // Merge section2 into section1
        section1.merge(section2);

        // Test override and additions at root level
        assertEquals("value1", section1.get("key1"));     // Unchanged
        assertEquals("newvalue2", section1.get("key2"));  // Overridden
        assertEquals("value3", section1.get("key3"));     // Added

        // Test subsection merging
        ConfigSection mergedSub = section1.getSection("sub");
        assertEquals("newsubvalue1", mergedSub.get("subkey1"));  // Overridden
        assertEquals("subvalue2", mergedSub.get("subkey2"));     // Added
    }

    @Test
    void testClone() {
        ConfigLogger.info("Running testClone", "BaseConfigSectionTest", true);
        BaseConfigSection original = new BaseConfigSection("original");
        original.set("key1", "value1");
        original.set("key2", 42);

        ConfigSection sub = original.createSection("sub");
        sub.set("subkey", "subvalue");

        original.setComment("key1", "A comment");
        original.addMetadata("meta", "data");

        // Clone the section
        BaseConfigSection clone = original.clone();

        // Verify basic properties
        assertEquals("original", clone.getName());
        assertEquals("value1", clone.get("key1"));
        assertEquals(42, clone.getInt("key2"));
        assertTrue(clone.hasSection("sub"));
        assertEquals("subvalue", clone.getString("sub.subkey"));

        // Verify comments and metadata were cloned
        assertEquals("A comment", clone.getComment("key1"));
        assertEquals("data", clone.getMetadata("meta", String.class));

        // Verify modifications to clone don't affect original
        clone.set("key1", "modified");
        clone.getSection("sub").set("subkey", "modified");

        assertEquals("value1", original.get("key1"));  // Original unchanged
        assertEquals("modified", clone.get("key1"));   // Clone changed

        assertEquals("subvalue", original.getString("sub.subkey"));  // Original unchanged
        assertEquals("modified", clone.getString("sub.subkey"));     // Clone changed
    }

    @Test
    void testToMap() {
        ConfigLogger.info("Running testToMap", "BaseConfigSectionTest", true);
        BaseConfigSection section = new BaseConfigSection("root");
        section.set("key1", "value1");
        section.set("key2", 42);

        ConfigSection sub = section.createSection("sub");
        sub.set("subkey1", "subvalue1");
        sub.set("subkey2", "subvalue2");

        List<String> list = Arrays.asList("a", "b", "c");
        section.set("list", list);

        // Convert to map
        Map<String, Object> map = section.toMap();

        // Verify root values
        assertEquals("value1", map.get("key1"));
        assertEquals(42, map.get("key2"));
        assertEquals(list, ((Map<?, ?>) map.get("list")).get("__items"));
        // Todo split tomap normal and tomap for serialization and file saving

        // Verify subsection was converted to nested map
        assertTrue(map.get("sub") instanceof Map);
        Map<?, ?> subMap = (Map<?, ?>) map.get("sub");
        assertEquals("subvalue1", subMap.get("subkey1"));
        assertEquals("subvalue2", subMap.get("subkey2"));
    }
}
