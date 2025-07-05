package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

public class NotificationModule extends AbstractBaseConfigModule {
    private final List<BiConsumer<String, Object>> listeners = new ArrayList<>();

    public NotificationModule() {
        super("NotificationModule",
              "Notifies listeners of configuration changes",
              "1.0.0");
    }

    @Override
    protected void onInitialize() {
        // Nothing to initialize
    }

    @Override
    protected void onEnable() {
        registerEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onDisable() {
        unregisterEventListener(
            config.getEventBus(),
            ConfigValueEvent.class,
            this::onValueChangeEvent
        );
    }

    @Override
    protected void onCleanup() {
        listeners.clear();
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        if (event.getType() == ConfigValueEvent.Type.SET) {
            String key = event.getFullPath();
            Object newValue = event.getNewValue();

            for (BiConsumer<String, Object> listener : new ArrayList<>(listeners)) {
                try {
                    listener.accept(key, newValue);
                } catch (Exception e) {
                    // Prevent errors in listeners from affecting other listeners
                }
            }
        }
    }

    public void addListener(BiConsumer<String, Object> listener) {
        if (listener != null) {
            listeners.add(listener);
        }
    }

    public void removeListener(BiConsumer<String, Object> listener) {
        listeners.remove(listener);
    }

    public boolean hasListener(BiConsumer<String, Object> listener) {
        return listeners.contains(listener);
    }

    public int getListenerCount() {
        return listeners.size();
    }

    public Map<String, Object> getAdditionalModuleState() {
        Map<String, Object> state = new HashMap<>();
        state.put("listenerCount", listeners.size());
        return state;
    }
}
