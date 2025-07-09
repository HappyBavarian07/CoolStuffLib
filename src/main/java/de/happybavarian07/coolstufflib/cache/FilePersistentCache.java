package de.happybavarian07.coolstufflib.cache;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.*;

public class FilePersistentCache<K, V> implements PersistentCache<K, V> {
    private final ConcurrentMap<K, V> memoryCache = new ConcurrentHashMap<>();
    private final String cacheFile;
    private final int maxSize;
    private final Object fileLock = new Object();
    private final ScheduledExecutorService scheduler;
    private volatile boolean closed = false;

    public FilePersistentCache(String filename) {
        this(filename, Integer.MAX_VALUE, true, 30);
    }

    public FilePersistentCache(File file) {
        this(file.getAbsolutePath(), Integer.MAX_VALUE, true, 30);
    }

    public FilePersistentCache(String filename, int maxSize, boolean autoSave, int autoSaveIntervalSeconds) {
        this.cacheFile = filename;
        this.maxSize = maxSize;

        if (autoSave && autoSaveIntervalSeconds > 0) {
            this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "PersistentCache-AutoSave");
                t.setDaemon(true);
                return t;
            });
            this.scheduler.scheduleAtFixedRate(this::save, autoSaveIntervalSeconds,
                    autoSaveIntervalSeconds, TimeUnit.SECONDS);
        } else {
            this.scheduler = null;
        }

        load();
    }

    @Override
    public V get(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        return memoryCache.get(key);
    }

    @Override
    public void put(K key, V value, boolean overwrite) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (fileLock) {
            if (memoryCache.size() >= maxSize && !memoryCache.containsKey(key)) {
                return;
            }
            if (overwrite) {
                memoryCache.put(key, value);
            } else {
                memoryCache.putIfAbsent(key, value);
            }
        }
    }

    @Override
    public void put(K key, V value) {
        if (key == null || value == null) {
            if (key != null) {
                memoryCache.remove(key);
            }
            throw new IllegalArgumentException("Key and value must not be null");
        }
        synchronized (fileLock) {
            if (memoryCache.size() >= maxSize && !memoryCache.containsKey(key)) {
                return;
            }
            memoryCache.put(key, value);
        }
    }


    @Override
    public void remove(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        memoryCache.remove(key);
    }

    @Override
    public void clear() {
        memoryCache.clear();
    }

    @Override
    public boolean containsKey(K key) {
        if (key == null) {
            throw new IllegalArgumentException("Key must not be null");
        }
        return memoryCache.containsKey(key);
    }

    @Override
    public void save() {
        if (closed) return;

        synchronized (fileLock) {
            try {
                Path path = Paths.get(cacheFile);
                Path parent = path.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }

                Path tempFile = Paths.get(cacheFile + ".tmp");

                try (ObjectOutputStream oos = new ObjectOutputStream(
                        new BufferedOutputStream(Files.newOutputStream(tempFile)))) {
                    oos.writeObject(new ConcurrentHashMap<>(memoryCache));
                    oos.flush();
                }

                Files.move(tempFile, path);

            } catch (IOException e) {
                System.err.println("Failed to save cache to " + cacheFile + ": " + e.getMessage());
            }
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void load() {
        synchronized (fileLock) {
            Path path = Paths.get(cacheFile);
            if (!Files.exists(path)) {
                return;
            }

            try (ObjectInputStream ois = new ObjectInputStream(
                    new BufferedInputStream(Files.newInputStream(path)))) {
                ConcurrentMap<K, V> loaded = (ConcurrentMap<K, V>) ois.readObject();
                memoryCache.clear();
                memoryCache.putAll(loaded);
            } catch (IOException | ClassNotFoundException e) {
                System.err.println("Failed to load cache from " + cacheFile + ": " + e.getMessage());
            }
        }
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
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        save();
    }
}
