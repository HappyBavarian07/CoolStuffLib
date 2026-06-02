package de.happybavarian07.coolstufflib.menusystem;

import com.google.gson.Gson;
import de.happybavarian07.coolstufflib.menusystem.actions.MenuAction;
import de.happybavarian07.coolstufflib.service.api.Service;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * <p>Manages menu addons and global button specifications.</p>
 */
public class MenuAddonManager implements Service {
    private final UUID serviceId = UUID.randomUUID();
    private final Map<String, Map<String, MenuAddon>> menuAddonList = new HashMap<>();
    private final Map<String, AddonButtonSpec> globalButtonSpecs = new HashMap<>();
    private final Map<String, MenuAction> handlerIdRegistry = new HashMap<>();
    private boolean menuAddonManagerReady = false;

    @Override
    public UUID id() { return serviceId; }
    @Override
    public String serviceName() { return "menu-addon-manager"; }
    @Override
    public CompletableFuture<Void> init() { return CompletableFuture.completedFuture(null); }
    @Override
    public CompletableFuture<Void> shutdown() { return CompletableFuture.completedFuture(null); }

    public Map<String, Map<String, MenuAddon>> getMenuAddonList() {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager not ready");
        return menuAddonList;
    }

    /**
     * <p>Adds an addon to the manager.</p>
     *
     * @param addon The addon
     */
    public void addMenuAddon(MenuAddon addon) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager not ready");
        if (!menuAddonList.containsKey(addon.getMenu().getConfigMenuAddonFeatureName()))
            menuAddonList.put(addon.getMenu().getConfigMenuAddonFeatureName(), new HashMap<>());
        menuAddonList.get(addon.getMenu().getConfigMenuAddonFeatureName()).put(addon.getName(), addon);
    }

    /**
     * <p>Removes an addon.</p>
     *
     * @param menuName The menu name
     * @param name     The addon name
     * @return {@code true} if removed
     */
    public boolean removeMenuAddon(String menuName, String name) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager not ready");
        if (menuAddonList.isEmpty() || menuAddonList.get(menuName) == null || menuAddonList.get(menuName).isEmpty()) return false;
        if (!menuAddonList.get(menuName).containsKey(name)) return false;

        menuAddonList.get(menuName).remove(name);
        return true;
    }

    /**
     * <p>Gets all addons for a menu.</p>
     *
     * @param menuName The menu name
     * @return A map of addons
     */
    public Map<String, MenuAddon> getMenuAddons(String menuName) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager not ready");
        if (menuAddonList.isEmpty() || menuAddonList.get(menuName) == null || menuAddonList.get(menuName).isEmpty()) return new HashMap<>();

        return menuAddonList.get(menuName);
    }

    public boolean hasMenuAddon(String menuName, String addonName) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager not ready");
        if (menuAddonList.isEmpty() || menuAddonList.get(menuName) == null || menuAddonList.get(menuName).isEmpty()) return false;

        return menuAddonList.get(menuName).containsKey(addonName);
    }

    public void setMenuAddonManagerReady(boolean mamReady) {
        this.menuAddonManagerReady = mamReady;
    }

    public boolean isMenuAddonManagerReady() {
        return menuAddonManagerReady;
    }

    public static class AddonButtonSpec {
        public String menuTypeId;
        public Integer slot;
        public String serializedItem;
        public String handlerId;
        public Set<Integer> forbiddenSlots;
    }

    /**
     * <p>Registers a button specification globally.</p>
     *
     * @param spec   The specification
     * @param action The action to execute
     */
    public void registerGlobalButton(AddonButtonSpec spec, MenuAction action) {
        globalButtonSpecs.put(spec.handlerId, spec);
        handlerIdRegistry.put(spec.handlerId, action);
    }

    /**
     * <p>Persists global buttons to a file.</p>
     *
     * @param file The target file
     * @throws Exception If persistence fails
     */
    public void persistGlobalButtons(File file) throws Exception {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new ArrayList<>(globalButtonSpecs.values()), writer);
        }
    }

    /**
     * <p>Loads global buttons from a file.</p>
     *
     * @param file The source file
     * @throws Exception If loading fails
     */
    public void loadGlobalButtons(File file) throws Exception {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(file)) {
            AddonButtonSpec[] specs = gson.fromJson(reader, AddonButtonSpec[].class);
            for (AddonButtonSpec spec : specs) {
                globalButtonSpecs.put(spec.handlerId, spec);
            }
        }
    }

    public void rebindHandlers(Map<String, MenuAction> runtimeHandlers) {
        for (String handlerId : globalButtonSpecs.keySet()) {
            if (runtimeHandlers.containsKey(handlerId)) {
                handlerIdRegistry.put(handlerId, runtimeHandlers.get(handlerId));
            }
        }
    }
}
