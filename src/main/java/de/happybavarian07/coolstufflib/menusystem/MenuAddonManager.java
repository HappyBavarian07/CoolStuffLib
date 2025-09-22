package de.happybavarian07.coolstufflib.menusystem;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:06
 */

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import com.google.gson.Gson;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;

public class MenuAddonManager {
    private final Map<String, Map<String, MenuAddon>> menuAddonList = new HashMap<>();
    private boolean menuAddonManagerReady = false;

    public Map<String, Map<String, MenuAddon>> getMenuAddonList() {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager (MAM) not ready to use yet. The Start Method has not been called yet.");
        return menuAddonList;
    }

    public void addMenuAddon(MenuAddon addon) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager (MAM) not ready to use yet. The Start Method has not been called yet.");
        if (!menuAddonList.containsKey(addon.getMenu().getConfigMenuAddonFeatureName()))
            menuAddonList.put(addon.getMenu().getConfigMenuAddonFeatureName(), new HashMap<>());
        menuAddonList.get(addon.getMenu().getConfigMenuAddonFeatureName()).put(addon.getName(), addon);
    }

    public boolean removeMenuAddon(String menuName, String name) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager (MAM) not ready to use yet. The Start Method has not been called yet.");
        if (menuAddonList.isEmpty() || menuAddonList.get(menuName) == null || menuAddonList.get(menuName).isEmpty()) return false;
        if (!menuAddonList.get(menuName).containsKey(name)) return false;

        menuAddonList.get(menuName).remove(name);
        return true;
    }

    public Map<String, MenuAddon> getMenuAddons(String menuName) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager (MAM) not ready to use yet. The Start Method has not been called yet.");
        if (menuAddonList.isEmpty() || menuAddonList.get(menuName) == null || menuAddonList.get(menuName).isEmpty()) return new HashMap<>();

        return menuAddonList.get(menuName);
    }

    public boolean hasMenuAddon(String menuName, String addonName) {
        if(!menuAddonManagerReady) throw new RuntimeException("MenuAddonManager (MAM) not ready to use yet. The Start Method has not been called yet.");
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

    private final Map<String, AddonButtonSpec> globalButtonSpecs = new HashMap<>();
    private final Map<String, MenuAction> handlerIdRegistry = new HashMap<>();

    public void registerGlobalButton(AddonButtonSpec spec, MenuAction action) {
        globalButtonSpecs.put(spec.handlerId, spec);
        handlerIdRegistry.put(spec.handlerId, action);
    }

    public void persistGlobalButtons(File file) throws Exception {
        Gson gson = new Gson();
        try (FileWriter writer = new FileWriter(file)) {
            gson.toJson(new ArrayList<>(globalButtonSpecs.values()), writer);
        }
    }

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
            } else {
                // log warning or handle missing handler
            }
        }
    }
}
