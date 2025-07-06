package de.happybavarian07.coolstufflib.integration;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.commandmanagement.CommandManager;
import de.happybavarian07.coolstufflib.commandmanagement.SubCommand;
import de.happybavarian07.coolstufflib.languagemanager.LanguageFile;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.PlaceholderType;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SystemIntegrationTest {

    static final Path tempDir = Path.of("TestOutputs/SystemIntegrationTest");

    @Mock
    private JavaPlugin mockPlugin;

    @Mock
    private Player mockPlayer;

    @Mock
    private CoolStuffLib mockCoolStuffLib;

    private LanguageManager languageManager;
    private TestIntegratedCommandManager commandManager;
    private TestIntegratedMenu menu;

    //@BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        File langFolder = tempDir.resolve("languages").toFile();
        langFolder.mkdirs();
        File testLangFile = langFolder.toPath().resolve("en_test.yml").toFile();
        if (!testLangFile.exists()) {
            try (FileWriter writer = new FileWriter(testLangFile)) {
                writer.write("LanguageFullName: English (Test)\n");
                writer.write("LanguageVersion: 1.0\n");
                writer.write("Messages:\n");
                writer.write("  Player.General.ReloadedLanguageFile: \"Language file reloaded.\"\n");
                writer.write("  Player.General.NoPermissions: \"You do not have permission: %permission%\"\n");
                writer.write("MenuTitles:\n");
                writer.write("  integrationFeature: \"Integration Test Menu\"\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        languageManager = new LanguageManager(mockPlugin, langFolder, "resources", "[INTEGRATION]");
        LanguageFile testLang = new LanguageFile(langFolder, "resources", "en_test");
        languageManager.addLang(testLang, "en_test");
        languageManager.setCurrentLang(testLang, false);

        when(mockPlayer.getName()).thenReturn("IntegrationTestPlayer");
        when(mockPlayer.hasPermission(anyString())).thenReturn(true);

        try (MockedStatic<CoolStuffLib> mockedStatic = mockStatic(CoolStuffLib.class)) {
            mockedStatic.when(CoolStuffLib::getLib).thenReturn(mockCoolStuffLib);
            when(mockCoolStuffLib.getLanguageManager()).thenReturn(languageManager);

            commandManager = new TestIntegratedCommandManager();
            PlayerMenuUtility utility = new PlayerMenuUtility(mockPlayer.getUniqueId());
            menu = new TestIntegratedMenu(utility);
        }
    }

    //@Test
    void testLanguageManagerCommandManagerIntegration() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%player%", "TestPlayer", false);

        TestSubCommand subCommand = new TestSubCommand();

        assertNotNull(commandManager.getSubCommand("open"));
        assertEquals("open", subCommand.name());
        assertTrue(subCommand.permissionAsString().contains("integration"));
    }

    //@Test
    void testLanguageManagerMenuIntegration() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%menu_title%", "Integration Test Menu", false);

        assertEquals("Integration Test Menu", menu.getMenuName());
        assertEquals("integrationFeature", menu.getConfigMenuAddonFeatureName());
        assertEquals(54, menu.getSlots());
    }

    //@Test
    void testFullSystemWorkflow() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%player%", mockPlayer.getName(), false);
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%command%", "integration", false);

        TestSubCommand subCommand = new TestSubCommand();

        String[] args = {"open"};
        boolean commandResult = commandManager.onCommand(mockPlayer, args);

        assertTrue(commandResult);
        assertTrue(subCommand.wasExecuted);
        assertEquals(mockPlayer, subCommand.lastSender);
    }

    static class TestIntegratedCommandManager extends CommandManager {
        @Override
        public String getCommandName() {
            return "integration";
        }

        @Override
        public String getCommandUsage() {
            return "/integration <subcommand>";
        }

        @Override
        public String getCommandInfo() {
            return "Integration test command";
        }

        @Override
        public JavaPlugin getJavaPlugin() {
            return mock(JavaPlugin.class);
        }

        @Override
        public List<String> getCommandAliases() {
            return Arrays.asList("int", "test");
        }

        @Override
        public String getCommandPermissionAsString() {
            return "integration.use";
        }

        @Override
        public boolean autoRegisterPermission() {
            return true;
        }

        @Override
        public void setup() {
            registerSubCommand(new TestSubCommand());
        }
    }

    static class TestSubCommand extends SubCommand {
        boolean wasExecuted = false;
        CommandSender lastSender = null;

        public TestSubCommand() {
            super("integration");
        }

        @Override
        public String name() {
            return "open";
        }

        @Override
        public String info() {
            return "Opens integration test menu";
        }

        @Override
        public String[] aliases() {
            return new String[0];
        }

        @Override
        public Map<Integer, String[]> subArgs(CommandSender sender, int isPlayer, String[] args) {
            return Map.of();
        }

        @Override
        public String syntax() {
            return "";
        }

        @Override
        public String permissionAsString() {
            return "";
        }

        @Override
        public boolean isPlayerRequired() {
            return true;
        }

        @Override
        public boolean autoRegisterPermission() {
            return true;
        }
    }

    static class TestIntegratedMenu extends Menu {
        public TestIntegratedMenu(PlayerMenuUtility playerMenuUtility) {
            super(playerMenuUtility);
        }

        @Override
        public String getMenuName() {
            return "Integration Test Menu";
        }

        @Override
        public String getConfigMenuAddonFeatureName() {
            return "integrationFeature";
        }

        @Override
        public int getSlots() {
            return 54;
        }

        @Override
        public void handleMenu(InventoryClickEvent e) {
        }

        @Override
        public void handleOpenMenu(InventoryOpenEvent e) {
        }

        @Override
        public void handleCloseMenu(InventoryCloseEvent e) {
        }

        @Override
        public void setMenuItems() {
        }
    }
}
