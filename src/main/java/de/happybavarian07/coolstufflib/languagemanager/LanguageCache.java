// File: `src/main/java/de/happybavarian07/coolstufflib/languagemanager/LanguageCache.java`
package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.Bukkit;

import java.util.HashMap;
import java.util.Map;

public class LanguageCache {
    private final String languageName;
    private final Map<String, Object> languageCache;
    private long lastAccess;

    public LanguageCache(String languageName) {
        this.languageName = languageName;
        this.languageCache = new HashMap<>();
        this.lastAccess = System.currentTimeMillis();
        setup();
    }

    public void setup() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(CoolStuffLib.getLib().getJavaPluginUsingLib(), () -> {
            if (languageCache.size() > 600 || System.currentTimeMillis() - lastAccess > 600000) {
                clearCache();
            }
        }, 6000, 6000);
    }

    public void addData(String key, Object value, boolean replace) {
        if (replace) {
            languageCache.put(key, value);
        } else {
            languageCache.putIfAbsent(key, value);
        }
        lastAccess = System.currentTimeMillis();
    }

    public Object getData(String key) {
        lastAccess = System.currentTimeMillis();
        return languageCache.get(key);
    }

    public boolean containsKey(String key) {
        return languageCache.containsKey(key);
    }

    public void removeData(String key) {
        languageCache.remove(key);
    }

    public void clearCache() {
        languageCache.clear();
    }

    public String getLanguageName() {
        return languageName;
    }

    public Map<String, Object> getLanguageCache() {
        return languageCache;
    }

    public long getLastAccess() {
        return lastAccess;
    }
}