package de.happybavarian07.coolstufflib.configstuff.advanced.section;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ListSectionTest {
    @BeforeAll
    static void setupLogger() {
        if (!isLoggerInitialized()) {
            ConfigLogger.initialize(new java.io.File("target"));
        }
    }
    private static boolean isLoggerInitialized() {
        try {
            ConfigLogger.getLogger();
            return true;
        } catch (IllegalStateException e) {
            return false;
        }
    }

    @Test
    void testBasicOperations() {
        ListSection section = new ListSection("testList");

        // Add items
        section.add("item1");
        section.add("item2");
        section.add("item3");

        // Test size and retrieval
        assertEquals(3, section.size());
        assertFalse(section.isEmpty());
        assertEquals("item1", section.get(0));
        assertEquals("item2", section.get(1));
        assertEquals("item3", section.get(2));

        // Test typed retrieval
        assertEquals("item1", section.get(0, String.class));

        // Test default value
        assertEquals("default", section.get(5, "default", String.class));

        ConfigLogger.info("ListSection size: " + section.size(), "ListSectionTest", true);
        ConfigLogger.info("ListSection items: " + section.toList(), "ListSectionTest", true);
        ConfigLogger.info("ListSection full path: " + section.getFullPath(), "ListSectionTest", true);
        ConfigLogger.info("ListSection parent: " + section.getParent(), "ListSectionTest", true);
        ConfigLogger.info("ListSection items as String: " + section.getItemsAs(String.class), "ListSectionTest", true);

        // Test contains
        assertTrue(section.contains("item2"));
        assertFalse(section.contains("nonexistent"));

        // Test update
        section.set(1, "updatedItem");
        assertEquals("updatedItem", section.get(1));

        // Test removal
        section.remove(0);
        assertEquals(2, section.size());
        assertEquals("updatedItem", section.get(0));

        // Test clear
        section.clear();
        assertEquals(0, section.size());
        assertTrue(section.isEmpty());
    }

    @Test
    void testBulkOperations() {
        ListSection section = new ListSection("bulkList");

        // Test addAll
        List<String> items = Arrays.asList("a", "b", "c");
        section.addAll(items);

        assertEquals(3, section.size());
        assertEquals("a", section.get(0));
        assertEquals("b", section.get(1));
        assertEquals("c", section.get(2));

        // Test getItems
        List<Object> retrievedItems = section.getItems();
        assertEquals(3, retrievedItems.size());
        assertEquals("a", retrievedItems.get(0));

        // Test getItemsAs with type
        List<String> stringItems = section.getItemsAs(String.class);
        assertEquals(3, stringItems.size());
        assertEquals("a", stringItems.get(0));

        // Test fromList
        section.clear();
        section.fromList(Arrays.asList(1, 2, 3));
        assertEquals(3, section.size());
        assertEquals(1, section.get(0));
    }

    @Test
    void testNestedValues() {
        ListSection section = new ListSection("nestedList");

        // We can add regular values to the list
        section.add("string");
        section.add(42);
        section.add(true);

        // Test retrieval
        assertEquals("string", section.get(0));
        assertEquals(42, section.get(1));
        assertEquals(true, section.get(2));

        // Values in the list can still be accessed via the ConfigSection interface
        section.set("key", "value");
        assertEquals("value", section.get("key"));

        // And we can have both list items and key-value pairs
        assertEquals(3, section.size());  // List size unchanged
        assertTrue(section.contains("key"));  // Key accessible
    }

    @Test
    void testToMap() {
        ListSection section = new ListSection("mapConversion");

        // Add list items
        section.add("item1");
        section.add("item2");

        // Add key-value pairs
        section.set("key1", "value1");
        section.set("key2", "value2");

        // Convert to map
        Map<String, Object> map = section.toMap();

        // Verify both types of data are present
        assertTrue(map.containsKey("key1"));
        assertEquals("value1", map.get("key1"));

        assertTrue(map.containsKey("key2"));
        assertEquals("value2", map.get("key2"));

        // Check list items are stored under the special key
        assertTrue(map.containsKey("__items"));
        Object itemsObj = map.get("__items");
        assertTrue(itemsObj instanceof List);

        List<?> items = (List<?>) itemsObj;
        assertEquals(2, items.size());
        assertEquals("item1", items.get(0));
        assertEquals("item2", items.get(1));
    }

    @Test
    void testMerge() {
        ListSection section1 = new ListSection("list1");
        section1.add("A");
        section1.add("B");
        section1.set("key1", "val1");

        ListSection section2 = new ListSection("list2");
        section2.add("C");
        section2.add("D");
        section2.set("key2", "val2");

        ConfigLogger.info("Initial section1: " + section1.toList(), "ListSectionTest", true);
        ConfigLogger.info("Initial section2: " + section2.toList(), "ListSectionTest", true);
        // Merge section2 into section1
        section1.merge(section2);

        ConfigLogger.info("Merged section1: " + section1.toList(), "ListSectionTest", true);
        ConfigLogger.info("Merged section2: " + section2.toList(), "ListSectionTest", true);

        // Verify list items are merged
        List<Object> mergedItems = section1.getItems();
        assertEquals(4, mergedItems.size());
        assertEquals("A", mergedItems.get(0));
        assertEquals("B", mergedItems.get(1));
        assertEquals("C", mergedItems.get(2));
        assertEquals("D", mergedItems.get(3));

        // Verify keys are merged
        assertEquals("val1", section1.get("key1"));
        assertEquals("val2", section1.get("key2"));
    }

    @Test
    void testHierarchy() {
        // Create a parent section
        BaseConfigSection parent = new BaseConfigSection("parent");

        // Create a ListSection with the parent
        ListSection listSection = new ListSection("list", parent);

        // Verify parent relationship
        assertEquals("parent", listSection.getParent().getName());
        assertEquals("parent.list", listSection.getFullPath());

        // Add items to the list
        listSection.add("item1");
        listSection.add("item2");

        // Verify the parent has access to the child section
        assertTrue(parent.hasSection("list"));
        ConfigSection retrievedList = parent.getSection("list");
        assertTrue(retrievedList instanceof ListSection);

        // Add the list section to the parent's sections map
        parent.createSection("list");

        // Access list items via parent
        assertEquals(2, ((ListSection)parent.getSection("list")).size());
    }
}
