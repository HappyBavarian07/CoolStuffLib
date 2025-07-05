package de.happybavarian07.coolstufflib.configstuff.advanced.modules;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * <p>Configuration validation module that enforces custom validation rules on configuration values
 * in real-time. Automatically validates values when they are set and maintains error tracking
 * for failed validations.</p>
 *
 * <p>This module provides:</p>
 * <ul>
 * <li>Real-time validation of configuration values as they are modified</li>
 * <li>Custom validation rule definition and management</li>
 * <li>Error tracking and reporting for failed validations</li>
 * <li>Event-driven validation triggered by configuration changes</li>
 * <li>Support for multiple validation rules per configuration key</li>
 * </ul>
 *
 * <pre><code>
 * ValidationModule validator = new ValidationModule();
 * config.registerModule(validator);
 *
 * // Add validation rules
 * validator.addRule("server.port", new RangeValidationRule(1, 65535));
 * validator.addRule("database.host", new NonEmptyStringValidationRule());
 *
 * // Values are automatically validated when set
 * config.set("server.port", 8080); // Valid
 * config.set("server.port", 70000); // Invalid - will be tracked as error
 * </code></pre>
 */
public class ValidationModule extends AbstractBaseConfigModule {
    private final Map<String, List<ValidationRule>> rules = new HashMap<>();
    private final Map<String, String> lastErrors = new HashMap<>();

    /**
     * <p>Constructs a new ValidationModule with default configuration.</p>
     *
     * <pre><code>
     * ValidationModule validator = new ValidationModule();
     * config.registerModule(validator);
     * </code></pre>
     */
    public ValidationModule() {
        super("ValidationModule",
                "Validates configuration values against defined rules",
                "1.0.0");
    }

    @Override
    protected void onInitialize() {
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
        rules.clear();
        lastErrors.clear();
    }

    private void onValueChangeEvent(ConfigValueEvent event) {
        if (event.getType() == ConfigValueEvent.Type.SET) {
            String key = event.getFullPath();
            Object newValue = event.getNewValue();
            validateValue(key, newValue);
        }
    }

    /**
     * <p>Validates a configuration value against all registered rules for the specified key.</p>
     *
     * <pre><code>
     * boolean isValid = validator.validateValue("server.port", 8080);
     * if (!isValid) {
     *     String error = validator.getErrors().get("server.port");
     *     System.out.println("Validation failed: " + error);
     * }
     * </code></pre>
     *
     * @param key the configuration key to validate
     * @param value the value to validate against the rules
     * @return true if validation passes, false if any rule fails
     */
    public boolean validateValue(String key, Object value) {
        List<ValidationRule> ruleList = rules.get(key);
        if (ruleList != null && !ruleList.isEmpty()) {
            for (ValidationRule rule : ruleList) {
                String error = rule.validate(value);
                if (error != null) {
                    lastErrors.put(key, error);
                    return false;
                }
            }
            lastErrors.remove(key);
            return true;
        }
        return true;
    }

    /**
     * <p>Adds a validation rule for a specific configuration key. If the key already has a value,
     * it will be immediately validated against the new rule.</p>
     *
     * <pre><code>
     * ValidationRule portRule = new RangeValidationRule(1, 65535);
     * validator.addRule("server.port", portRule);
     *
     * ValidationRule hostRule = new NonEmptyStringValidationRule();
     * validator.addRule("database.host", hostRule);
     * </code></pre>
     *
     * @param key the configuration key to apply the rule to
     * @param rule the validation rule to add
     */
    public void addRule(String key, ValidationRule rule) {
        rules.computeIfAbsent(key, k -> new ArrayList<>()).add(rule);
        if (config != null && config.containsKey(key)) {
            validateValue(key, config.get(key));
        }
    }

    /**
     * <p>Removes all validation rules for the specified configuration key and clears any associated errors.</p>
     *
     * <pre><code>
     * validator.removeRule("server.port");
     * // All rules for server.port are now removed
     * </code></pre>
     *
     * @param key the configuration key to remove rules from
     */
    public void removeRule(String key) {
        rules.remove(key);
        lastErrors.remove(key);
    }

    /**
     * <p>Checks if any validation rules are registered for the specified configuration key.</p>
     *
     * <pre><code>
     * if (validator.hasRule("server.port")) {
     *     System.out.println("Port validation is active");
     * }
     * </code></pre>
     *
     * @param key the configuration key to check
     * @return true if at least one rule is registered for the key, false otherwise
     */
    public boolean hasRule(String key) {
        List<ValidationRule> ruleList = rules.get(key);
        return ruleList != null && !ruleList.isEmpty();
    }

