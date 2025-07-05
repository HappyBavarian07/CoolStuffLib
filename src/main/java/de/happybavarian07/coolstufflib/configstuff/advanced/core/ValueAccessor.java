package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigLifecycleEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigValueEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.modules.ModuleManager;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * <p>Core component that provides type-safe value access and manipulation operations
 * for advanced configuration systems. Acts as a centralized facade for all value
 * operations while handling event notifications and type conversions.</p>
 *
 * <p>This accessor provides:</p>
 * <ul>
 * <li>Type-safe getter methods with optional default values</li>
 * <li>Event-driven value change notifications</li>
 * <li>Bulk value operations for efficiency</li>
 * <li>Integration with configuration sections and modules</li>
 * </ul>
 *
 * <pre><code>
 * ValueAccessor accessor = new ValueAccessor(sectionManager, eventBus);
 * String host = accessor.getString("database.host", "localhost");
 * accessor.set("debug.enabled", true, config);
 * </code></pre>
 */
public class ValueAccessor {
    private final SectionManager sectionManager;
    private final ConfigEventBus eventBus;

    public ValueAccessor(SectionManager sectionManager, ConfigEventBus eventBus) {
        this.sectionManager = sectionManager;
        this.eventBus = eventBus;
    }

    /**
     * <p>Retrieves a raw value from the configuration at the specified path.</p>
     *
     * <pre><code>
     * Object value = accessor.get("database.timeout");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the value at the path, or null if not found
     */
    public Object get(String path) {
        return sectionManager.getRootSection().get(path);
    }

    /**
     * <p>Retrieves a raw value with a fallback default if the path doesn't exist.</p>
     *
     * <pre><code>
     * Object timeout = accessor.get("database.timeout", 30);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default value to return if path doesn't exist
     * @return the value at the path, or def if not found
     */
    public Object get(String path, Object def) {
        return sectionManager.getRootSection().get(path, def);
    }

    /**
     * <p>Retrieves a string value from the configuration.</p>
     *
     * <pre><code>
     * String host = accessor.getString("database.host");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the string value at the path, or null if not found or not a string
     */
    public String getString(String path) {
        return sectionManager.getRootSection().getString(path);
    }

    /**
     * <p>Retrieves a string value with a fallback default.</p>
     *
     * <pre><code>
     * String host = accessor.getString("database.host", "localhost");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default string to return if path doesn't exist
     * @return the string value at the path, or def if not found
     */
    public String getString(String path, String def) {
        return sectionManager.getRootSection().getString(path, def);
    }

    /**
     * <p>Retrieves a boolean value from the configuration.</p>
     *
     * <pre><code>
     * boolean enabled = accessor.getBoolean("features.debug");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the boolean value at the path, or false if not found or not boolean
     */
    public boolean getBoolean(String path) {
        return sectionManager.getRootSection().getBoolean(path);
    }

    /**
     * <p>Retrieves a boolean value with a fallback default.</p>
     *
     * <pre><code>
     * boolean debug = accessor.getBoolean("debug.enabled", false);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default boolean to return if path doesn't exist
     * @return the boolean value at the path, or def if not found
     */
    public boolean getBoolean(String path, boolean def) {
        return sectionManager.getRootSection().getBoolean(path, def);
    }

    /**
     * <p>Retrieves an integer value from the configuration.</p>
     *
     * <pre><code>
     * int port = accessor.getInt("server.port");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the integer value at the path, or 0 if not found or not numeric
     */
    public int getInt(String path) {
        return sectionManager.getRootSection().getInt(path);
    }

    /**
     * <p>Retrieves an integer value with a fallback default.</p>
     *
     * <pre><code>
     * int port = accessor.getInt("server.port", 8080);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default integer to return if path doesn't exist
     * @return the integer value at the path, or def if not found
     */
    public int getInt(String path, int def) {
        return sectionManager.getRootSection().getInt(path, def);
    }

    /**
     * <p>Retrieves a long value from the configuration.</p>
     *
     * <pre><code>
     * long maxSize = accessor.getLong("cache.maxSize");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the long value at the path, or 0L if not found or not numeric
     */
    public long getLong(String path) {
        return sectionManager.getRootSection().getLong(path);
    }

