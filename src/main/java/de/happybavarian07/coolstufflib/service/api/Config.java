package de.happybavarian07.coolstufflib.service.api;

/**
 * Configuration interface for retrieving various types of configuration values
 * by their corresponding keys.
 */
public interface Config {
    /**
     * <p>Gets a string value for the given key.</p>
     * <pre><code>String value = config.getString("key");</code></pre>
     *
     * @param key the config key
     * @return the string value
     */
    String getString(String key);

    /**
     * <p>Gets an integer value for the given key.</p>
     * <pre><code>int value = config.getInt("key");</code></pre>
     *
     * @param key the config key
     * @return the integer value
     */
    int getInt(String key);

    /**
     * <p>Gets a boolean value for the given key.</p>
     * <pre><code>boolean value = config.getBoolean("key");</code></pre>
     *
     * @param key the config key
     * @return the boolean value
     */
    boolean getBoolean(String key);

    /**
     * <p>Gets an object value for the given key.</p>
     * <pre><code>Object value = config.get("key");</code></pre>
     *
     * @param key the config key
     * @return the object value
     */
    Object get(String key);
}
