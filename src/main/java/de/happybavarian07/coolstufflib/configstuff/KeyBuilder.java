package de.happybavarian07.coolstufflib.configstuff;

import org.bukkit.configuration.file.FileConfiguration;

/**
 * <p>Utility class for building and manipulating hierarchical configuration keys with intelligent
 * parsing capabilities. Provides methods for constructing dot-separated key paths, validating
 * key relationships, and managing configuration section hierarchies.</p>
 *
 * <p>Key features include:</p>
 * <ul>
 * <li>Dynamic key path construction from configuration lines</li>
 * <li>Automatic validation against existing configuration structure</li>
 * <li>Support for custom separators and indentation handling</li>
 * <li>Key relationship analysis (parent/child detection)</li>
 * <li>Configuration section type checking</li>
 * </ul>
 *
 * <pre><code>
 * KeyBuilder builder = new KeyBuilder(config, '.');
 * builder.parseLine("database:");
 * builder.parseLine("  host: localhost");
 * String fullKey = builder.toString(); // "database.host"
 * </code></pre>
 */
public class KeyBuilder implements Cloneable {

    private final FileConfiguration config;
    private final char separator;
    private final StringBuilder builder;

    public KeyBuilder(FileConfiguration config, char separator) {
        this.config = config;
        this.separator = separator;
        this.builder = new StringBuilder();
    }

    private KeyBuilder(KeyBuilder keyBuilder) {
        this.config = keyBuilder.config;
        this.separator = keyBuilder.separator;
        this.builder = new StringBuilder(keyBuilder.toString());
    }

    /**
     * <p>Parses a configuration line and updates the internal key path based on the line's
     * structure and indentation. Automatically validates the resulting path against the
     * configuration and removes invalid segments.</p>
     *
     * <p>The parsing process:</p>
     * <ul>
     * <li>Extracts the key name from the line (before the colon)</li>
     * <li>Validates the complete path against the configuration</li>
     * <li>Removes invalid parent keys if path doesn't exist</li>
     * <li>Appends the new key to build the complete path</li>
     * </ul>
     *
     * <pre><code>
     * builder.parseLine("server:");
     * builder.parseLine("  port: 8080");
     * // Builder now contains "server.port"
     * </code></pre>
     *
     * @param line the configuration line to parse (format: "key: value" or "key:")
     */
    public void parseLine(String line) {
        line = line.trim();
        String[] currentSplitLine = line.split(":");

        //Checks keyBuilder path against config to see if the path is valid.
        //If the path doesn't exist in the config it keeps removing last key in keyBuilder.
        while (builder.length() > 0 && !config.contains(builder.toString() + separator + currentSplitLine[0])) {
            removeLastKey();
        }

        //Add the separator if there is already a key inside keyBuilder
        //If currentSplitLine[0] is 'key2' and keyBuilder contains 'key1' the result will be 'key1.' if '.' is the separator
        if (builder.length() > 0)
            builder.append(separator);

        //Appends the current key to keyBuilder
        //If keyBuilder is 'key1.' and currentSplitLine[0] is 'key2' the resulting keyBuilder will be 'key1.key2' if separator is '.'
        builder.append(currentSplitLine[0]);
    }

    public String getLastKey() {
        if (builder.length() == 0)
            return "";

        return builder.toString().split("[" + separator + "]")[0];
    }

    /**
     * <p>Checks whether the key builder currently contains any key path data.</p>
     *
     * <pre><code>
     * KeyBuilder builder = new KeyBuilder(config, '.');
     * boolean empty = builder.isEmpty(); // true
     * builder.parseLine("key: value");
     * empty = builder.isEmpty(); // false
     * </code></pre>
     *
     * @return true if no key path has been built, false otherwise
     */
    public boolean isEmpty() {
        return builder.length() == 0;
    }

    /**
     * <p>Determines whether the current key path is a sub-key of the specified parent key.
     * A sub-key relationship exists when the current path starts with the parent key
     * followed by the separator character.</p>
     *
     * <pre><code>
     * builder.parseLine("database.connection.host");
     * boolean isSubKey = builder.isSubKeyOf("database.connection"); // true
     * isSubKey = builder.isSubKeyOf("server"); // false
     * </code></pre>
     *
     * @param parentKey the potential parent key to check against
     * @return true if current key path is a sub-key of parentKey, false otherwise
     */
    public boolean isSubKeyOf(String parentKey) {
        return isSubKeyOf(parentKey, builder.toString(), separator);
    }

