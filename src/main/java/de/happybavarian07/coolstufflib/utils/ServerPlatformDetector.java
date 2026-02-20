package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;

import java.util.logging.Logger;

public class ServerPlatformDetector {

    private static final Logger LOGGER = Bukkit.getLogger();

    private static Boolean isHybridServer = null;
    private static String platformName = null;
    private static boolean detectionLogged = false;

    private static final String[] HYBRID_PLATFORMS = {
        "Cardboard",
        "Mohist",
        "Banner",
        "Arclight",
        "Magma",
        "CatServer"
    };

    private static final String[][] PLATFORM_CLASSES = {
        {"org.cardboardpowered.impl.inventory.CardboardInventoryView", "Cardboard"},
        {"com.mohistmc.banner.bukkit.inventory.BannerInventoryView", "Banner/Mohist"},
        {"io.izzel.arclight.common.bridge.core.entity.player.ServerPlayerEntityBridge", "Arclight"},
        {"org.magmafoundation.magma.inventory.MagmaInventoryView", "Magma"},
        {"catserver.server.inventory.CatServerInventoryView", "CatServer"}
    };

    public static boolean isHybridServer() {
        if (isHybridServer == null) {
            detectPlatform();
        }
        return isHybridServer;
    }

    public static String getPlatformName() {
        if (platformName == null) {
            detectPlatform();
        }
        return platformName;
    }

    public static boolean requiresInventoryViewCompat() {
        return isHybridServer();
    }

    private static void detectPlatform() {
        String version = Bukkit.getVersion().toLowerCase();
        String name = Bukkit.getName().toLowerCase();
        String serverVersion = Bukkit.getServer().getVersion().toLowerCase();

        boolean detected = false;

        for (String platform : HYBRID_PLATFORMS) {
            if (version.contains(platform.toLowerCase()) ||
                name.contains(platform.toLowerCase()) ||
                serverVersion.contains(platform.toLowerCase())) {
                platformName = platform;
                isHybridServer = true;
                detected = true;
                break;
            }
        }

        if (!detected) {
            for (String[] platformClass : PLATFORM_CLASSES) {
                try {
                    Class.forName(platformClass[0]);
                    platformName = platformClass[1];
                    isHybridServer = true;
                    detected = true;
                    break;
                } catch (ClassNotFoundException ignored) {
                }
            }
        }

        if (!detected) {
            platformName = "Spigot/Paper";
            isHybridServer = false;
        }

        if (!detectionLogged) {
            if (isHybridServer) {
                LOGGER.info("[CoolStuffLib] Detected hybrid server: " + platformName);
                LOGGER.info("[CoolStuffLib] Enabling inventory compatibility mode");
            } else {
                LOGGER.info("[CoolStuffLib] Detected standard server: " + platformName);
            }
            detectionLogged = true;
        }
    }

    public static void resetDetection() {
        isHybridServer = null;
        platformName = null;
        detectionLogged = false;
    }
}

