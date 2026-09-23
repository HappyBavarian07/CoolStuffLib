package de.happybavarian07.coolstufflib.utils;

import org.bukkit.Bukkit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CardboardCommandGuardTest {
    @AfterEach
    void tearDown() {
        CardboardCommandGuard.cleanup();
    }

    @Test
    void deepNestingIsBlocked() {
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger("test"));
            for (int i = 0; i < 4; i++) assertTrue(CardboardCommandGuard.enterCommand());
            assertFalse(CardboardCommandGuard.enterCommand());
        }
    }

    @Test
    void isolatedScopeStartsFreshAndRestoresState() {
        for (int i = 0; i < 4; i++) CardboardCommandGuard.enterCommand();

        boolean allowedInside = CardboardCommandGuard.isolated(CardboardCommandGuard::enterCommand);

        assertTrue(allowedInside);
        try (MockedStatic<Bukkit> bukkit = mockStatic(Bukkit.class)) {
            bukkit.when(Bukkit::getLogger).thenReturn(Logger.getLogger("test"));
            assertFalse(CardboardCommandGuard.enterCommand(), "outer depth must be restored");
        }
    }
}
