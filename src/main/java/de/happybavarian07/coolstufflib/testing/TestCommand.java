package de.happybavarian07.coolstufflib.testing;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class TestCommand implements CommandExecutor, TabCompleter {
    private final InGameTestRunner testRunner;

    public TestCommand(JavaPlugin plugin) {
        this.testRunner = new InGameTestRunner(plugin);
        registerTestClasses();
    }

    private void registerTestClasses() {
        try {
            // Register in-game test classes
            testRunner.registerTestClass(Class.forName("de.happybavarian07.coolstufflib.testing.tests.InGameUtilsTest"));

            // Try to register original test classes if they exist in the runtime
            registerOriginalTestClass("de.happybavarian07.coolstufflib.integration.SystemIntegrationTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.commandmanagement.CommandManagerTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtilityTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.menusystem.MenuTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.utils.UtilsTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.languagemanager.LanguageManagerTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.backupmanager.FileBackupTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.backupmanager.BackupManagerTest");
            registerOriginalTestClass("de.happybavarian07.coolstufflib.configstuff.core.ConfigManagerTest");
        } catch (Exception e) {
            // Log the error but continue
            e.printStackTrace();
        }
    }

    private void registerOriginalTestClass(String className) {
        try {
            Class<?> testClass = Class.forName(className);
            testRunner.registerTestClass(testClass);
        } catch (ClassNotFoundException ignored) {
            // Class not available in runtime, which is expected
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("coolstufflib.test")) {
            sender.sendMessage("§cYou don't have permission to run tests.");
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage("§6Running all tests...");
            testRunner.runAllTests().thenAccept(results -> {
                testRunner.printResults(sender);
            });
            return true;
        }

        if (args[0].equalsIgnoreCase("list")) {
            sender.sendMessage("§6Available test classes:");
            testRunner.getTestClasses().keySet().forEach(name ->
                sender.sendMessage("§e- " + name));
            return true;
        }

        String testClass = args[0];
        sender.sendMessage("§6Running tests for: " + testClass);
        testRunner.runTest(testClass).thenAccept(results -> {
            if (results.isEmpty()) {
                sender.sendMessage("§cTest class not found: " + testClass);
            } else {
                testRunner.printResults(sender);
            }
        });

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> options = new ArrayList<>(testRunner.getTestClasses().keySet());
            options.add("list");

            return options.stream()
                .filter(option -> option.toLowerCase().startsWith(args[0].toLowerCase()))
                .collect(Collectors.toList());
        }
        return Arrays.asList();
    }
}
