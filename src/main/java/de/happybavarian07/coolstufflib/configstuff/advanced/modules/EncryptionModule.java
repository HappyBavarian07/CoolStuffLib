package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Map;

public class EncryptionModule extends ConfigModule {
    private final String algorithm;
    private final Key key;
    private final Key decryptKey;

    public EncryptionModule(String algorithm, Key key, Key decryptKey) {
        this.algorithm = algorithm;
        this.key = key;
        this.decryptKey = decryptKey != null ? decryptKey : key;
    }

    @Override
    public String getName() {
        return "EncryptionModule";
    }

    @Override
    public void enable() { /* Do nothing */ }

    @Override
    public void disable() { /* Do nothing */ }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
    }

    @Override
    public void onConfigChange(String keyName, Object oldValue, Object newValue) {
        if (getConfig() != null && newValue != null && isEnabled() && newValue instanceof String) {
            try {
                byte[] input = newValue.toString().getBytes();
                Cipher cipher = Cipher.getInstance(algorithm);
                cipher.init(Cipher.ENCRYPT_MODE, key);
                byte[] encrypted = cipher.doFinal(input);
                getConfig().setValue(keyName, Base64.getEncoder().encodeToString(encrypted));
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return decrypt(value);
    }

    public boolean isBase64Encoded(String value) {
        if (value == null || value.isEmpty()) return false;
        try {
            Base64.getDecoder().decode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public Object decrypt(Object value) {
        if (getConfig() == null || !isEnabled() || !(value instanceof String) || isBase64Encoded((String) value))
            return value;
        try {
            byte[] encrypted = Base64.getDecoder().decode((String) value);
            Cipher cipher = Cipher.getInstance(algorithm);
            cipher.init(Cipher.DECRYPT_MODE, decryptKey);
            byte[] decrypted = cipher.doFinal(encrypted);
            return new String(decrypted);
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return config != null && config.containsKey("encryption");
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("algorithm", algorithm, "keyLength", key.getEncoded().length, "enabled", isEnabled());
    }

    public static void saveKeyToFile(Key key, Path path) throws Exception {
        Files.write(path, key.getEncoded());
    }

    public static Key loadSecretKeyFromFile(Path path, String algorithm) throws Exception {
        byte[] encoded = Files.readAllBytes(path);
        return new SecretKeySpec(encoded, algorithm);
    }

    public static KeyPair loadKeyPairFromFiles(Path pubPath, Path privPath, String algorithm) throws Exception {
        byte[] pubBytes = Files.readAllBytes(pubPath);
        byte[] privBytes = Files.readAllBytes(privPath);
        KeyFactory kf = KeyFactory.getInstance(algorithm);
        PublicKey pub = kf.generatePublic(new X509EncodedKeySpec(pubBytes));
        PrivateKey priv = kf.generatePrivate(new PKCS8EncodedKeySpec(privBytes));
        return new KeyPair(pub, priv);
    }

    public static void saveKeyPairToFiles(KeyPair pair, Path pubPath, Path privPath) throws Exception {
        Files.write(pubPath, pair.getPublic().getEncoded());
        Files.write(privPath, pair.getPrivate().getEncoded());
    }
}
