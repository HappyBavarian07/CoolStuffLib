package de.happybavarian07.coolstufflib.menusystem;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:06
 */

import java.util.HashMap;
import java.util.Map;

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
}