    /**
     * <p>Determines whether the specified key is a sub-key of the current key path.
     * This is the inverse operation of {@link #isSubKeyOf(String)}.</p>
     *
     * <pre><code>
     * builder.parseLine("database.connection");
     * boolean hasSubKey = builder.isSubKey("database.connection.host"); // true
     * hasSubKey = builder.isSubKey("server.port"); // false
     * </code></pre>
     *
     * @param subKey the potential sub-key to check
     * @return true if subKey is a sub-key of current path, false otherwise
     */
    public boolean isSubKey(String subKey) {
        return isSubKeyOf(builder.toString(), subKey, separator);
    }

    /**
     * <p>Static utility method to determine parent-child key relationships using a custom
     * separator. Checks if the sub-key starts with the parent key followed by the separator.</p>
     *
     * <pre><code>
     * boolean isChild = KeyBuilder.isSubKeyOf("app", "app.database.host", '.');
     * // Returns true - "app.database.host" is a sub-key of "app"
     *
     * boolean isChild2 = KeyBuilder.isSubKeyOf("app", "application.port", '.');
     * // Returns false - "application.port" is not a sub-key of "app"
     * </code></pre>
     *
     * @param parentKey the parent key to test against
     * @param subKey the potential sub-key to test
     * @param separator the character used to separate key segments
     * @return true if subKey is a sub-key of parentKey, false otherwise
     */
    public static boolean isSubKeyOf(String parentKey, String subKey, char separator) {
        if (parentKey.isEmpty())
            return false;

        return subKey.startsWith(parentKey)
                && subKey.substring(parentKey.length()).startsWith(String.valueOf(separator));
    }

    /**
     * <p>Generates appropriate indentation string for a configuration key based on its
     * hierarchical depth. Each level of nesting adds two spaces of indentation.</p>
     *
     * <pre><code>
     * String indent = KeyBuilder.getIndents("server.database.host", '.');
     * // Returns "    " (4 spaces for depth 2: server -> database -> host)
     *
     * String indent2 = KeyBuilder.getIndents("port", '.');
     * // Returns "" (no indentation for root level)
     * </code></pre>
     *
     * @param key the configuration key to analyze
     * @param separator the character used to separate key segments
     * @return indentation string with 2 spaces per nesting level
     */
    public static String getIndents(String key, char separator) {
        String[] splitKey = key.split("[" + separator + "]");
        StringBuilder builder = new StringBuilder();

        for (int i = 1; i < splitKey.length; i++) {
            builder.append("  ");
        }

        return builder.toString();
    }

    /**
     * <p>Checks whether the current key path represents a configuration section
     * in the associated configuration file.</p>
     *
     * <pre><code>
     * builder.parseLine("database");
     * boolean isSection = builder.isConfigSection();
     * // Returns true if "database" is a section with sub-keys
     * </code></pre>
     *
     * @return true if current path is a configuration section, false otherwise
     */
    public boolean isConfigSection() {
        String key = builder.toString();
        return config.isConfigurationSection(key);
    }

    /**
     * <p>Checks whether the current key path represents a configuration section that
     * contains at least one sub-key. This is useful for distinguishing between
     * empty sections and sections with actual content.</p>
     *
     * <pre><code>
     * builder.parseLine("database");
     * boolean hasKeys = builder.isConfigSectionWithKeys();
     * // Returns true only if "database" section contains sub-keys
     * </code></pre>
     *
     * @return true if current path is a non-empty configuration section, false otherwise
     */
    public boolean isConfigSectionWithKeys() {
        String key = builder.toString();
        return config.isConfigurationSection(key) && !config.getConfigurationSection(key).getKeys(false).isEmpty();
    }

    /**
     * <p>Removes the last key segment from the current path, effectively moving up
     * one level in the key hierarchy. If the path contains only one segment,
     * the builder becomes empty.</p>
     *
     * <pre><code>
     * builder.parseLine("server.database.host");
     * builder.removeLastKey(); // Path becomes "server.database"
     * builder.removeLastKey(); // Path becomes "server"
     * builder.removeLastKey(); // Path becomes empty
     * </code></pre>
     */
    public void removeLastKey() {
        if (builder.length() == 0)
            return;

        String keyString = builder.toString();
        String[] split = keyString.split("[" + separator + "]");
        int minIndex = Math.max(0, builder.length() - split[split.length - 1].length() - 1);
        builder.replace(minIndex, builder.length(), "");
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    /**
     * <p>Creates a deep copy of this KeyBuilder instance, including its current
     * key path state and configuration reference.</p>
     *
     * <pre><code>
     * KeyBuilder original = new KeyBuilder(config, '.');
     * original.parseLine("database.host");
     * KeyBuilder copy = original.clone();
     * // copy contains the same path and can be modified independently
     * </code></pre>
     *
     * @return a new KeyBuilder instance with identical state
     * @throws CloneNotSupportedException if cloning is not supported
     */
    @Override
    protected KeyBuilder clone() throws CloneNotSupportedException {
        return (KeyBuilder) super.clone();
    }
}
