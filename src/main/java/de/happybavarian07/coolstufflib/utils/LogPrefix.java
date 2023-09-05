package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 28.07.2023 | 11:00
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;

public enum LogPrefix {
    ACTIONSLOGGER_PLAYER("ActionsLogger - Player", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PLAYER", true)),
    ACTIONSLOGGER_SERVER("ActionsLogger - Server", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.ACTIONSLOGGER_SERVER", true)),
    ACTIONSLOGGER_PANEL("ActionsLogger - Panel", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PANEL", true)),
    ACTIONSLOGGER_PLUGIN("ActionsLogger - Plugin", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.ACTIONSLOGGER_PLUGIN", true)),
    COOLSTUFFLIB("CoolStuffLib", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB", true)),
    COOLSTUFFLIB_MAIN("CoolStuffLib - Main", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB_MAIN", true)),
    COOLSTUFFLIB_COMMANDS("CoolStuffLib - Commands", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB_COMMANDS", true)),
    COOLSTUFFLIB_GUI("CoolStuffLib - GUI", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB_GUI", true)),
    COOLSTUFFLIB_LISTENER("CoolStuffLib - Listener", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB_LISTENER", true)),
    COOLSTUFFLIB_UTILS("CoolStuffLib - Utils", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COOLSTUFFLIB_UTILS", true)),
    DATELOGGER("DateLogger", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.DATELOGGER", true)),
    API("API", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.API", true)),
    COMMANDS("Commands", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.COMMANDS", true)),
    CONFIG("Config", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.CONFIG", true)),
    DATABASE("Database", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.DATABASE", true)),
    DEBUG("Debug", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.DEBUG", true)),
    ERROR("Error", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.ERROR", true)),
    WARNING("Error", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.WARNING", true)),
    FILE("File", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.FILE", true)),
    INFO("Info", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.INFO", true)),
    INITIALIZER("Initializer", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.INITIALIZER", true)),
    VAULT_MONEY("Vault - Money", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.VAULT_MONEY", true)),
    VAULT_PERMISSION("Vault - Permission", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.VAULT_PERMISSION", true)),
    VAULT_CHAT("Vault - Chat", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.VAULT_CHAT", true)),
    VAULT_ECONOMY("Vault - Economy", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.VAULT_ECONOMY", true)),
    VAULT_PLUGIN("Vault - Plugin", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.VAULT_PLUGIN", true)),
    UPDATER("Updater", CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Plugin.LogActions.IndividualActions.UPDATER", true));

    private final String logPrefix;
    private final boolean enabled;

    LogPrefix(String logPrefix, boolean enabled) {
        this.logPrefix = logPrefix;
        this.enabled = enabled;
    }

    public String getLogPrefix() {
        return logPrefix;
    }

    public boolean isEnabled() {
        return enabled;
    }
}
