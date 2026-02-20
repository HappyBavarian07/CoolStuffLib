package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;

import java.util.logging.Logger;

public class HybridInventoryUtils {

    private static final Logger LOGGER = Bukkit.getLogger();
    private static boolean compatibilityModeEnabled = false;
    private static boolean firstIncompatibilityDetected = false;

    public static InventoryView openInventorySafe(Player player, Inventory inventory) {
        if (player == null || inventory == null) {
            LOGGER.warning("[CoolStuffLib] Cannot open inventory: player or inventory is null");
            return null;
        }

        // lets disable all this till we fix it since this is kinda useless since it doesnt work at all most of the time anyways so just open inv normally
        return player.openInventory(inventory);

        /*
        if (compatibilityModeEnabled || ServerPlatformDetector.isHybridServer()) {
            return openInventoryReflective(player, inventory);
        }

        try {
            return player.openInventory(inventory);
        } catch (ClassCastException e) {
            if (!firstIncompatibilityDetected) {
                LOGGER.warning("[CoolStuffLib] ClassCastException detected during inventory opening");
                LOGGER.warning("[CoolStuffLib] Enabling compatibility mode for future operations");
                LOGGER.info("[CoolStuffLib] Using reflection for inventory operations (minor performance impact)");
                firstIncompatibilityDetected = true;
            }
            compatibilityModeEnabled = true;
            return openInventoryReflective(player, inventory);
        } catch (Exception e) {
            LOGGER.severe("[CoolStuffLib] Unexpected error opening inventory: " + e.getClass().getName());
            LOGGER.severe("[CoolStuffLib] Error message: " + e.getMessage());
            return null;
        }*/
    }

    private static InventoryView openInventoryReflective(Player player, Inventory inventory) {
        if (ServerPlatformDetector.getPlatformName().equals("Cardboard")) {
            return openInventoryCardboard(player, inventory);
        }

        try {
            var method = player.getClass().getMethod("openInventory", Inventory.class);
            method.setAccessible(true);
            Object result = method.invoke(player, inventory);

            if (result == null) {
                LOGGER.warning("[CoolStuffLib] openInventory returned null");
                return null;
            }

            if (result instanceof InventoryView) {
                return (InventoryView) result;
            } else {
                return new InventoryViewWrapper(result);
            }
        } catch (Exception e) {
            LOGGER.severe("[CoolStuffLib] Failed to open inventory using reflection");
            LOGGER.severe("[CoolStuffLib] Error: " + e.getClass().getName() + " - " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    private static InventoryView openInventoryCardboard(Player player, Inventory inventory) {
        try {
            Class<?> craftHumanEntityClass = player.getClass();

            var openCustomInventoryMethod = craftHumanEntityClass.getDeclaredMethod(
                "openCustomInventory",
                Inventory.class
            );
            openCustomInventoryMethod.setAccessible(true);

            Object result = openCustomInventoryMethod.invoke(player, inventory);

            if (result instanceof InventoryView) {
                return (InventoryView) result;
            }

            LOGGER.warning("[CoolStuffLib] Cardboard inventory opened but no view returned, creating wrapper");
            return createFallbackView(player, inventory);

        } catch (Exception e) {
            LOGGER.warning("[CoolStuffLib] Cardboard inventory opening failed: " + e.getMessage());
            if (e.getCause() != null) {
                LOGGER.fine("[CoolStuffLib] Cause: " + e.getCause().getMessage());
            }
            return createFallbackView(player, inventory);
        }
    }

    private static InventoryView createFallbackView(Player player, Inventory inventory) {
        try {
            player.closeInventory();

            var plugin = Bukkit.getPluginManager().getPlugin("CoolStuffLib");
            if (plugin != null) {
                Bukkit.getScheduler().runTaskLater(plugin, () -> {
                    try {
                        player.openInventory(inventory);
                    } catch (Exception ignored) {
                    }
                }, 1L);
            }

            return new FallbackInventoryView(player, inventory);
        } catch (Exception e) {
            LOGGER.severe("[CoolStuffLib] Failed to create fallback view");
            return null;
        }
    }

    public static <T> T safeCast(Object obj, Class<T> targetClass) {
        if (obj == null) {
            return null;
        }

        if (targetClass.isInstance(obj)) {
            return targetClass.cast(obj);
        }

        LOGGER.fine("[CoolStuffLib] safeCast: " + obj.getClass().getName() +
                    " is not instance of " + targetClass.getName());
        return null;
    }

    public static boolean needsReflectiveAccess() {
        return compatibilityModeEnabled || ServerPlatformDetector.isHybridServer();
    }

    public static boolean isCompatibilityModeEnabled() {
        return compatibilityModeEnabled;
    }

    public static void setCompatibilityMode(boolean enabled) {
        if (enabled && !compatibilityModeEnabled) {
            LOGGER.info("[CoolStuffLib] Compatibility mode manually enabled");
        } else if (!enabled && compatibilityModeEnabled) {
            LOGGER.info("[CoolStuffLib] Compatibility mode manually disabled");
        }
        compatibilityModeEnabled = enabled;
    }

    public static void resetCompatibilityMode() {
        compatibilityModeEnabled = false;
        firstIncompatibilityDetected = false;
    }
}

