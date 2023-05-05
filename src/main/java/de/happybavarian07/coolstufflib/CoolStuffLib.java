package de.happybavarian07.coolstufflib;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:14
 */

import de.happybavarian07.coolstufflib.commandmanagement.CommandManagerRegistry;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.menusystem.MenuAddonManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class CoolStuffLib {
    private static CoolStuffLib lib;
    private LanguageManager languageManager;
    private boolean languageManagerEnabled = false;
    private CommandManagerRegistry commandManagerRegistry;
    private boolean commandManagerRegistryEnabled = false;
    private MenuAddonManager menuAddonManager;
    private boolean menuAddonManagerEnabled = false;
    private final JavaPlugin javaPluginUsingLib;
    private boolean placeholderAPIEnabled = false;

    /**
     * You can find a full Tutorial on this Lib under this Link: <>.
     * Here is a Video from me explaining it a bit more and showing one Example.
     * @param enablingClass Class you have to override to edit the starting methods.
     * @param javaPluginUsingLib You have to set your Java Plugin Here to make it work.
     * @param languageManager The Language Manager Class
     * @param commandManagerRegistry The Command Manager Registry Class
     * @param menuAddonManager The Menu Addon Manager Class
     */
    public CoolStuffLib(EnablingClass enablingClass, JavaPlugin javaPluginUsingLib, LanguageManager languageManager, CommandManagerRegistry commandManagerRegistry, MenuAddonManager menuAddonManager) {
        lib = this;
        this.javaPluginUsingLib = javaPluginUsingLib;
        if (this.javaPluginUsingLib == null) {
            throw new RuntimeException("CoolStuffLib did not find a Plugin it got called from. Returning. Report the Issue to the Plugin Dev(s), that may have programmed the Plugin.");
        }
        if (!enablingClass.getClass().isAssignableFrom(EnablingClass.class)) {
            throw new RuntimeException("CoolStuffLib did not find an Enabling Class being passed in the Constructor. Report the Issue to the Plugin Dev(s) from " + javaPluginUsingLib.getName());
        }
        if (languageManager != null) {
            if(Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
                placeholderAPIEnabled = true;
            }
            // Language Manager Enable Code
            enablingClass.startLanguageManager(languageManager, javaPluginUsingLib,);
            languageManagerEnabled = true;
        }
        if (commandManagerRegistry != null) {
            // Command Registry Enable Code
            commandManagerRegistryEnabled = true;
        }
        if(menuAddonManager != null) {
            // Menu Addon Manager Init Code
            menuAddonManagerEnabled = true;
        }
    }

    public static CoolStuffLib getLib() {
        return lib;
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public CommandManagerRegistry getCommandManagerRegistry() {
        return commandManagerRegistry;
    }

    public MenuAddonManager getMenuAddonManager() {
        return menuAddonManager;
    }

    public JavaPlugin getJavaPluginUsingLib() {
        return javaPluginUsingLib;
    }

    public boolean isLanguageManagerEnabled() {
        return languageManagerEnabled;
    }

    public boolean isCommandManagerRegistryEnabled() {
        return commandManagerRegistryEnabled;
    }

    public boolean isMenuAddonManagerEnabled() {
        return menuAddonManagerEnabled;
    }

    public boolean isPlaceholderAPIEnabled() {
        return placeholderAPIEnabled;
    }
}
