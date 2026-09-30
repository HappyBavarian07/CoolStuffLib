package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionKind;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Records a snapshot of the config on every {@link ConfigLifecycleEvent.Type#SAVE}, keeps the history inside the
 * config itself under {@code __versions} and reloads it on initialize.</p>
 *
 * <p>A config publishes SAVE after it wrote itself to disk, so a version captured by that event is written to the file
 * by the config's next save. The history therefore lags exactly one save behind the file; re-entering {@code save()}
 * from a SAVE listener was rejected because it would fire SAVE (and every backup a second time) for all other
 * listeners. Call {@code save()} once more before closing the config to flush the last version.</p>
 */
public class VersioningModule extends AbstractBaseConfigModule {
    /** Config key the version history is stored under. */
    public static final String VERSIONS_KEY = "__versions";
    /** How many snapshots are kept inside the config file by default. */
    public static final int DEFAULT_MAX_VERSIONS = 10;

    private static final String VERSION_FIELD = "__version";
    private static final String TIMESTAMP_FIELD = "__timestamp";
    private static final String SNAPSHOT_FIELD = "snapshot";
    private static final String TYPE_FIELD = "__type__";

    private int version = 0;
    private final List<Map<String, Object>> versions = new ArrayList<>();
    private int maxVersions = DEFAULT_MAX_VERSIONS;

    public VersioningModule() {
        this(DEFAULT_MAX_VERSIONS);
    }

    public VersioningModule(int maxVersions) {
        super("VersioningModule",
                "Tracks configuration versions and changes over time",
                "1.0.0");
        this.maxVersions = Math.max(1, maxVersions);
    }

    @Override
    protected void onInitialize() {

        loadVersionData();
    }

    @Override
    protected void onEnable() {
        registerEventListener(config.getEventBus(),
                ConfigLifecycleEvent.class,
                this::onConfigLifecycle);
    }

    @Override
    protected void onDisable() {
        unregisterEventListeners(config.getEventBus(), ConfigLifecycleEvent.class);

        saveVersion();
    }

    @Override
    protected void onCleanup() {

        versions.clear();
        version = 0;
    }

    private void onConfigLifecycle(ConfigLifecycleEvent event) {
        if (event.getType() == ConfigLifecycleEvent.Type.SAVE) {
            saveVersion();
        }
    }

    public void saveVersion() {
        if (config == null) return;


        Map<String, Object> snapshot = new HashMap<>(config.getRootSection().toSerializableMap());
        // The history lives inside the config, so it must never end up inside one of its own snapshots.
        snapshot.remove(VERSIONS_KEY);
        snapshot.put(VERSION_FIELD, version);
        snapshot.put(TIMESTAMP_FIELD, System.currentTimeMillis());


        versions.add(snapshot);
        version++;
        trimToMaxVersions();
        writeVersionData();
    }

    /**
     * Persists the history as {@code __versions.<n>.__version/__timestamp/snapshot}. The list is keyed by version number
     * instead of being a plain list, because the file handlers write list entries as scalars and would not restore
     * nested snapshots. The history nodes are plain sections on purpose: a typed section is written with a
     * {@code __type__} marker, and the loader only restores the values of such a section, never its subsections.
     */
    private void writeVersionData() {
        ConfigSection target = config.getRootSection().createSection(VERSIONS_KEY, SectionKind.DEFAULT, true);
        for (Map<String, Object> versionData : versions) {
            Map<String, Object> snapshot = new HashMap<>(versionData);
            snapshot.remove(VERSION_FIELD);
            snapshot.remove(TIMESTAMP_FIELD);
            snapshot.remove(VERSIONS_KEY);

            ConfigSection entry = target.createSection(String.valueOf(versionData.get(VERSION_FIELD)),
                    SectionKind.DEFAULT, true);
            entry.set(VERSION_FIELD, versionData.get(VERSION_FIELD));
            entry.set(TIMESTAMP_FIELD, versionData.get(TIMESTAMP_FIELD));

            ConfigSection snapshotSection = entry.createSection(SNAPSHOT_FIELD, SectionKind.DEFAULT, true);
            for (Map.Entry<String, Object> value : snapshot.entrySet()) {
                snapshotSection.set(value.getKey(), value.getValue());
            }
        }
    }

    private void loadVersionData() {

        if (config == null) return;


        Map<String, Object> storedVersions = asMap(config.get(VERSIONS_KEY));
        if (storedVersions == null) return;

        List<Map<String, Object>> loaded = new ArrayList<>();
        for (Map.Entry<String, Object> stored : storedVersions.entrySet()) {
            int storedNumber;
            try {
                storedNumber = Integer.parseInt(stored.getKey());
            } catch (NumberFormatException e) {
                continue;
            }

            Map<String, Object> entry = asMap(stored.getValue());
            if (entry == null) {
                ConfigLogger.warning("Ignoring unreadable version entry '" + stored.getKey() + "' under "
                        + VERSIONS_KEY + " of config " + config.getName(), "VersioningModule", true);
                continue;
            }
            loaded.add(toVersionData(entry, storedNumber));
        }
        loaded.sort(Comparator.comparingInt(data -> ((Number) data.get(VERSION_FIELD)).intValue()));

        versions.clear();
        versions.addAll(loaded);
        trimToMaxVersions();
        version = nextVersionNumber();
    }

    private static Map<String, Object> toVersionData(Map<String, Object> entry, int storedNumber) {
        Map<String, Object> versionData = new HashMap<>();
        Object storedVersion = entry.get(VERSION_FIELD);
        versionData.put(VERSION_FIELD, storedVersion instanceof Number number ? number.intValue() : storedNumber);
        versionData.put(TIMESTAMP_FIELD, entry.get(TIMESTAMP_FIELD));

        Map<String, Object> snapshot = asMap(entry.get(SNAPSHOT_FIELD));
        if (snapshot == null) {
            // Entries written before the snapshot field existed hold the config keys directly.
            snapshot = new HashMap<>(entry);
            snapshot.remove(VERSION_FIELD);
            snapshot.remove(TIMESTAMP_FIELD);
            snapshot.remove(TYPE_FIELD);
        }
        versionData.putAll(snapshot);
        return versionData;
    }

    private static Map<String, Object> asMap(Object value) {
        if (value instanceof ConfigSection section) {
            return section.toSerializableMap();
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(String.valueOf(entry.getKey()), entry.getValue());
            }
            return result;
        }
        return null;
    }

    private void trimToMaxVersions() {
        while (versions.size() > maxVersions) {
            versions.remove(0);
        }
    }

    private int nextVersionNumber() {
        int highest = -1;
        for (Map<String, Object> versionData : versions) {
            highest = Math.max(highest, ((Number) versionData.get(VERSION_FIELD)).intValue());
        }
        return highest + 1;
    }

    public int getCurrentVersion() {
        return version;
    }

    public int getMaxVersions() {
        return maxVersions;
    }

    public void setMaxVersions(int maxVersions) {
        this.maxVersions = Math.max(1, maxVersions);
        trimToMaxVersions();
    }

    public List<Map<String, Object>> getVersionHistory() {
        return new ArrayList<>(versions);
    }

    public Map<String, Object> getVersion(int versionNumber) {
        for (Map<String, Object> versionData : versions) {
            if (versionData.containsKey(VERSION_FIELD)) {
                int storedVersion = ((Number) versionData.get(VERSION_FIELD)).intValue();
                if (storedVersion == versionNumber) {
                    return new HashMap<>(versionData);
                }
            }
        }
        return null;
    }

    @Override
    public void configure(Map<String, Object> configuration) {
        if (configuration == null) return;

        super.configure(configuration);
        Object value = configuration.get("maxVersions");
        if (value instanceof Number number) {
            setMaxVersions(number.intValue());
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("currentVersion", version);
        state.put("versionCount", versions.size());
        state.put("maxVersions", maxVersions);
        return state;
    }
}
