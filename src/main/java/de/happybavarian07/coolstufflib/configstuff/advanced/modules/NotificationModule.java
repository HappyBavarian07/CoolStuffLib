package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class NotificationModule extends ConfigModule {
    private final List<BiConsumer<String, Object>> listeners = new ArrayList<>();

    @Override public String getName() { return "NotificationModule"; }
    @Override public void enable() { /* Do nothing */ }
    @Override public void disable() { /* Do nothing */ }
    @Override public void reload() {}
    @Override public void save() {}
    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }
    public void addListener(BiConsumer<String, Object> listener) {
        listeners.add(listener);
    }
    public void removeListener(BiConsumer<String, Object> listener) {
        listeners.remove(listener);
    }
    @Override public void onConfigChange(String key, Object oldValue, Object newValue) {
        for (BiConsumer<String, Object> listener : listeners) {
            listener.accept(key, newValue);
        }
    }
    @Override public Map<String, Object> getModuleState() {
        return Map.of("listenerCount", listeners.size());
    }
    @Override public boolean supportsConfig(AdvancedConfig config) { return true; }
}
