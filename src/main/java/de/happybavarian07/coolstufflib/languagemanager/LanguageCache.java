// File: `src/main/java/de/happybavarian07/coolstufflib/languagemanager/LanguageCache.java`
package de.happybavarian07.coolstufflib.languagemanager;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class LanguageCache {
    private final String languageName;
    private final Map<String, Object> languageCache;
    private long lastAccess;
    private static final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();

    public LanguageCache(String languageName) {
        this.languageName = languageName;
        this.languageCache = new HashMap<>();
        this.lastAccess = System.currentTimeMillis();
        setup();
    }

    public void setup() {
        executor.scheduleAtFixedRate(() -> {
            if (languageCache.size() > 600 || System.currentTimeMillis() - lastAccess > 600000) {
                clearCache();
            }
        }, 6000, 6000, TimeUnit.MILLISECONDS);
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