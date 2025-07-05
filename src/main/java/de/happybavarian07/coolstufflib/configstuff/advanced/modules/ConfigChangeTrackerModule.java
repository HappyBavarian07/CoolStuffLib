package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.*;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.io.File;
import java.util.*;
import java.util.function.Predicate;

public class ConfigChangeTrackerModule extends AbstractBaseConfigModule {
    private final List<String> changes = new ArrayList<>();
    private int maxChangesToTrack = 100;
    private boolean trackValueChanges = true;
    private boolean trackSectionChanges = true;

    private final ConfigEventListener<ConfigValueEvent> valueChangeListener = this::onValueChange;
    private final ConfigEventListener<ConfigSectionEvent> sectionChangeListener = this::onSectionChange;
    private final ConfigEventListener<ConfigLifecycleEvent> lifecycleEventListener = this::onLifecycleEvent;

    public ConfigChangeTrackerModule() {
        super("ConfigChangeTracker",
              "Tracks configuration changes and provides a change history",
              "1.0.0");
    }

    @Override
    protected void onInitialize() {
        // Nothing needed for initialization
    }

    @Override
    protected void onEnable() {
        registerEventListeners();
    }

    @Override
    protected void onDisable() {
        unregisterEventListeners();
    }

    @Override
    protected void onCleanup() {
        changes.clear();
    }

    private void registerEventListeners() {
        if (trackValueChanges) {
            registerEventListener(
                config.getEventBus(),
                ConfigValueEvent.class,
                valueChangeListener
            );
        }

        if (trackSectionChanges) {
            registerEventListener(
                config.getEventBus(),
                ConfigSectionEvent.class,
                sectionChangeListener
            );
        }

        // Always track config lifecycle events
        registerEventListener(
            config.getEventBus(),
            ConfigLifecycleEvent.class,
            lifecycleEventListener
        );
    }

