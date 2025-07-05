package de.happybavarian07.coolstufflib.testing;

import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;

public class TestInitializer {

    private final JavaPlugin plugin;
    private boolean isEnabled = false;

    public TestInitializer(JavaPlugin plugin) {
        this.plugin = plugin;
    }

    public void initialize() {
        if (isEnabled) {
            return;
        }

        // Register test command
        TestCommand testCommand = new TestCommand(plugin);
        PluginCommand command = plugin.getCommand("coolstufftest");

        if (command != null) {
            command.setExecutor(testCommand);
            command.setTabCompleter(testCommand);
            plugin.getLogger().info("In-game test system initialized. Use /coolstufftest to run tests.");
            isEnabled = true;
        } else {
            plugin.getLogger().warning("Failed to register test command - command not found in plugin.yml.");
        }
    }

    public void shutdown() {
        if (!isEnabled) {
            return;
        }

        // Clean up any resources if needed
        isEnabled = false;
    }

    public boolean isEnabled() {
        return isEnabled;
    }
}