    /**
     * <p>Gets a copy of all current validation errors. The map contains configuration keys
     * mapped to their respective error messages.</p>
     *
     * <pre><code>
     * Map&lt;String, String&gt; errors = validator.getErrors();
     * for (Map.Entry&lt;String, String&gt; entry : errors.entrySet()) {
     *     System.out.println(entry.getKey() + ": " + entry.getValue());
     * }
     * </code></pre>
     *
     * @return a new map containing all current validation errors
     */
    public Map<String, String> getErrors() {
        return new HashMap<>(lastErrors);
    }

    /**
     * <p>Checks if there are any current validation errors.</p>
     *
     * <pre><code>
     * if (validator.hasErrors()) {
     *     System.out.println("Configuration has validation errors");
     *     Map&lt;String, String&gt; errors = validator.getErrors();
     *     // Handle errors
     * }
     * </code></pre>
     *
     * @return true if there are validation errors, false otherwise
     */
    public boolean hasErrors() {
        return !lastErrors.isEmpty();
    }

    /**
     * <p>Gets all validation rules registered for a specific configuration key.</p>
     *
     * <pre><code>
     * List&lt;ValidationRule&gt; portRules = validator.getRules("server.port");
     * System.out.println("Port has " + portRules.size() + " validation rules");
     * </code></pre>
     *
     * @param key the configuration key to get rules for
     * @return a list of validation rules for the key, or empty list if none exist
     */
    public List<ValidationRule> getRules(String key) {
        return rules.getOrDefault(key, new ArrayList<>());
    }

    /**
     * <p>Validates all currently registered configuration keys against their rules
     * and updates the error tracking accordingly.</p>
     *
     * <pre><code>
     * validator.validateAll();
     * if (validator.hasErrors()) {
     *     System.out.println("Some configuration values are invalid");
     * }
     * </code></pre>
     *
     * @return true if all validations pass, false if any validation fails
     */
    public boolean validateAll() {
        boolean allValid = true;
        for (String key : rules.keySet()) {
            if (config != null && config.containsKey(key)) {
                if (!validateValue(key, config.get(key))) {
                    allValid = false;
                }
            }
        }
        return allValid;
    }

    /**
     * <p>Clears all validation errors without removing the validation rules themselves.</p>
     *
     * <pre><code>
     * validator.clearErrors();
     * // All error tracking is reset, but rules remain active
     * </code></pre>
     */
    public void clearErrors() {
        lastErrors.clear();
    }

    /**
     * <p>Gets the specific error message for a configuration key, if any exists.</p>
     *
     * <pre><code>
     * String portError = validator.getError("server.port");
     * if (portError != null) {
     *     System.out.println("Port validation error: " + portError);
     * }
     * </code></pre>
     *
     * @param key the configuration key to get the error for
     * @return the error message if validation failed, null if no error exists
     */
    public String getError(String key) {
        return lastErrors.get(key);
    }

    /**
     * <p>Adds a string validation rule for a configuration key. The validator can be a regex pattern
     * or a custom predicate function.</p>
     *
     * <pre><code>
     * validator.addStringValidator("username", "^[a-zA-Z0-9_]{3,16}$");
     * // or using a predicate
     * validator.addStringValidator("username", value -&gt; value != null &amp;&amp; !value.toString().isEmpty());
     * </code></pre>
     *
     * @param path the configuration key to apply the validation to
     * @param validator the validation rule as a String pattern or Predicate function
     */
    public void addStringValidator(String path, Object validator) {
        if (validator instanceof String) {
            addRule(path, new PatternValidationRule((String) validator));
        } else if (validator instanceof Predicate<?>) {
            @SuppressWarnings("unchecked")
            Predicate<Object> predicate = (Predicate<Object>) validator;
            addRule(path, new PredicateValidationRule(predicate, "String validation failed"));
        } else {
            throw new IllegalArgumentException("Validator must be a String pattern or Predicate function");
        }
    }

    public <T> void addValidator(String path, Class<T> type, Validator<T> validator) {
        addRule(path, new TypedValidationRule<>(type, validator));
    }

    public void addPatternValidator(String path, String pattern) {
        addRule(path, new PatternValidationRule(pattern));
    }

    public void addPatternValidator(String path, String pattern, String errorMessage) {
        addRule(path, new PatternValidationRule(pattern, errorMessage));
    }