    private void unregisterEventListeners() {
        if (config == null) {
            return;
        }
        unregisterEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            valueChangeListener
        );
        unregisterEventListener(
            config.getEventBus(),
            ConfigSectionEvent.class,
            sectionChangeListener
        );
        unregisterEventListener(
            config.getEventBus(),
            ConfigLifecycleEvent.class,
            lifecycleEventListener
        );
    }

    private void onValueChange(ConfigValueEvent event) {
        String timestamp = new Date().toString();
        String path = event.getFullPath();
        String changeType = event.getType() == ConfigValueEvent.Type.SET ? "SET" :
                           (event.getType() == ConfigValueEvent.Type.REMOVE ? "REMOVE" : "GET");
        String oldValue = event.getOldValue() != null ? event.getOldValue().toString() : "null";
        String newValue = event.getNewValue() != null ? event.getNewValue().toString() : "null";

        String changeLog = String.format("[%s] %s %s - old: %s, new: %s",
                timestamp, changeType, path, oldValue, newValue);

        addChange(changeLog);
    }

    private void onSectionChange(ConfigSectionEvent event) {
        String timestamp = new Date().toString();
        String path = event.getFullPath();
        String changeType = event.getType() == ConfigSectionEvent.Type.CREATED ? "CREATE_SECTION" : "REMOVE_SECTION";

        String changeLog = String.format("[%s] %s %s", timestamp, changeType, path);

        addChange(changeLog);
    }

    private void onLifecycleEvent(ConfigLifecycleEvent event) {
        String timestamp = new Date().toString();
        String changeType = "";

        switch (event.getType()) {
            case LOAD:
                changeType = "LOADED";
                break;
            case SAVE:
                changeType = "SAVED";
                break;
            case RELOAD:
                changeType = "RELOADED";
                break;
            case CLEAR:
                changeType = "CLEARED";
                break;
            default:
                return;
        }

        String changeLog = String.format("[%s] CONFIG_%s", timestamp, changeType);

        addChange(changeLog);
    }

    private synchronized void addChange(String changeLog) {
        changes.add(changeLog);

        // Maintain change history limit
        while (changes.size() > maxChangesToTrack) {
            changes.remove(0);
        }
    }

    @Override
    public boolean isConfigured() {
        return true;
    }

    @Override
    public void configure(Map<String, Object> configuration) {
        if (configuration.containsKey("maxChangesToTrack")) {
            Object value = configuration.get("maxChangesToTrack");
            if (value instanceof Number) {
                maxChangesToTrack = ((Number) value).intValue();
            }
        }

        if (configuration.containsKey("trackValueChanges")) {
            Object value = configuration.get("trackValueChanges");
            if (value instanceof Boolean) {
                trackValueChanges = (Boolean) value;
            }
        }

        if (configuration.containsKey("trackSectionChanges")) {
            Object value = configuration.get("trackSectionChanges");
            if (value instanceof Boolean) {
                trackSectionChanges = (Boolean) value;
            }
        }

        // If the module is enabled, update listeners based on new configuration
        if (state == ModuleState.ENABLED) {
            unregisterEventListeners();
            registerEventListeners();
        }
    }

    public List<String> getChanges() {
        return new ArrayList<>(changes);
    }

    public List<String> getFilteredChanges(Predicate<String> filter) {
        List<String> filteredChanges = new ArrayList<>();
        for (String change : changes) {
            if (filter.test(change)) {
                filteredChanges.add(change);
            }
        }
        return filteredChanges;
    }

    public void clearChanges() {
        changes.clear();
    }

    public void setMaxChangesToTrack(int maxChanges) {
        this.maxChangesToTrack = Math.max(1, maxChanges);

        // Trim the change list if it's now too large
        while (changes.size() > maxChangesToTrack) {
            changes.remove(0);
        }
    }

    public int getMaxChangesToTrack() {
        return maxChangesToTrack;
    }

    public boolean isTrackValueChanges() {
        return trackValueChanges;
    }

    public void setTrackValueChanges(boolean trackValueChanges) {
        if (this.trackValueChanges != trackValueChanges) {
            this.trackValueChanges = trackValueChanges;

            // Update event listeners if the module is enabled
            if (state == ModuleState.ENABLED) {
                unregisterEventListeners();
                registerEventListeners();
            }
        }
    }

    public boolean isTrackSectionChanges() {
        return trackSectionChanges;
    }

    public void setTrackSectionChanges(boolean trackSectionChanges) {
        if (this.trackSectionChanges != trackSectionChanges) {
            this.trackSectionChanges = trackSectionChanges;

            // Update event listeners if the module is enabled
            if (state == ModuleState.ENABLED) {
                unregisterEventListeners();
                registerEventListeners();
            }
        }
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }

    public Map<String, Object> exportToMap() {
        Map<String, Object> exportedChanges = new HashMap<>();
        exportedChanges.put("changes", new ArrayList<>(changes));
        exportedChanges.put("maxChangesToTrack", maxChangesToTrack);
        exportedChanges.put("trackValueChanges", trackValueChanges);
        exportedChanges.put("trackSectionChanges", trackSectionChanges);
        return exportedChanges;
    }

    public void saveToFile(File file) {
        if (file == null || !file.exists()) {
            throw new IllegalArgumentException("File cannot be null or does not exist");
        }

        try {
            StringBuilder sb = new StringBuilder();
            sb.append("Max Changes to Track: ").append(maxChangesToTrack).append("\n");
            sb.append("Track Value Changes: ").append(trackValueChanges).append("\n");
            sb.append("Track Section Changes: ").append(trackSectionChanges).append("\n");
            sb.append("Changes:\n");

            for (String change : changes) {
                sb.append(change).append("\n");
            }

            java.nio.file.Files.writeString(file.toPath(), sb.toString());
        } catch (Exception e) {
            throw new RuntimeException("Failed to save changes to file: " + e.getMessage(), e);
        }
    }
}
