package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersioningModule extends AbstractBaseConfigModule {
    private int version = 0;
    private final List<Map<String, Object>> versions = new ArrayList<>();

    public VersioningModule() {
        super("VersioningModule",
                "Tracks configuration versions and changes over time",
                "1.0.0");
    }

    @Override
    protected void onInitialize() {

        loadVersionData();
    }

    @Override
    protected void onEnable() {
        // Nothing to do on enable
    }

    @Override
    protected void onDisable() {

        saveVersion();
    }

    @Override
    protected void onCleanup() {

        versions.clear();
        version = 0;
    }

    public void saveVersion() {
        if (config == null) return;


        Map<String, Object> snapshot = new HashMap<>(config.getRootSection().toSerializableMap());
        snapshot.put("__version", version);
        snapshot.put("__timestamp", System.currentTimeMillis());


        versions.add(snapshot);
        version++;
    }

    private void loadVersionData() {

        if (config == null) return;


        Object storedVersions = config.get("__versions");
        if (storedVersions instanceof List) {
            try {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> loadedVersions = (List<Map<String, Object>>) storedVersions;
                versions.clear();
                versions.addAll(loadedVersions);


                for (Map<String, Object> versionData : versions) {
                    if (versionData.containsKey("__version")) {
                        int versionNumber = ((Number) versionData.get("__version")).intValue();
                        if (versionNumber > version) {
                            version = versionNumber;
                        }
                    }
                }
            } catch (ClassCastException e) {

            }
        }
    }

    public int getCurrentVersion() {
        return version;
    }

    public List<Map<String, Object>> getVersionHistory() {
        return new ArrayList<>(versions);
    }

    public Map<String, Object> getVersion(int versionNumber) {
        for (Map<String, Object> versionData : versions) {
            if (versionData.containsKey("__version")) {
                int storedVersion = ((Number) versionData.get("__version")).intValue();
                if (storedVersion == versionNumber) {
                    return new HashMap<>(versionData);
                }
            }
        }
        return null;
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }
}