    @Override
    protected Map<String, Object> getAdditionalModuleState() {
        return Map.of();
    }

    public void validateAllValues() {
        if (config == null) return;
        lastErrors.clear();
        for (String key : rules.keySet()) {
            if (config.containsKey(key)) {
                validateValue(key, config.get(key));
            }
        }
    }

    /**
     * <p>Gets the total number of configuration keys that have validation rules.</p>
     *
     * <pre><code>
     * int validatedKeys = validator.getValidatedKeyCount();
     * System.out.println("Validating " + validatedKeys + " configuration keys");
     * </code></pre>
     *
     * @return the number of keys with at least one validation rule
     */
    public int getValidatedKeyCount() {
        return (int) rules.values().stream()
                .filter(list -> !list.isEmpty())
                .count();
    }

    /**
     * <p>Gets all configuration keys that currently have validation rules registered.</p>
     *
     * <pre><code>
     * List&lt;String&gt; validatedKeys = validator.getValidatedKeys();
     * for (String key : validatedKeys) {
     *     System.out.println("Key with validation: " + key);
     * }
     * </code></pre>
     *
     * @return a list of all keys that have validation rules
     */
    public List<String> getValidatedKeys() {
        return new ArrayList<>(rules.keySet());
    }

    public boolean hasError(String password) {
        return lastErrors.containsKey(password);
    }

    public Map<String, List<ValidationRule>> getAllRules() {
        return new HashMap<>(rules);
    }

    /**
     * <p>Interface for custom validation rules that can be applied to configuration values.</p>
     */
    public interface ValidationRule {
        /**
         * <p>Validates the provided value and returns an error message if validation fails.</p>
         *
         * @param value the value to validate
         * @return null if validation passes, error message if validation fails
         */
        String validate(Object value);
    }

    public static class TypeValidationRule implements ValidationRule {
        private final Class<?> expectedType;

        public TypeValidationRule(Class<?> expectedType) {
            this.expectedType = expectedType;
        }

        @Override
        public String validate(Object value) {
            if (value != null && !expectedType.isInstance(value)) {
                return "Expected type " + expectedType.getSimpleName() +
                        " but got " + value.getClass().getSimpleName();
            }
            return null;
        }
    }

    public static class RangeValidationRule implements ValidationRule {
        private final double min;
        private final double max;

        public RangeValidationRule(double min, double max) {
            this.min = min;
            this.max = max;
        }

        @Override
        public String validate(Object value) {
            if (value instanceof Number) {
                double numValue = ((Number) value).doubleValue();
                if (numValue < min || numValue > max) {
                    return "Value must be between " + min + " and " + max;
                }
            } else {
                return "Value must be a number";
            }
            return null;
        }
    }

    public static class PatternValidationRule implements ValidationRule {
        private final String pattern;
        private final String errorMessage;

        public PatternValidationRule(String pattern) {
            this(pattern, "Value must match pattern: " + pattern);
        }

        public PatternValidationRule(String pattern, String errorMessage) {
            this.pattern = pattern;
            this.errorMessage = errorMessage;
        }

        @Override
        public String validate(Object value) {
            if (value instanceof String) {
                if (!((String) value).matches(pattern)) {
                    return errorMessage;
                }
            } else {
                return "Value must be a string";
            }
            return null;
        }
    }

    public static class PredicateValidationRule implements ValidationRule {
        private final Predicate<Object> predicate;
        private final String errorMessage;

        public PredicateValidationRule(Predicate<Object> predicate, String errorMessage) {
            this.predicate = predicate;
            this.errorMessage = errorMessage;
        }

        @Override
        public String validate(Object value) {
            if (!predicate.test(value)) {
                return errorMessage;
            }
            return null;
        }
    }

    @FunctionalInterface
    public interface Validator<T> {
        boolean validate(T value);

        default String getErrorMessage() {
            return "Validation failed";
        }
    }

    public static class TypedValidationRule<T> implements ValidationRule {
        private final Class<T> type;
        private final Validator<T> validator;

        public TypedValidationRule(Class<T> type, Validator<T> validator) {
            this.type = type;
            this.validator = validator;
        }

        @Override
        public String validate(Object value) {
            if (value != null && !type.isInstance(value)) {
                return "Expected type " + type.getSimpleName() +
                        " but got " + value.getClass().getSimpleName();
            }
            if (!validator.validate(type.cast(value))) {
                return validator.getErrorMessage();
            }
            return null;
        }
    }
}
