package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

public abstract class AbstractConfigFileHandler implements ConfigFileHandler {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    protected final Pattern fileExtension;
    protected volatile boolean wasCalled = false;

    protected AbstractConfigFileHandler(Pattern fileExtensions) {
        this.fileExtension = fileExtensions;
    }

    @Override
    public void save(File file, Map<String, Object> data) throws IOException {
        wasCalled = true;
        save(file, data, Collections.emptyMap());
    }

    @Override
    public void save(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        wasCalled = true;
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }
        if (data == null) {
            throw new IllegalArgumentException("Data must not be null");
        }
        if (!canHandle(file)) {
            throw new IOException("File type not supported by this handler: " + file.getName());
        }
        lock.writeLock().lock();
        try {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    throw new IOException("Failed to create file: " + file.getPath());
                }
            }
            if (!file.canWrite()) {
                throw new IOException("File cannot be written: " + file.getPath());
            }
            Map<String, Object> dataCopy = new ConcurrentHashMap<>();
            for (Map.Entry<String, Object> entry : data.entrySet()) {
                Object value = entry.getValue();
                if (value instanceof ConfigSection section) {
                    dataCopy.put(entry.getKey(), section.toSerializableMap());
                } else {
                    dataCopy.put(entry.getKey(), value);
                }
            }
            Map<String, String> commentsCopy = new ConcurrentHashMap<>(comments);
            doSave(file, dataCopy, commentsCopy);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Map<String, Object> load(File file) throws IOException {
        wasCalled = true;
        if (file == null) {
            throw new IllegalArgumentException("File must not be null");
        }
        if (!file.exists()) {
            throw new IOException("File does not exist: " + file.getPath());
        }
        if (!file.isFile()) {
            throw new IOException("Not a file: " + file.getPath());
        }
        if (!canHandle(file)) {
            throw new IOException("File type not supported by this handler: " + file.getName());
        }
        lock.readLock().lock();
        try {
            if (!file.canRead()) {
                throw new IOException("File cannot be read: " + file.getPath());
            }
            if (file.length() == 0) {
                ConfigLogger.warn("File is empty: " + file.getPath(), "ConfigFileHandler", true);
                return new ConcurrentHashMap<>();
            }
            Map<String, Object> result = doLoad(file);

            if (result == null) return new ConcurrentHashMap<>();
            return result;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Map<String, String> loadComments(File file) throws IOException {
        wasCalled = true;
        lock.readLock().lock();
        try {
            return new ConcurrentHashMap<>(doLoadComments(file));
        } catch (Exception e) {
            return new ConcurrentHashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }

    protected abstract void doSave(File file, Map<String, Object> data) throws IOException;

    protected void doSave(File file, Map<String, Object> data, Map<String, String> comments) throws IOException {
        // Default implementation ignores comments
        doSave(file, data);
    }

    protected abstract Map<String, Object> doLoad(File file) throws IOException;

    protected Map<String, String> doLoadComments(File file) throws IOException {
        // Default implementation returns no comments
        return Collections.emptyMap();
    }

    @Override
    public abstract boolean supportsComments();

    @Override
    public boolean canHandle(File file) {
        try {
            lock.readLock().lock();
            return canHandleInternal(file);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public String getFileExtension() {
        return fileExtension.pattern();
    }

    private boolean canHandleInternal(File file) {
        try {
            String fileName = file.getName();
            return this.fileExtension.matcher(fileName).matches();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean wasCalled() {
        return wasCalled;
    }
}