    /**
     * <p>Retrieves a long value with a fallback default.</p>
     *
     * <pre><code>
     * long timeout = accessor.getLong("connection.timeout", 5000L);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default long to return if path doesn't exist
     * @return the long value at the path, or def if not found
     */
    public long getLong(String path, long def) {
        return sectionManager.getRootSection().getLong(path, def);
    }

    /**
     * <p>Retrieves a double value from the configuration.</p>
     *
     * <pre><code>
     * double rate = accessor.getDouble("performance.threshold");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the double value at the path, or 0.0 if not found or not numeric
     */
    public double getDouble(String path) {
        return sectionManager.getRootSection().getDouble(path);
    }

    /**
     * <p>Retrieves a double value with a fallback default.</p>
     *
     * <pre><code>
     * double multiplier = accessor.getDouble("scaling.factor", 1.0);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default double to return if path doesn't exist
     * @return the double value at the path, or def if not found
     */
    public double getDouble(String path, double def) {
        return sectionManager.getRootSection().getDouble(path, def);
    }

    /**
     * <p>Retrieves a float value from the configuration.</p>
     *
     * <pre><code>
     * float precision = accessor.getFloat("rendering.precision");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the float value at the path, or 0.0f if not found or not numeric
     */
    public float getFloat(String path) {
        return sectionManager.getRootSection().getValue(path, 0.0f, Float.class);
    }

    /**
     * <p>Retrieves a float value with a fallback default.</p>
     *
     * <pre><code>
     * float quality = accessor.getFloat("audio.quality", 0.8f);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default float to return if path doesn't exist
     * @return the float value at the path, or def if not found
     */
    public float getFloat(String path, float def) {
        return sectionManager.getRootSection().getValue(path, def, Float.class);
    }

    /**
     * <p>Retrieves a list value from the configuration.</p>
     *
     * <pre><code>
     * List&lt;?&gt; items = accessor.getList("features.enabled");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the list value at the path, or null if not found or not a list
     */
    public List<?> getList(String path) {
        return sectionManager.getRootSection().getList(path);
    }

    /**
     * <p>Retrieves a list value with a fallback default.</p>
     *
     * <pre><code>
     * List&lt;?&gt; defaults = Arrays.asList("basic", "core");
     * List&lt;?&gt; modules = accessor.getList("modules.active", defaults);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default list to return if path doesn't exist
     * @return the list value at the path, or def if not found
     */
    public List<?> getList(String path, List<?> def) {
        return sectionManager.getRootSection().getList(path, def);
    }

    /**
     * <p>Retrieves a string list from the configuration.</p>
     *
     * <pre><code>
     * List&lt;String&gt; languages = accessor.getStringList("supported.languages");
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @return the string list at the path, or null if not found or not a string list
     */
    public List<String> getStringList(String path) {
        return sectionManager.getRootSection().getStringList(path);
    }

    /**
     * <p>Retrieves a string list with a fallback default.</p>
     *
     * <pre><code>
     * List&lt;String&gt; defaultLangs = Arrays.asList("en", "es");
     * List&lt;String&gt; langs = accessor.getStringList("languages", defaultLangs);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default string list to return if path doesn't exist
     * @return the string list at the path, or def if not found
     */
    public List<String> getStringList(String path, List<String> def) {
        return sectionManager.getRootSection().getStringList(path, def);
    }

    /**
     * <p>Retrieves a typed value with type safety and default fallback.</p>
     *
     * <pre><code>
     * Integer maxUsers = accessor.get("limits.users", 100, Integer.class);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param def the default value to return if path doesn't exist
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the typed value at the path, or def if not found or wrong type
     */
    public <T> T get(String path, T def, Class<T> type) {
        return sectionManager.getRootSection().getValue(path, def, type);
    }

    /**
     * <p>Retrieves a typed value with strict type checking.</p>
     *
     * <pre><code>
     * Boolean feature = accessor.getValue("features.experimental", Boolean.class);
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param type the expected type class
     * @param <T> the type parameter
     * @return the typed value at the path, or null if not found or wrong type
     */
    public <T> T getValue(String path, Class<T> type) {
        return sectionManager.getRootSection().getValue(path, type);
    }

