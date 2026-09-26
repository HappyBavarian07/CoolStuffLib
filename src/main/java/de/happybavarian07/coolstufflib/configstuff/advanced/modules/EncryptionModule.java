package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Key;
import java.security.KeyFactory;
import java.security.SecureRandom;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * <p>Stores the values of protected keys encrypted. {@code config.get(key)} returns the encrypted text (prefixed with
 * {@code enc:}); {@link #getDecrypted(String)} returns the plain value.</p>
 *
 * <p>With AES every value gets its own random IV and is encrypted with AES/GCM, so equal values look different in the
 * file and changes to the encrypted text are detected. Values written by older versions (AES/ECB) can still be read.</p>
 */
public class EncryptionModule extends AbstractBaseConfigModule {
    private static final String PREFIX = "enc:";
    private static final String AES_GCM = "AES/GCM/NoPadding";
    private static final int GCM_IV_BYTES = 12;
    private static final int GCM_TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final String algorithm;
    private final Key encryptionKey;
    private final Key decryptionKey;
    private final Set<String> protectedKeys = new CopyOnWriteArraySet<>();

    public EncryptionModule(String algorithm, Key encryptionKey, Key decryptionKey) {
        super("EncryptionModule", "Provides encryption and decryption for sensitive configuration values", "1.1.0");
        this.algorithm = algorithm;
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey != null ? decryptionKey : encryptionKey;
    }

    @Override
    protected void onInitialize() {
        // Saved as a set, but a YAML file loads it back as a list
        if (config.get("__encryptedKeys") instanceof Collection<?> keys) {
            for (Object key : keys) {
                if (key instanceof String name) protectedKeys.add(name);
            }
        }
    }

    @Override
    protected void onEnable() {
        registerEventListener(config.getEventBus(), ConfigValueEvent.class, this::onValueChangeEvent);
    }

    @Override
    protected void onDisable() {
        unregisterEventListeners(config.getEventBus(), ConfigValueEvent.class);
        config.set("__encryptedKeys", new ArrayList<>(protectedKeys));
    }

    @Override
    protected void onCleanup() {
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        String key = event.getFullPath();
        if (event.getType() != ConfigValueEvent.Type.SET || !protectedKeys.contains(key)) return;
        if (event.getNewValue() instanceof String value && !isEncrypted(value)) {
            try {
                event.setNewValue(encrypt(value));
            } catch (Exception e) {
                logError("Failed to encrypt value for key: " + key, e);
            }
        }
    }

    /**
     * <p>Returns the decrypted value of a protected key. {@code config.get(key)} returns the
     * encrypted text; use this method to read it.</p>
     *
     * @return the plain value, the stored value if it is not encrypted, or null if the key is missing
     */
    public String getDecrypted(String key) {
        if (!(config.get(key) instanceof String value)) return null;
        if (!isEncrypted(value)) return value;
        try {
            return decrypt(value);
        } catch (Exception e) {
            logError("Failed to decrypt value for key: " + key, e);
            return null;
        }
    }

    /** Encrypts the current value of {@code key} (if it is a string) and every value set later. */
    public void protectKey(String key) {
        protectedKeys.add(key);
        if (config.containsKey(key) && config.get(key) instanceof String value && !isEncrypted(value)) {
            config.set(key, value);
        }
    }

    /** Stops protecting {@code key} and stores its value as plain text again. */
    public void unprotectKey(String key) {
        protectedKeys.remove(key);
        if (config.containsKey(key) && config.get(key) instanceof String value && isEncrypted(value)) {
            try {
                config.set(key, decrypt(value));
            } catch (Exception e) {
                logError("Failed to decrypt value for key: " + key, e);
            }
        }
    }

    public Set<String> getProtectedKeys() {
        return Collections.unmodifiableSet(protectedKeys);
    }

    public boolean isKeyProtected(String key) {
        return protectedKeys.contains(key);
    }

    private boolean usesAes() {
        return "AES".equalsIgnoreCase(algorithm);
    }

    private String encrypt(String data) throws Exception {
        byte[] plain = data.getBytes(StandardCharsets.UTF_8);
        if (!usesAes()) {
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
            return PREFIX + Base64.getEncoder().encodeToString(cipher.doFinal(plain));
        }
        byte[] iv = new byte[GCM_IV_BYTES];
        RANDOM.nextBytes(iv);
        Cipher cipher = Cipher.getInstance(AES_GCM);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(GCM_TAG_BITS, iv));
        byte[] encrypted = cipher.doFinal(plain);
        return PREFIX + Base64.getEncoder().encodeToString(ByteBuffer.allocate(iv.length + encrypted.length)
                .put(iv).put(encrypted).array());
    }

    private String decrypt(String data) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(data.startsWith(PREFIX) ? data.substring(PREFIX.length()) : data);
        if (usesAes() && decoded.length > GCM_IV_BYTES) {
            try {
                Cipher cipher = Cipher.getInstance(AES_GCM);
                cipher.init(Cipher.DECRYPT_MODE, decryptionKey, new GCMParameterSpec(GCM_TAG_BITS, decoded, 0, GCM_IV_BYTES));
                return new String(cipher.doFinal(decoded, GCM_IV_BYTES, decoded.length - GCM_IV_BYTES), StandardCharsets.UTF_8);
            } catch (javax.crypto.AEADBadTagException legacyValue) {
                // Written by an older version with the JCE default mode (AES/ECB)
            }
        }
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        return new String(cipher.doFinal(decoded), StandardCharsets.UTF_8);
    }

    private boolean isEncrypted(String value) {
        return value.startsWith(PREFIX);
    }

    private void logError(String message, Exception e) {
        ConfigLogger.error(message, e, getName(), true);
    }

    public static class Builder {
        private String algorithm = "AES";
        private Key key;
        private Key decryptKey;

        public Builder withAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        /** For AES the key needs 16, 24 or 32 bytes (UTF-8). */
        public Builder withSymmetricKey(String key) {
            this.key = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), algorithm);
            return this;
        }

        public Builder withAsymmetricKeys(String publicKeyPath, String privateKeyPath) throws Exception {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

            if (publicKeyPath != null) {
                byte[] publicKeyBytes = Files.readAllBytes(Path.of(publicKeyPath));
                this.key = keyFactory.generatePublic(new X509EncodedKeySpec(publicKeyBytes));
            }

            if (privateKeyPath != null) {
                byte[] privateKeyBytes = Files.readAllBytes(Path.of(privateKeyPath));
                this.decryptKey = keyFactory.generatePrivate(new PKCS8EncodedKeySpec(privateKeyBytes));
            }

            return this;
        }

        public EncryptionModule build() {
            if (key == null) {
                throw new IllegalArgumentException("Encryption key must be provided");
            }
            return new EncryptionModule(algorithm, key, decryptKey);
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of("protectedKeys", protectedKeys.size());
    }
}
