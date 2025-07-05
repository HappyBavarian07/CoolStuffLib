package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.AutoGenModule;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates.AutoGenTemplate;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;
import java.util.concurrent.CopyOnWriteArraySet;

public class EncryptionModule extends AbstractBaseConfigModule {
    private final String algorithm;
    private final Key encryptionKey;
    private final Key decryptionKey;
    private final Set<String> protectedKeys = new CopyOnWriteArraySet<>();
    private final Set<UUID> protectedTemplates = new HashSet<>();
    private final Map<String, String> fileEncryptionMap = new HashMap<>();

    public EncryptionModule(String algorithm, Key encryptionKey, Key decryptionKey) {
        super("EncryptionModule", "Provides encryption and decryption for sensitive configuration values", "1.0.0");
        this.algorithm = algorithm;
        this.encryptionKey = encryptionKey;
        this.decryptionKey = decryptionKey != null ? decryptionKey : encryptionKey;
    }

    @Override
    protected void onInitialize() {
        Object keysObj = config.get("__encryptedKeys");
        if (keysObj instanceof Set<?> set) {
            for (Object item : set) {
                if (item instanceof String key) {
                    protectedKeys.add(key);
                }
            }
        }

        Object templatesObj = config.get("__encryptedTemplates");
        if (templatesObj instanceof Set<?> set) {
            for (Object id : set) {
                if (id instanceof String) {
                    try {
                        protectedTemplates.add(UUID.fromString((String) id));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        }

        Object filesObj = config.get("__encryptedFiles");
        if (filesObj instanceof Map<?, ?> map) {
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                if (entry.getKey() instanceof String key && entry.getValue() instanceof String value) {
                    fileEncryptionMap.put(key, value);
                }
            }
        }
    }

    @Override
    protected void onEnable() {
        registerEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onDisable() {
        saveState();
    }

    @Override
    protected void onCleanup() {

    }

    private void saveState() {
        config.set("__encryptedKeys", new HashSet<>(protectedKeys));

        Set<String> templateIds = new HashSet<>();
        for (UUID id : protectedTemplates) {
            templateIds.add(id.toString());
        }
        config.set("__encryptedTemplates", templateIds);

        config.set("__encryptedFiles", new HashMap<>(fileEncryptionMap));
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        String key = event.getFullPath();

        if (event.getType() == ConfigValueEvent.Type.SET && protectedKeys.contains(key)) {
            if (event.getNewValue() instanceof String) {
                try {
                    String encrypted = encrypt((String) event.getNewValue());
                    event.setNewValue(encrypted);
                } catch (Exception e) {
                    logError("Failed to encrypt value for key: " + key, e);
                }
            }
        } else if (event.getType() == ConfigValueEvent.Type.GET && protectedKeys.contains(key)) {
            if (event.getOldValue() instanceof String value) {
                try {
                    if (isEncrypted(value)) {
                        String decrypted = decrypt(value);
                        // Store decrypted value back to config directly since event doesn't have setValue
                        config.set(key, decrypted);
                    }
                } catch (Exception e) {
                    logError("Failed to decrypt value for key: " + key, e);
                }
            }
        }
    }

    public void protectKey(String key) {
        protectedKeys.add(key);

        if (config.containsKey(key) && config.get(key) instanceof String value) {
            try {
                if (!isEncrypted(value)) {
                    String encrypted = encrypt(value);
                    config.set(key, encrypted);
                }
            } catch (Exception e) {
                logError("Failed to encrypt existing value for key: " + key, e);
            }
        }
    }

    public void protectTemplate(UUID templateId) {
        protectedTemplates.add(templateId);

        AutoGenModule autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
        if (autoGenModule != null) {
            AutoGenTemplate template = autoGenModule.getTemplateById(templateId);
            if (template != null) {
                encryptTemplate(template);
            }
        }
    }

    public void protectTemplate(String templateName) {
        AutoGenModule autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
        if (autoGenModule != null) {
            UUID templateId = autoGenModule.getTemplateNameToIdMap().get(templateName);
            if (templateId != null) {
                protectTemplate(templateId);
            }
        }
    }

    public void protectFile(String filePath) {
        File file = new File(filePath);
        if (file.exists() && file.isFile()) {
            try {
                String content = Files.readString(file.toPath());
                String encrypted = encrypt(content);
                Files.writeString(file.toPath(), encrypted);
                fileEncryptionMap.put(filePath, "encrypted");
            } catch (Exception e) {
                logError("Failed to encrypt file: " + filePath, e);
            }
        }
    }

    public void unprotectKey(String key) {
        protectedKeys.remove(key);

        if (config.containsKey(key) && config.get(key) instanceof String value) {
            try {
                if (isEncrypted(value)) {
                    String decrypted = decrypt(value);
                    config.set(key, decrypted);
                }
            } catch (Exception e) {
                logError("Failed to decrypt value for key: " + key, e);
            }
        }
    }

    public void unprotectTemplate(UUID templateId) {
        protectedTemplates.remove(templateId);

        AutoGenModule autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
        if (autoGenModule != null) {
            AutoGenTemplate template = autoGenModule.getTemplateById(templateId);
            if (template != null) {
                decryptTemplate(template);
            }
        }
    }

    public void unprotectTemplate(String templateName) {
        AutoGenModule autoGenModule = (AutoGenModule) config.getModuleByName("AutoGenModule");
        if (autoGenModule != null) {
            UUID templateId = autoGenModule.getTemplateNameToIdMap().get(templateName);
            if (templateId != null) {
                unprotectTemplate(templateId);
            }
        }
    }

    public void unprotectFile(String filePath) {
        if (fileEncryptionMap.containsKey(filePath)) {
            File file = new File(filePath);
            if (file.exists() && file.isFile()) {
                try {
                    String content = Files.readString(file.toPath());
                    String decrypted = decrypt(content);
                    Files.writeString(file.toPath(), decrypted);
                    fileEncryptionMap.remove(filePath);
                } catch (Exception e) {
                    logError("Failed to decrypt file: " + filePath, e);
                }
            }
        }
    }

    private void encryptTemplate(AutoGenTemplate template) {
        try {
            File tempFile = File.createTempFile("template", ".json");
            template.writeToFile(tempFile);
            String content = Files.readString(tempFile.toPath());
            String encrypted = encrypt(content);

            File encryptedFile = new File(tempFile.getParentFile(), tempFile.getName() + ".encrypted");
            Files.writeString(encryptedFile.toPath(), encrypted);

            if (!tempFile.delete()) {
                logError("Failed to delete temporary file: " + tempFile.getPath(), null);
            }
        } catch (Exception e) {
            logError("Failed to encrypt template", e);
        }
    }

    private void decryptTemplate(AutoGenTemplate template) {
        try {
            File tempFile = File.createTempFile("template", ".json.encrypted");
            template.writeToFile(tempFile);
            String content = Files.readString(tempFile.toPath());
            String decrypted = decrypt(content);

            File decryptedFile = new File(tempFile.getParentFile(), tempFile.getName().replace(".encrypted", ""));
            Files.writeString(decryptedFile.toPath(), decrypted);

            if (!tempFile.delete()) {
                logError("Failed to delete temporary file: " + tempFile.getPath(), null);
            }
        } catch (Exception e) {
            logError("Failed to decrypt template", e);
        }
    }

    public Set<String> getProtectedKeys() {
        return Collections.unmodifiableSet(protectedKeys);
    }

    public Set<UUID> getProtectedTemplates() {
        return Collections.unmodifiableSet(protectedTemplates);
    }

    public Map<String, String> getProtectedFiles() {
        return Collections.unmodifiableMap(fileEncryptionMap);
    }

    public boolean isKeyProtected(String key) {
        return protectedKeys.contains(key);
    }

    public boolean isTemplateProtected(UUID templateId) {
        return protectedTemplates.contains(templateId);
    }

    public boolean isFileProtected(String filePath) {
        return fileEncryptionMap.containsKey(filePath);
    }

    private String encrypt(String data) throws Exception {
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey);
        byte[] encrypted = cipher.doFinal(data.getBytes());
        return Base64.getEncoder().encodeToString(encrypted);
    }

    private String decrypt(String data) throws Exception {
        byte[] decoded = Base64.getDecoder().decode(data);
        Cipher cipher = Cipher.getInstance(algorithm);
        cipher.init(Cipher.DECRYPT_MODE, decryptionKey);
        byte[] decrypted = cipher.doFinal(decoded);
        return new String(decrypted);
    }

    private boolean isEncrypted(String value) {
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    private void logError(String message, Exception e) {
        System.err.println("[EncryptionModule] " + message);
        if (e != null) {
            System.err.println("[EncryptionModule] Cause: " + e.getMessage());
        }
    }

    public static class Builder {
        private String algorithm = "RSA";
        private Key key;
        private Key decryptKey;

        public Builder withAlgorithm(String algorithm) {
            this.algorithm = algorithm;
            return this;
        }

        public Builder withSymmetricKey(String key) {
            this.key = new SecretKeySpec(key.getBytes(), algorithm);
            return this;
        }

        public Builder withAsymmetricKeys(String publicKeyPath, String privateKeyPath) throws Exception {
            KeyFactory keyFactory = KeyFactory.getInstance(algorithm);

            if (publicKeyPath != null) {
                byte[] publicKeyBytes = Files.readAllBytes(Path.of(publicKeyPath));
                X509EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
                this.key = keyFactory.generatePublic(publicKeySpec);
            }

            if (privateKeyPath != null) {
                byte[] privateKeyBytes = Files.readAllBytes(Path.of(privateKeyPath));
                PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(privateKeyBytes);
                this.decryptKey = keyFactory.generatePrivate(privateKeySpec);
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
        return Map.of();
    }
}
