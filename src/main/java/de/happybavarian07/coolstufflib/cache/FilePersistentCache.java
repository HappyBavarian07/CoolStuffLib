package de.happybavarian07.coolstufflib.cache;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import org.checkerframework.checker.units.qual.K;

import java.io.*;
import java.lang.reflect.Type;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

/**
 * <p>Implements a thread-safe, persistent cache with automatic JSON migration.</p>
 */

public class FilePersistentCache<K, V> implements PersistentCache<K, V> {
    private final ConcurrentMap<K, V> memoryCache = new ConcurrentHashMap<>();
    private final String cacheFile;
    private final int maxSize;
    private final Object fileLock = new Object();
    private final ScheduledExecutorService scheduler;
    private final Gson gson;
    private final Class<V> valueClass;
    private final Type loadType;
    private volatile boolean closed = false;

    public FilePersistentCache(String filename) {
        this(filename, Integer.MAX_VALUE, true, 30, (Class<V>) Object.class);
    }

    public FilePersistentCache(File file) {
        this(file.getAbsolutePath(), Integer.MAX_VALUE, true, 30, (Class<V>) Object.class);
    }

    public FilePersistentCache(String filename, int maxSize, boolean autoSave, int autoSaveIntervalSeconds, Class<V> valueClass) {
        this.cacheFile = filename;
        this.maxSize = maxSize;
        this.valueClass = valueClass;
        this.loadType = TypeToken.getParameterized(ConcurrentMap.class, String.class, valueClass).getType();
        this.gson = createGson();//valueClass == Object.class);
        this.scheduler = createScheduler(autoSave, autoSaveIntervalSeconds);
        load();
    }

    private Gson createGson(/*boolean objectValues*/) {
        GsonBuilder builder = new GsonBuilder().setPrettyPrinting();
        //if (objectValues) {
        //    builder.registerTypeAdapter(Object.class, new ObjectTypeAdapter());
        //}
        return builder.create();
    }

    private static final class ObjectTypeAdapter implements JsonDeserializer<Object> {
        @Override
        public Object deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) {
            return read(json);
        }

        private Object read(JsonElement json) {
            if (json == null || json.isJsonNull()) return null;

            if (json.isJsonPrimitive()) {
                JsonPrimitive p = json.getAsJsonPrimitive();
                if (p.isBoolean()) return p.getAsBoolean();
                if (p.isString()) return p.getAsString();

                String s = p.getAsString();
                if (s.matches("[-+]?\\d+")) {
                    try { return Integer.parseInt(s); } catch (NumberFormatException ignored) {}
                    try { return Long.parseLong(s); } catch (NumberFormatException ignored) {}
                    return new BigInteger(s);
                }
                return p.getAsDouble();
            }

            if (json.isJsonArray()) {
                List<Object> list = new ArrayList<>();
                for (JsonElement element : json.getAsJsonArray()) {
                    list.add(read(element));
                }
                return list;
            }

            if (json.isJsonObject()) {
                Map<String, Object> map = new LinkedHashMap<>();
                for (Map.Entry<String, JsonElement> entry : json.getAsJsonObject().entrySet()) {
                    map.put(entry.getKey(), read(entry.getValue()));
                }
                return map;
            }

            return null;
        }
    }

    private ScheduledExecutorService createScheduler(boolean autoSave, int interval) {
        if (!autoSave || interval <= 0) return null;
        ScheduledExecutorService s = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "PersistentCache-AutoSave");
            t.setDaemon(true);
            return t;
        });
        s.scheduleAtFixedRate(this::save, interval, interval, TimeUnit.SECONDS);
        return s;
    }

    @Override
    public V get(K key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        return memoryCache.get(key);
    }

    @Override
    public void put(K key, V value, boolean overwrite) {
        validateInput(key, value);
        synchronized (fileLock) {
            if (memoryCache.size() >= maxSize && !memoryCache.containsKey(key)) return;
            if (overwrite) memoryCache.put(key, value);
            else memoryCache.putIfAbsent(key, value);
        }
    }

    @Override
    public void put(K key, V value) {
        validateInput(key, value);
        synchronized (fileLock) {
            if (memoryCache.size() >= maxSize && !memoryCache.containsKey(key)) return;
            memoryCache.put(key, value);
        }
    }

    @Override
    public void remove(K key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        memoryCache.remove(key);
    }

    @Override
    public void clear() {
        memoryCache.clear();
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) throw new IllegalArgumentException("Key must not be null");
        return memoryCache.containsKey(key);
    }

    @Override
    public void save() {
        if (closed) return;
        writeToDisk();
    }

    private void writeToDisk() {
        synchronized (fileLock) {
            Path path = Paths.get(cacheFile);
            ensureParentExists(path);
            Path tempFile = Paths.get(cacheFile + ".tmp");

            try (Writer writer = Files.newBufferedWriter(tempFile)) {
                gson.toJson(memoryCache, writer);
                Files.move(tempFile, path, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                System.err.println("Failed to save cache: " + e.getMessage());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        synchronized (fileLock) {
            Path jsonPath = Paths.get(cacheFile);
            Path binaryPath = Paths.get(cacheFile + ".bin");

            if (Files.exists(jsonPath)) {
                loadJson(jsonPath);
                return;
            }

            if (Files.exists(binaryPath)) {
                migrateBinaryToJson(binaryPath, jsonPath);
            }
        }
    }

    private void loadJson(Path path) {
        try (Reader reader = Files.newBufferedReader(path)) {
            ConcurrentMap<K, V> loaded = gson.fromJson(reader, loadType);
            if (loaded != null) {
                memoryCache.clear();
                memoryCache.putAll(loaded);
            }
        } catch (IOException e) {
            System.err.println("Failed to load JSON cache: " + e.getMessage());
        }
    }

    private void migrateBinaryToJson(Path binaryPath, Path jsonPath) {
        try (ObjectInputStream ois = new ObjectInputStream(new BufferedInputStream(Files.newInputStream(binaryPath)))) {
            ConcurrentMap<K, V> loaded = (ConcurrentMap<K, V>) ois.readObject();
            memoryCache.clear();
            memoryCache.putAll(loaded);
            save();
            Files.delete(binaryPath);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Failed to migrate binary cache: " + e.getMessage());
        }
    }

    private void ensureParentExists(Path path) {
        try {
            if (path.getParent() != null) Files.createDirectories(path.getParent());
        } catch (IOException e) {
            System.err.println("Failed to create directories: " + e.getMessage());
        }
    }

    private void validateInput(K key, V value) {
        if (key == null || value == null) throw new IllegalArgumentException("Key/Value must not be null");
    }

    public int size() {
        return memoryCache.size();
    }

    public String getCacheFile() {
        return cacheFile;
    }

    public void close() {
        if (closed) return;
        closed = true;

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
        writeToDisk();
    }
}
