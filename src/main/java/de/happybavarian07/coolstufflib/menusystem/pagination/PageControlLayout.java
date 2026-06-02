package de.happybavarian07.coolstufflib.menusystem.pagination;

/**
 * <p>Holds slots used for page controls in a menu or zone.</p>
 */
public class PageControlLayout {
    private final int previousSlot;
    private final int closeSlot;
    private final int nextSlot;
    private final int refreshSlot;

    /**
     * <p>Creates a control layout.</p>
     *
     * @param previousSlot Slot for the previous button
     * @param closeSlot    Slot for the close button
     * @param nextSlot     Slot for the next button
     * @param refreshSlot  Slot for the refresh button
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
