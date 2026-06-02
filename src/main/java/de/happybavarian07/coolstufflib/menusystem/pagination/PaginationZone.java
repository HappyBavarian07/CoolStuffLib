package de.happybavarian07.coolstufflib.menusystem.pagination;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Defines which inventory slots are used by a paginated data area.
 * <p>
 * Supports both contiguous ranges and explicit slot lists.
 */
public class PaginationZone {
    private final int[] slots;

    /**
     * Creates a contiguous slot range (inclusive).
     *
     * @param startSlot first slot (inclusive)
     * @param endSlot last slot (inclusive)
     */
    public PaginationZone(int startSlot, int endSlot) {
        if (startSlot < 0 || endSlot < 0) throw new IllegalArgumentException("Slots must be >= 0");
        if (endSlot < startSlot) throw new IllegalArgumentException("endSlot must be >= startSlot");
        int length = endSlot - startSlot + 1;
        this.slots = new int[length];
        for (int i = 0; i < length; i++) this.slots[i] = startSlot + i;
    }

    /**
     * Creates a zone from explicit slots.
     *
     * @param slots target slots in display order
     */
    public PaginationZone(int... slots) {
        if (slots == null || slots.length == 0) throw new IllegalArgumentException("At least one slot is required");
        Set<Integer> uniqueSlots = new LinkedHashSet<>();
        for (int slot : slots) {
            if (slot < 0) throw new IllegalArgumentException("Slots must be >= 0");
            uniqueSlots.add(slot);
        }
        this.slots = new int[uniqueSlots.size()];
        int i = 0;
        for (Integer slot : uniqueSlots) this.slots[i++] = slot;
    }

    /**
     * Returns the configured slots in display order.
     *
     * @return copied slot array
     */
    public int[] getSlots() {
        return Arrays.copyOf(slots, slots.length);
    }
}
