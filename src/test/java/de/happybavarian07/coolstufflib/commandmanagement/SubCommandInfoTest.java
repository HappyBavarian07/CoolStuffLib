package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SubCommandInfoTest {
    @SubCommandInfo(name = "heal", aliases = {"h", "cure"})
    static class Heal extends SubCommand {
        Heal() {
            super("Admin");
        }
    }

    @SubCommandInfo(name = "feed", info = "Feeds you", syntax = "/admin feed [player]", permission = "custom.feed", autoRegisterPermission = false)
    static class Feed extends SubCommand {
        Feed() {
            super("admin");
        }
    }

    static class Unnamed extends SubCommand {
        Unnamed() {
            super("admin");
        }
    }

    @Test
    void annotationProvidesNameAndAliases() {
        Heal heal = new Heal();
        assertEquals("heal", heal.name());
        assertArrayEquals(new String[]{"h", "cure"}, heal.aliases());
        assertTrue(heal.subArgs(null, -1, new String[0]).isEmpty());
    }

    @Test
    void conventionDefaultsWithoutLanguageManager() {
        Heal heal = new Heal();
        assertEquals("admin.heal", heal.permissionAsString());
        assertTrue(heal.autoRegisterPermission());
        assertEquals("/Admin heal", heal.syntax());
        assertEquals("", heal.info());
    }

    @Test
    void infoAndSyntaxComeFromLanguageFileWhenPresent() {
        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getMessageOrDefault(eq("Commands.Admin.heal.Info"), isNull(), anyString(), eq(false))).thenReturn("Heals a player");
        when(lgm.getMessageOrDefault(eq("Commands.Admin.heal.Syntax"), isNull(), anyString(), eq(false)))
                .thenAnswer(invocation -> invocation.getArgument(2));
        Heal heal = new Heal();
        heal.setDependencies(null, lgm, null);

        assertEquals("Heals a player", heal.info());
        assertEquals("/Admin heal", heal.syntax());
    }

    @Test
    void explicitAnnotationValuesWin() {
        LanguageManager lgm = mock(LanguageManager.class);
        Feed feed = new Feed();
        feed.setDependencies(null, lgm, null);

        assertEquals("Feeds you", feed.info());
        assertEquals("/admin feed [player]", feed.syntax());
        assertEquals("custom.feed", feed.permissionAsString());
        assertFalse(feed.autoRegisterPermission());
        verifyNoInteractions(lgm);
    }

    @Test
    void missingNameExplainsWhatToDo() {
        IllegalStateException ex = assertThrows(IllegalStateException.class, () -> new Unnamed().name());
        assertTrue(ex.getMessage().contains("@SubCommandInfo"));
    }
}
