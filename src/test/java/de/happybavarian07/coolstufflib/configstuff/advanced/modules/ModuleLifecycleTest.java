package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.AdvancedInMemoryConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ModuleLifecycleTest {

    @Test
    void disabledModuleStopsListening() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("history");
        HistoryModule history = new HistoryModule(10);
        history.initialize(config);
        history.enable();
        config.set("key", "a");
        config.set("key", "b");
        int recorded = history.getHistorySize("key");
        assertTrue(recorded > 0);

        history.disable();
        config.set("key", "c");

        assertEquals(recorded, history.getHistorySize("key"));
    }

    @Test
    void encryptionModuleStoresEncryptedValuesAndDecryptsThem() {
        AdvancedInMemoryConfig config = new AdvancedInMemoryConfig("secrets");
        config.set("db.password", "old");
        EncryptionModule encryption = new EncryptionModule.Builder()
                .withAlgorithm("AES")
                .withSymmetricKey("0123456789abcdef")
                .build();
        encryption.initialize(config);
        encryption.enable();

        encryption.protectKey("db.password");
        assertTrue(config.getString("db.password").startsWith("enc:"));
        assertEquals("old", encryption.getDecrypted("db.password"));

        config.set("db.password", "secret");
        assertTrue(config.getString("db.password").startsWith("enc:"));
        assertEquals("secret", encryption.getDecrypted("db.password"));

        config.set("db.user", "admin");
        assertEquals("admin", config.getString("db.user"));
    }
}
