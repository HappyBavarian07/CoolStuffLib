package de.happybavarian07.coolstufflib.commandmanagement;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PaginatedListTest {
    @Test
    void preparesSortedPages() {
        PaginatedList<String> pages = new PaginatedList<>(List.of("ten", "two", "one"))
                .maxItemsPerPage(2)
                .sort("alphabetic", true);

        assertEquals(2, pages.pageCount());
        assertEquals(List.of("one", "ten"), pages.page(1));
        assertEquals(List.of("two"), pages.page(2));
        assertTrue(pages.page(3).isEmpty());
    }

    @Test
    void rejectsInvalidPageSize() {
        assertThrows(IllegalArgumentException.class,
                () -> new PaginatedList<>(List.of("one")).maxItemsPerPage(0));
    }
}
