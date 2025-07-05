package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;

import java.util.*;

public class HistoryModule extends AbstractBaseConfigModule {
    private final Map<String, Deque<Object>> history = new HashMap<>();
    private int maxHistorySize = 10;

    public HistoryModule() {
        super("HistoryModule",
                "Tracks changes to configuration values with undo capability",
                "1.0.0");
    }

    public HistoryModule(int maxHistorySize) {
        this();
        this.maxHistorySize = Math.max(1, maxHistorySize);
    }

    @Override
    protected void onInitialize() {
        // Nothing to initialize
    }

    @Override
    protected void onEnable() {
        // Register for value change events
        registerEventListener(
                config.getEventBus(),
                ConfigValueEvent.class,
                this::onValueChangeEvent
        );
    }

    @Override
    protected void onDisable() {
        // Unregister from value change events
        unregisterEventListener(
                config.getEventBus(),
                ConfigValueEvent.class,
                this::onValueChangeEvent
        );
    }

    @Override
    protected void onCleanup() {
        history.clear();
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        if (event.getType() == ConfigValueEvent.Type.SET) {
            String key = event.getFullPath();
            Object oldValue = event.getOldValue();

            if (oldValue != null) {
                Deque<Object> keyHistory = history.computeIfAbsent(key, k -> new ArrayDeque<>());
                keyHistory.push(oldValue);

                // Limit history size
                while (keyHistory.size() > maxHistorySize) {
                    keyHistory.removeLast();
                }
            }
        }
    }

    public boolean canRollback(String key) {
        Deque<Object> stack = history.get(key);
        return stack != null && !stack.isEmpty();
    }

    public Object rollback(String key) {
        if (config == null) {
            return null;
        }

        Deque<Object> stack = history.get(key);
        if (stack == null || stack.isEmpty()) {
            return null;
        }

        Object previousValue = stack.pop();
        config.set(key, previousValue);
        return previousValue;
    }

    public void clearHistory() {
        history.clear();
    }

    public void clearHistory(String key) {
        history.remove(key);
    }

    public int getHistorySize(String key) {
        Deque<Object> stack = history.get(key);
        return stack != null ? stack.size() : 0;
    }

    public Object peekHistory(String key) {
        Deque<Object> stack = history.get(key);
        return stack != null && !stack.isEmpty() ? stack.peek() : null;
    }

    public List<Object> getHistoryValues(String key) {
        Deque<Object> stack = history.get(key);
        if (stack == null || stack.isEmpty()) {
            return Collections.emptyList();
        }
        return new ArrayList<>(stack);
    }

    public Set<String> getTrackedKeys() {
        return new HashSet<>(history.keySet());
    }

    public int getMaxHistorySize() {
        return maxHistorySize;
    }

    public void setMaxHistorySize(int maxHistorySize) {
        this.maxHistorySize = Math.max(1, maxHistorySize);

        // Trim any histories that are now too large
        for (Deque<Object> stack : history.values()) {
            while (stack.size() > maxHistorySize) {
                stack.removeLast();
            }
        }
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        for (Map.Entry<String, Deque<Object>> entry : history.entrySet()) {
            state.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        state.put("maxHistorySize", maxHistorySize);
        return state;
    }
}
