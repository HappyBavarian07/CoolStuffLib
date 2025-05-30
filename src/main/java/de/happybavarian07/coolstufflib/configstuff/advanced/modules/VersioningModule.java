package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class VersioningModule extends ConfigModule {
    private int version = 0;
    private final List<Map<String, Object>> versions = new ArrayList<>();

    @Override
    public String getName() {
        return "VersioningModule";
    }

    @Override
    public void enable() { /* Do nothing */ }

    @Override
    public void disable() { /* Do nothing */ }

    @Override
    public void onAttach(AdvancedConfig config) {
        super.onAttach(config);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public void reload() {
    }

    @Override
    public void save() {
        if(getConfig() == null) return;
        if (!isEnabled()) return;
        saveVersion();
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return true;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of("version", version, "versions", new ArrayList<>(versions));
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    public int getVersion() {
        return version;
    }

    public void saveVersion() {
        if (getConfig() == null) return;
        Map<String, Object> snapshot = new HashMap<>(getConfig().getValueMap());
        versions.add(new HashMap<>(snapshot));
        version = versions.size();
    }

    public boolean rollbackToVersion(int v) {
        if (v < 1 || v > versions.size() || getConfig() == null) return false;
        Map<String, Object> snapshot = versions.get(v - 1);
        for (Map.Entry<String, Object> entry : snapshot.entrySet()) {
            getConfig().setValue(entry.getKey(), entry.getValue());
        }
        version = v;
        return true;
    }
}
