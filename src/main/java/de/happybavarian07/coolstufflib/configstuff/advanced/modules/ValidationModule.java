package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ConfigModule;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

public class ValidationModule extends ConfigModule {
    private final Map<String, ValidationRule> rules = new HashMap<>();
    private final Map<String, String> lastErrors = new HashMap<>();

    @Override
    public String getName() {
        return "ValidationModule";
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
    }

    @Override
    public void onConfigChange(String key, Object oldValue, Object newValue) {
        ValidationRule rule = rules.get(key);
        if (rule != null) {
            String error = rule.validate(newValue);
            if (error != null) {
                lastErrors.put(key, error);
            } else {
                lastErrors.remove(key);
            }
        }
    }

    @Override
    public boolean supportsConfig(AdvancedConfig config) {
        return true;
    }

    @Override
    public Map<String, Object> getModuleState() {
        return Map.of();
    }

    @Override
    public Object onGetValue(String key, Object value) {
        return value;
    }

    public void addRule(String key, Class<?> type, boolean required, Predicate<Object> predicate, String errorMessage) {
        rules.put(key, new ValidationRule(type, required, predicate, errorMessage));
    }

    public String getLastError(String key) {
        return lastErrors.get(key);
    }

    private record ValidationRule(Class<?> type, boolean required, Predicate<Object> predicate, String errorMessage) {

        String validate(Object value) {
            if (required && value == null) return "Value required";
            if (value != null && type != null && !type.isInstance(value))
                return "Wrong type: expected " + type.getSimpleName();
            if (predicate != null && value != null && !predicate.test(value))
                return errorMessage != null ? errorMessage : "Predicate failed";
            return null;
        }
    }
}
