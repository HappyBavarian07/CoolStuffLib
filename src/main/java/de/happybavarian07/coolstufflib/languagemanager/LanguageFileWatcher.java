package de.happybavarian07.coolstufflib.languagemanager;

import java.io.IOException;
import java.nio.file.*;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.stream.Stream;

import static java.nio.file.StandardWatchEventKinds.*;

/** Calls {@code onChange} with the changed language names once no file changed for {@code debounceMillis}. */
public final class LanguageFileWatcher implements AutoCloseable {
    private final Path root;
    private final long debounceMillis;
    private final Consumer<Set<String>> onChange;
    private final WatchService service;
    private final Thread thread;
    private volatile boolean running = true;

    public LanguageFileWatcher(Path root, long debounceMillis, Consumer<Set<String>> onChange) throws IOException {
        this.root = root;
        this.debounceMillis = debounceMillis;
        this.onChange = onChange;
        this.service = root.getFileSystem().newWatchService();
        this.thread = new Thread(this::loop, "CoolStuffLib-LanguageWatcher");
        this.thread.setDaemon(true);
    }

    public void start() throws IOException {
        try (Stream<Path> dirs = Files.walk(root)) {
            for (Path dir : dirs.filter(Files::isDirectory).toList()) register(dir);
        }
        thread.start();
    }

    private void register(Path dir) throws IOException {
        Path relative = root.relativize(dir);
        if (relative.getNameCount() > 0 && relative.getName(0).toString().startsWith("_")) return;
        dir.register(service, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
    }

    /** A whole tree can be copied in at once, so a new directory brings its own subdirectories with it. */
    private void registerTree(Path dir) {
        try (Stream<Path> dirs = Files.walk(dir)) {
            dirs.filter(Files::isDirectory).forEach(this::registerQuietly);
        } catch (IOException ignored) {
        }
    }

    private void registerQuietly(Path dir) {
        try {
            register(dir);
        } catch (IOException ignored) {
        }
    }

    private void loop() {
        Set<String> pending = new HashSet<>();
        long last = 0;
        while (running) {
            WatchKey key;
            try {
                key = service.poll(Math.max(50, debounceMillis / 4), TimeUnit.MILLISECONDS);
            } catch (InterruptedException | ClosedWatchServiceException e) {
                return;
            }
            if (key != null) {
                Path dir = (Path) key.watchable();
                for (WatchEvent<?> event : key.pollEvents()) {
                    if (event.kind() == OVERFLOW) {
                        addEveryLanguage(pending);
                        last = System.currentTimeMillis();
                        continue;
                    }
                    if (!(event.context() instanceof Path name)) continue;
                    Path changed = dir.resolve(name);
                    if (name.toString().endsWith(".tmp")) continue;
                    if (event.kind() == ENTRY_CREATE && Files.isDirectory(changed)) registerTree(changed);
                    String language = languageOf(changed);
                    if (language != null) {
                        pending.add(language);
                        last = System.currentTimeMillis();
                    }
                }
                key.reset();
            } else if (!pending.isEmpty() && System.currentTimeMillis() - last >= debounceMillis) {
                Set<String> languages = Set.copyOf(pending);
                pending.clear();
                try {
                    onChange.accept(languages);
                } catch (RuntimeException e) {
                    LanguageManager.getLogger().log(Level.WARNING, "Reloading the changed languages failed", e);
                }
            }
        }
    }

    /** Lost events cannot be attributed to a language, so every language counts as changed. */
    private void addEveryLanguage(Set<String> pending) {
        try (Stream<Path> children = Files.list(root)) {
            children.map(child -> languageOf(child)).filter(Objects::nonNull).forEach(pending::add);
        } catch (IOException ignored) {
        }
    }

    private String languageOf(Path changed) {
        Path relative = root.relativize(changed);
        if (relative.getNameCount() == 0) return null;
        String first = relative.getName(0).toString();
        if (first.startsWith("_") || first.startsWith(".")) return null;
        if (relative.getNameCount() == 1) return first.endsWith(".yml") ? first.substring(0, first.length() - 4) : null;
        return first;
    }

    @Override
    public void close() {
        running = false;
        try {
            service.close();
        } catch (IOException ignored) {
        }
        thread.interrupt();
    }
}
