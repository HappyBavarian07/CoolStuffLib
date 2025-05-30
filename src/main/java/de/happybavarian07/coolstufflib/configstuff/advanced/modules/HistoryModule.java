package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.ArrayList;

public class HistoryModule extends ConfigModule {
    private final Map<String, Deque<Object>> history = new HashMap<>();

    @Override
    public String getName() { return "HistoryModule"; }
    @Override
    public void enable() { /* Do nothing */ }
    @Override
    public void disable() { /* Do nothing */ }
    @Override
    public void reload() {}
    @Override
    public void save() {}
    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
        if (oldValue != null) {
            history.computeIfAbsent(key, k -> new ArrayDeque<>()).push(oldValue);
        }
    }
    @Override
    public boolean supportsConfig(AdvancedConfig config) { return true; }
    @Override
    public Map<String, Object> getModuleState() {
        Map<String, Object> state = new HashMap<>();
        for (var entry : history.entrySet()) {
            state.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return state;
    }
    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }
    public boolean canRollback(String key) {
        Deque<Object> stack = history.get(key);
        return stack != null && !stack.isEmpty();
    }
    public Object rollback(String key) {
        Deque<Object> stack = history.get(key);
        if(getConfig() == null || stack == null || stack.isEmpty()) {
            return null;
        }
        if(!isEnabled()) {
            return null;
        }
        Object oldValue = stack.pop();
        getConfig().setValue(key, oldValue);
        return oldValue;
    }
}
