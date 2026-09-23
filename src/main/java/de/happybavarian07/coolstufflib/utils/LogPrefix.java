package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 28.07.2023 | 11:00
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;

public enum LogPrefix {
    ACTIONSLOGGER_PLAYER("ActionsLogger - Player", "Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PLAYER"),
    ACTIONSLOGGER_SERVER("ActionsLogger - Server", "Plugin.LogActions.IndividualActions.ACTIONSLOGGER_SERVER"),
    ACTIONSLOGGER_PANEL("ActionsLogger - Panel", "Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PANEL"),
    ACTIONSLOGGER_PLUGIN("ActionsLogger - Plugin", "Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PLUGIN"),
    COOLSTUFFLIB("CoolStuffLib", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB"),
    COOLSTUFFLIB_MAIN("CoolStuffLib - Main", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB_MAIN"),
    COOLSTUFFLIB_COMMANDS("CoolStuffLib - Commands", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB_COMMANDS"),
    COOLSTUFFLIB_GUI("CoolStuffLib - GUI", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB_GUI"),
    COOLSTUFFLIB_LISTENER("CoolStuffLib - Listener", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB_LISTENER"),
    COOLSTUFFLIB_UTILS("CoolStuffLib - Utils", "Plugin.LogActions.IndividualActions.COOLSTUFFLIB_UTILS"),
    DATELOGGER("DateLogger", "Plugin.LogActions.IndividualActions.DATELOGGER"),
    API("API", "Plugin.LogActions.IndividualActions.API"),
    COMMANDS("Commands", "Plugin.LogActions.IndividualActions.COMMANDS"),
    CONFIG("Config", "Plugin.LogActions.IndividualActions.CONFIG"),
    DATABASE("Database", "Plugin.LogActions.IndividualActions.DATABASE"),
    DEBUG("Debug", "Plugin.LogActions.IndividualActions.DEBUG"),
    ERROR("Error", "Plugin.LogActions.IndividualActions.ERROR"),
    WARNING("Warning","Plugin.LogActions.IndividualActions.WARNING"),
    FILE("File", "Plugin.LogActions.IndividualActions.FILE"),
    INFO("Info", "Plugin.LogActions.IndividualActions.INFO"),
    INITIALIZER("Initializer", "Plugin.LogActions.IndividualActions.INITIALIZER"),
    VAULT_MONEY("Vault - Money", "Plugin.LogActions.IndividualActions.VAULT_MONEY"),
    VAULT_PERMISSION("Vault - Permission", "Plugin.LogActions.IndividualActions.VAULT_PERMISSION"),
    VAULT_CHAT("Vault - Chat", "Plugin.LogActions.IndividualActions.VAULT_CHAT"),
    VAULT_ECONOMY("Vault - Economy", "Plugin.LogActions.IndividualActions.VAULT_ECONOMY"),
    VAULT_PLUGIN("Vault - Plugin", "Plugin.LogActions.IndividualActions.VAULT_PLUGIN"),
    UPDATER("Updater", "Plugin.LogActions.IndividualActions.UPDATER");

    private final String logPrefix;
    private final String configPath;
    private boolean enabled = true;

    LogPrefix(String logPrefix, String configPath) {
        this.logPrefix = logPrefix;
        this.configPath = configPath;
    }

    public static void setup() {
        CoolStuffLib lib = CoolStuffLib.getLib();
        if (lib == null) return;

        var config = lib.getJavaPluginUsingLib().getConfig();
        for (LogPrefix prefix : LogPrefix.values()) {
            prefix.enabled = config.getBoolean(prefix.configPath, true);
        }
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
