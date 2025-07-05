package de.happybavarian07.coolstufflib.testing;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class LibraryTestInitializer {

    private final AutoTestRunner testRunner;
    private final JavaPlugin plugin;

    public LibraryTestInitializer(JavaPlugin plugin) {
        this.plugin = plugin;
        this.testRunner = new AutoTestRunner(plugin);

        registerTestClasses();
    }

    private void registerTestClasses() {
        // Register in-game test classes
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.testing.tests.InGameUtilsTest");

        // Try to register other test classes
        registerStandardTestClasses();
    }

    private void registerStandardTestClasses() {
        // Register your existing test classes
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.integration.SystemIntegrationTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.commandmanagement.CommandManagerTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtilityTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.menusystem.MenuTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.utils.UtilsTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.languagemanager.LanguageManagerTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.backupmanager.FileBackupTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.backupmanager.BackupManagerTest");
        testRunner.registerTestClass("de.happybavarian07.coolstufflib.configstuff.core.ConfigManagerTest");
    }

    public void executeTests() {
        // Schedule tests to run shortly after the server starts
        // Using 40 ticks (2 seconds) to ensure server has initialized
        plugin.getLogger().log(Level.INFO, "Scheduling automatic tests for CoolStuffLib");
        testRunner.scheduleAutomaticExecution(40L);
    }

    public AutoTestRunner getTestRunner() {
        return testRunner;
    }
}
