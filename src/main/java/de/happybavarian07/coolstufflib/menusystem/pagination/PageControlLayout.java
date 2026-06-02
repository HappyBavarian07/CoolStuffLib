package de.happybavarian07.coolstufflib.menusystem.pagination;

/**
 * Holds slots used for page controls in a menu or zone.
 */
public class PageControlLayout {
    private final int previousSlot;
    private final int closeSlot;
    private final int nextSlot;
    private final int refreshSlot;

    /**
     * Creates a control layout.
     *
     * @param previousSlot previous-page slot
     * @param closeSlot close slot
     * @param nextSlot next-page slot
     * @param refreshSlot refresh slot
     */
    public PageControlLayout(int previousSlot, int closeSlot, int nextSlot, int refreshSlot) {
        this.previousSlot = previousSlot;
        this.closeSlot = closeSlot;
        this.nextSlot = nextSlot;
        this.refreshSlot = refreshSlot;
    }

    public int getPreviousSlot() { return previousSlot; }
    public int getCloseSlot() { return closeSlot; }
    public int getNextSlot() { return nextSlot; }
    public int getRefreshSlot() { return refreshSlot; }
}
