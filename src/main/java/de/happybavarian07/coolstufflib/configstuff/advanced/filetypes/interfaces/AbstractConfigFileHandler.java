package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractConfigFileHandler implements ConfigFileHandler {
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    @Override
    public void save(File file, Map<String, Object> data) throws IOException {
        lock.writeLock().lock();
        try {
            // Ensure directory exists
            File parent = file.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                throw new IOException("Failed to create directory: " + parent);
            }

            // Create a defensive copy of the data
            Map<String, Object> dataCopy = new ConcurrentHashMap<>(data);

            // Delegate to implementation
            doSave(file, dataCopy);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    @Override
    public Map<String, Object> load(File file) throws IOException {
        lock.readLock().lock();
        try {
            Map<String, Object> result = doLoad(file);
            return result != null ? new ConcurrentHashMap<>(result) : new ConcurrentHashMap<>();
        } finally {
            lock.readLock().unlock();
        }
    }
    
    protected abstract void doSave(File file, Map<String, Object> data) throws IOException;
    protected abstract Map<String, Object> doLoad(File file) throws IOException;
}