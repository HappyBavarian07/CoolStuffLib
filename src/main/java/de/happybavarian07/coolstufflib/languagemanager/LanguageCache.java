// File: `src/main/java/de/happybavarian07/coolstufflib/languagemanager/LanguageCache.java`
package de.happybavarian07.coolstufflib.languagemanager;

import java.util.Map;

@Deprecated(since = "3.0.0")
public class LanguageCache {
    private final String languageName;
    private final Map<String, Object> languageCache = new java.util.concurrent.ConcurrentHashMap<>();

    public LanguageCache(String languageName) {
        this.languageName = languageName;
    }

    public void setup() {
    }

    public void addData(String key, Object value, boolean replace) {
        if (value == null) return;
        if (replace) languageCache.put(key, value);
        else languageCache.putIfAbsent(key, value);
    }

    public Object getData(String key) {
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
}
