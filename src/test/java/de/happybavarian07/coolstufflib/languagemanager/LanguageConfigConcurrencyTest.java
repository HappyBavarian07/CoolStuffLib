package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.storage.*;
import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * <p>A reload can be started from the watcher thread while the main thread reloads the same language, so the
 * swap in {@link LanguageConfig} is driven from a scripted backend that can be stopped at a chosen moment.</p>
 */
class LanguageConfigConcurrencyTest {
    private static final String BACKEND = "gated-concurrency-test";
    private static final String KEY = "Messages.Player.Greeting";
    private static final BlockingQueue<Step> SCRIPT = new LinkedBlockingQueue<>();

    @TempDir
    Path folder;

    @BeforeAll
    static void registerTheScriptedBackend() {
        LanguageStorage.registerBackend(BACKEND, folder -> new GatedBackend());
    }

    @Test
    void aReloadThatStartedEarlierDoesNotOverwriteANewerOne() throws Exception {
        SCRIPT.add(step(reads("one")));
        SCRIPT.add(step(reads("one")));
        Step newer = gate(reads("two"));
        Step older = gate(broken());
        SCRIPT.add(newer);
        SCRIPT.add(older);

        LanguageConfig config = newConfig();
        assertEquals("one", config.getConfig().get(KEY));

        List<Throwable> failures = new CopyOnWriteArrayList<>();
        Thread first = reloading(config, failures, "reload-first");
        assertTrue(newer.entered.await(10, TimeUnit.SECONDS), "the first reload never reached the backend");
        Thread second = reloading(config, failures, "reload-second");
        // An unguarded reloadConfig lets the second one read the same generation; a guarded one keeps it out.
        older.entered.await(2, TimeUnit.SECONDS);

        newer.release.countDown();
        join(first);
        older.release.countDown();
        join(second);

        assertTrue(failures.isEmpty(), failures.toString());
        assertEquals("two", config.getConfig().get(KEY), "the slower reload chained from the older generation");
        assertEquals("two", config.getLoaded().merged().get(KEY).value());
    }

    @Test
    void loadedAndConfigArePublishedTogetherAsOneSnapshot() {
        List<Field> published = Arrays.stream(LanguageConfig.class.getDeclaredFields())
                .filter(field -> !Modifier.isStatic(field.getModifiers()))
                .filter(field -> carries(field.getType(), LoadResult.class)
                        && carries(field.getType(), FileConfiguration.class))
                .toList();

        assertEquals(1, published.size(), "one field must publish the load result and the config together: " + published);
        assertTrue(Modifier.isVolatile(published.get(0).getModifiers()), published.get(0) + " must be volatile");
    }

    private LanguageConfig newConfig() {
        LanguageStorage.useBackend(folder.toFile(), BACKEND);
        return new LanguageConfig(folder.resolve("en.yml").toFile(), folder.toFile(), "lang-fixtures/legacy", "en");
    }

    private static boolean carries(Class<?> type, Class<?> value) {
        return Arrays.stream(type.getDeclaredMethods()).anyMatch(m -> value.isAssignableFrom(m.getReturnType()));
    }

    private static Thread reloading(LanguageConfig config, List<Throwable> failures, String name) {
        Thread thread = new Thread(() -> {
            try {
                config.reloadConfig();
            } catch (Throwable t) {
                failures.add(t);
            }
        }, name);
        thread.setDaemon(true);
        thread.start();
        return thread;
    }

    private static void join(Thread thread) throws InterruptedException {
        thread.join(TimeUnit.SECONDS.toMillis(10));
        assertFalse(thread.isAlive(), thread.getName() + " did not finish");
    }

    private static ReadResult reads(String value) {
        LanguageEntry entry = new LanguageEntry(KEY, value, null, new LanguageEntry.Location("en/messages/Player.yml", 1));
        return new ReadResult(Map.of(KEY, entry), Map.of(), List.of());
    }

    /** The file this language owns cannot be parsed, so its keys have to keep the values of the previous load. */
    private static ReadResult broken() {
        return new ReadResult(Map.of(), Map.of(), List.of(new LanguageProblem("en", 1, "broken yaml")));
    }

    private static Step step(ReadResult result) {
        return new Step(result, new CountDownLatch(0));
    }

    private static Step gate(ReadResult result) {
        return new Step(result, new CountDownLatch(1));
    }

    /** One scripted {@link LanguageBackend#read(String)}, optionally parked until the test lets it continue. */
    private static final class Step {
        private final ReadResult result;
        private final CountDownLatch entered = new CountDownLatch(1);
        private final CountDownLatch release;

        private Step(ReadResult result, CountDownLatch release) {
            this.result = result;
            this.release = release;
        }

        private void enter() {
            entered.countDown();
            try {
                release.await();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    private static final class GatedBackend implements LanguageBackend {
        @Override
        public String id() {
            return BACKEND;
        }

        @Override
        public Set<String> languages() {
            return Set.of("en");
        }

        @Override
        public ReadResult read(String language) {
            Step step = SCRIPT.poll();
            if (step == null) throw new IllegalStateException("the scripted backend was read more often than the test scripts");
            step.enter();
            return step.result;
        }

        @Override
        public List<LanguageProblem> write(String language, Collection<LanguageEntry> entries, Map<String, String> sectionComments) {
            return List.of();
        }

        @Override
        public boolean isWritable() {
            return false;
        }
    }
}