    /**
     * <p>Retrieves a typed value wrapped in an Optional for null-safe handling.</p>
     *
     * <pre><code>
     * Optional&lt;String&gt; theme = accessor.getOptionalValue("ui.theme", String.class);
     * theme.ifPresent(t -&gt; applyTheme(t));
     * </code></pre>
     *
     * @param path the configuration path to retrieve
     * @param type the expected type class
     * @param <T> the type parameter
     * @return Optional containing the typed value, or empty if not found
     */
    public <T> Optional<T> getOptionalValue(String path, Class<T> type) {
        return sectionManager.getRootSection().getOptionalValue(path, type);
    }

    /**
     * <p>Sets a value at the specified path and publishes change events.</p>
     *
     * <pre><code>
     * accessor.set("database.host", "newhost.example.com", config);
     * </code></pre>
     *
     * @param path the configuration path to set
     * @param value the value to set at the path
     * @param config the configuration instance for event context
     */
    public void set(String path, Object value, AdvancedConfig config) {
        Object oldValue = sectionManager.getRootSection().get(path);
        sectionManager.getRootSection().set(path, value);
        String sectionPath = path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : "";
        String valueName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        ConfigSection section = sectionPath.isEmpty() ? sectionManager.getRootSection() : sectionManager.getRootSection().getSection(sectionPath);
        eventBus.publish(ConfigValueEvent.valueSet(config, section, valueName, oldValue, value));
    }

    /**
     * <p>Sets multiple values efficiently in a single operation with consolidated events.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; updates = new HashMap&lt;&gt;();
     * updates.put("database.host", "newhost");
     * updates.put("database.port", 5432);
     * accessor.setBulk(updates, config);
     * </code></pre>
     *
     * @param values map of paths to values to set
     * @param config the configuration instance for event context
     */
    public void setBulk(Map<String, Object> values, AdvancedConfig config) {
        if (values == null || values.isEmpty()) return;
        for (Map.Entry<String, Object> e : values.entrySet()) set(e.getKey(), e.getValue(), config);
    }

    /**
     * <p>Removes a value at the specified path and publishes removal events.</p>
     *
     * <pre><code>
     * accessor.remove("deprecated.setting", config);
     * </code></pre>
     *
     * @param path the configuration path to remove
     * @param config the configuration instance for event context
     */
    public void remove(String path, AdvancedConfig config) {
        Object oldValue = sectionManager.getRootSection().get(path);
        sectionManager.getRootSection().remove(path);
        String sectionPath = path.contains(".") ? path.substring(0, path.lastIndexOf('.')) : "";
        String valueName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        ConfigSection section = sectionPath.isEmpty() ? sectionManager.getRootSection() : sectionManager.getRootSection().getSection(sectionPath);
        eventBus.publish(ConfigValueEvent.valueRemove(config, section, valueName, oldValue));
    }

    /**
     * <p>Checks whether a configuration path exists.</p>
     *
     * <pre><code>
     * if (accessor.containsKey("features.experimental")) {
     *     // Handle experimental features
     * }
     * </code></pre>
     *
     * @param path the configuration path to check
     * @return true if the path exists, false otherwise
     */
    public boolean containsKey(String path) {
        return sectionManager.getRootSection().contains(path);
    }

    /**
     * <p>Clears all configuration values and publishes lifecycle events.</p>
     *
     * <pre><code>
     * accessor.clear(config);
     * </code></pre>
     *
     * @param config the configuration instance for event context
     */
    public void clear(AdvancedConfig config) {
        sectionManager.getRootSection().clear();
        eventBus.publish(ConfigLifecycleEvent.configClear(config));
    }

    /**
     * <p>Copies all values from another configuration and publishes lifecycle events.</p>
     *
     * <pre><code>
     * accessor.copyFrom(sourceConfig, targetConfig, moduleManager);
     * </code></pre>
     *
     * @param config2 the source configuration to copy from
     * @param config the target configuration instance for event context
     * @param moduleManager the module manager for handling copy operations
     */
    public void copyFrom(AdvancedConfig config2, AdvancedConfig config, ModuleManager moduleManager) {
        sectionManager.getRootSection().merge(config2.getRootSection());
        eventBus.publish(ConfigLifecycleEvent.configCopied(config, config2));
    }
}
