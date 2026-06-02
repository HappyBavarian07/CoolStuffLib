package de.happybavarian07.coolstufflib.menusystem.pagination;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * <p>Defines which inventory slots are used by a paginated data area.</p>
 * <p>Supports both contiguous ranges and explicit slot lists.</p>
 */
public class PaginationZone {
    private final int[] slots;

    /**
     * <p>Creates a contiguous slot range (inclusive).</p>
     *
     * @param startSlot First slot (inclusive)
     * @param endSlot   Last slot (inclusive)
     */
    public PaginationZone(int startSlot, int endSlot) {
        if (startSlot < 0 || endSlot < 0) throw new IllegalArgumentException("Slots must be >= 0");
        if (endSlot < startSlot) throw new IllegalArgumentException("endSlot must be >= startSlot");
        int length = endSlot - startSlot + 1;
        this.slots = new int[length];
        for (int i = 0; i < length; i++) this.slots[i] = startSlot + i;
    }

    /**
     * <p>Creates a zone from explicit slots.</p>
     *
     * @param slots Target slots in display order
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
     * <p>Returns the configured slots in display order.</p>
     *
     * @return Copied slot array
     */
    public int[] getSlots() {
        return Arrays.copyOf(slots, slots.length);
    }
}
