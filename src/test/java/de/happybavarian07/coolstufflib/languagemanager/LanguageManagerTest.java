package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.ExpressionEngine;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.File;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LanguageManagerTest {

    static final Path tempDir = Path.of("TestOutputs/LanguageManagerTest");

    @Mock
    private JavaPlugin mockPlugin;

    @Mock
    private Player mockPlayer;

    private LanguageManager languageManager;
    private File langFolder;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        langFolder = tempDir.resolve("languages").toFile();
        langFolder.mkdirs();

        languageManager = new LanguageManager(mockPlugin, langFolder, "resources", "[TEST]");
    }

    @Test
    void testLanguageManagerInitialization() {
        assertNotNull(languageManager);
        assertEquals("[TEST]", languageManager.getPrefix());
        assertNotNull(languageManager.getRegisteredLanguages());
        assertTrue(languageManager.getRegisteredLanguages().isEmpty());
    }

    @Test
    void testPlaceholderRegistration() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%test%", "testValue", false);
        assertTrue(languageManager.getPlaceholders().containsKey("%test%"));
        assertEquals("testValue", languageManager.getPlaceholders().get("%test%").getValue());
    }

    @Test
    void testPlaceholderByTypeAndKey() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%player%", "TestPlayer", false);
        Placeholder retrieved = languageManager.getPlaceholders().get("%player%");
        assertNotNull(retrieved);
        assertEquals("TestPlayer", retrieved.getValue());
        assertEquals(PlaceholderType.MESSAGE, retrieved.getType());
    }

    @Test
    void testPlaceholderRemoval() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%temp%", "temporary", false);
        assertTrue(languageManager.getPlaceholders().containsKey("%temp%"));
        languageManager.removePlaceholder(PlaceholderType.MESSAGE, "%temp%");
        assertFalse(languageManager.getPlaceholders().containsKey("%temp%"));
    }

    @Test
    void testClearPlaceholders() {
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%test1%", "value1", false);
        languageManager.addPlaceholder(PlaceholderType.MESSAGE, "%test2%", "value2", false);
        assertFalse(languageManager.getPlaceholders().isEmpty());
        languageManager.getPlaceholders().clear();
        assertTrue(languageManager.getPlaceholders().isEmpty());
    }

    @Test
    void testPrefixGetterAndSetter() {
        assertEquals("[TEST]", languageManager.getPrefix());
        languageManager.setPrefix("[NEW]");
        assertEquals("[NEW]", languageManager.getPrefix());
    }

    @Test
    void testExpressionEnginePoolAccess() {
        assertNotNull(languageManager.getExpressionEnginePool());
        ExpressionEngine defaultEngine = languageManager.getExpressionEnginePool().getEngineForLanguage("default");
        assertNotNull(defaultEngine);
    }
}
