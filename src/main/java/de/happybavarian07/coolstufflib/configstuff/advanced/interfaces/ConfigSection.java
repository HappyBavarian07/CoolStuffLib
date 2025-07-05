package de.happybavarian07.coolstufflib.configstuff.advanced.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.section.value.SectionValueStore;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * <p>Represents a section of configuration that can contain values and nested sections.
 * ConfigSection provides hierarchical storage with strong typing support, path-based
 * access, and metadata capabilities.</p>
 *
 * <pre><code>
 * ConfigSection config = ...;
 * config.set("server.port", 8080);
 * config.createSection("database").set("url", "jdbc:mysql://localhost:3306/mydb");
 * </code></pre>
 */
public interface ConfigSection extends Cloneable, Serializable {
    /**
     * <p>Gets the name of this section.</p>
     *
     * @return The section name
     */
    String getName();

    /**
     * <p>Gets the full path of this section within the configuration hierarchy.</p>
     *
     * <pre><code>
     * ConfigSection parent = ...;
     * ConfigSection child = parent.createSection("database");
     * child.getFullPath(); // Returns "parent.database"
     * </code></pre>
     *
     * @return The full path from root to this section using dot notation
     */
    String getFullPath();

    /**
     * <p>Gets the parent section of this section.</p>
     *
     * @return The parent section, or null if this is a root section
     */
    ConfigSection getParent();

    /**
     * <p>Gets a map of all direct subsections within this section.</p>
     *
     * @return An unmodifiable map of subsections with their names as keys
     */
    Map<String, ConfigSection> getSubSections();

    /**
     * <p>Gets a section at the specified path.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * ConfigSection dbSection = config.getSection("database");
     * ConfigSection tablesSection = config.getSection("database.tables");
     * </code></pre>
     *
     * @param path The path to the section, using dot notation for nesting
     * @return The section at the specified path, or null if not found
     */
    ConfigSection getSection(String path);

    /**
     * <p>Creates a section at the specified path, creating parent sections as needed.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * ConfigSection dbSection = config.createSection("database.settings");
     * // Both "database" and "database.settings" sections now exist
     * </code></pre>
     *
     * @param path The path where the section should be created
     * @return The newly created or existing section
     */
    ConfigSection createSection(String path);

    /**
     * <p>Creates a custom section of the specified class at the given path, creating parent sections as needed.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * ConfigSection customSection = config.createCustomSection("myCustomSection", MyCustomSectionClass.class);
     * </code></pre>
     *
     * @param name The name of the custom section
     * @param clazz The class of the custom section, must extend ConfigSection
     * @return The newly created custom section
     */
    <T extends ConfigSection> T createCustomSection(String name, Class<T> clazz);

    /**
     * <p>Checks if a section exists at the specified path.</p>
     *
     * @param path The path to check
     * @return True if a section exists at the path, false otherwise
     */
    boolean hasSection(String path);

    /**
     * <p>Removes the section at the specified path.</p>
     *
     * @param path The path to the section to remove
     */
    void removeSection(String path);

    /**
     * <p>Gets a typed value from the specified path.</p>
     *
     * @param path The path to the value
     * @param type The expected type of the value
     * @param <T> The type parameter
     * @return The value cast to the specified type, or null if not found or not convertible
     */
    <T> T getValue(String path, Class<T> type);

    /**
     * <p>Gets a typed value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @param type The expected type of the value
     * @param <T> The type parameter
     * @return The value cast to the specified type, or the default value if not found
     */
    <T> T getValue(String path, T defaultValue, Class<T> type);

    /**
     * <p>Gets an Optional containing the typed value if it exists.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * Optional&lt;Integer&gt; port = config.getOptionalValue("server.port", Integer.class);
     * port.ifPresent(p -> startServer(p));
     * </code></pre>
     *
     * @param path The path to the value
     * @param type The expected type of the value
     * @param <T> The type parameter
     * @return An Optional containing the value if it exists and can be converted
     */
    <T> Optional<T> getOptionalValue(String path, Class<T> type);

    /**
     * <p>Gets a raw object value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The raw object value, or null if not found
     */
    Object get(String path);

    /**
     * <p>Gets a raw object value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The raw object value, or the default value if not found
     */
    Object get(String path, Object defaultValue);

    /**
     * <p>Gets a String value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The String value, or null if not found
     */
    String getString(String path);

    /**
     * <p>Gets a String value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The String value, or the default value if not found
     */
    String getString(String path, String defaultValue);

    /**
     * <p>Gets a boolean value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The boolean value, or false if not found
     */
    boolean getBoolean(String path);

    /**
     * <p>Gets a boolean value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The boolean value, or the default value if not found
     */
    boolean getBoolean(String path, boolean defaultValue);

    /**
     * <p>Gets an int value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The int value, or 0 if not found
     */
    int getInt(String path);

    /**
     * <p>Gets an int value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The int value, or the default value if not found
     */
    int getInt(String path, int defaultValue);

    /**
     * <p>Gets a long value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The long value, or 0 if not found
     */
    long getLong(String path);

    /**
     * <p>Gets a long value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The long value, or the default value if not found
     */
    long getLong(String path, long defaultValue);

    /**
     * <p>Gets a double value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The double value, or 0.0 if not found
     */
    double getDouble(String path);

    /**
     * <p>Gets a double value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The double value, or the default value if not found
     */
    double getDouble(String path, double defaultValue);

    /**
     * <p>Gets a List value from the specified path.</p>
     *
     * @param path The path to the value
     * @return The List value, or null if not found
     */
    List<?> getList(String path);

    /**
     * <p>Gets a List value from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The List value, or the default value if not found
     */
    List<?> getList(String path, List<?> defaultValue);

    /**
     * <p>Gets a List of Strings from the specified path.</p>
     *
     * @param path The path to the value
     * @return The List of Strings, or null if not found
     */
    List<String> getStringList(String path);

    /**
     * <p>Gets a List of Strings from the specified path, with a default value if not found.</p>
     *
     * @param path The path to the value
     * @param defaultValue The default value to return if the path doesn't exist
     * @return The List of Strings, or the default value if not found
     */
    List<String> getStringList(String path, List<String> defaultValue);

    /**
     * <p>Sets a value at the specified path.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * config.set("server.host", "localhost");
     * config.set("server.port", 8080);
     * </code></pre>
     *
     * @param path The path where the value should be set
     * @param value The value to set
     */
    void set(String path, Object value);

    /**
     * <p>Removes a value at the specified path.</p>
     *
     * @param path The path to the value to remove
     */
    void remove(String path);

    /**
     * <p>Checks if a value exists at the specified path.</p>
     *
     * @param path The path to check
     * @return True if a value exists at the path, false otherwise
     */
    boolean contains(String path);

    /**
     * <p>Gets the set of keys in this section.</p>
     *
     * @param deep If true, returns all keys recursively from subsections using dot notation
     * @return A set of keys
     */
    Set<String> getKeys(boolean deep);

    /**
     * <p>Gets a map of all values in this section.</p>
     *
     * @param deep If true, returns values recursively from subsections using dot notation
     * @return A map of values with their paths as keys
     */
    Map<String, Object> getValues(boolean deep);

    /**
     * <p>Converts this section to a regular Map structure.</p>
     *
     * @return A map representation of this section
     */
    Map<String, Object> toMap();

    /**
     * <p>Converts this section to a serialization-specific Map structure.</p>
     *
     * @return A serialization-specific map representation of this section
     */
    Map<String, Object> toSerializableMap();

    /**
     * <p>Clears all values and subsections from this section.</p>
     */
    void clear();

    /**
     * <p>Merges another section into this one.</p>
     *
     * <p>Values from the other section will override existing values with the same path.
     * Subsections will be merged recursively.</p>
     *
     * @param other The section to merge into this one
     */
    void merge(ConfigSection other);

    /**
     * <p>Adds metadata to this section.</p>
     *
     * <p>Metadata is additional information that doesn't affect the actual configuration values
     * but can be useful for tracking, versioning, etc.</p>
     *
     * @param key The metadata key
     * @param value The metadata value
     */
    void addMetadata(String key, Object value);

    /**
     * <p>Gets typed metadata from this section.</p>
     *
     * @param key The metadata key
     * @param type The expected type of the metadata
     * @param <T> The type parameter
     * @return The metadata value cast to the specified type, or null if not found
     */
    <T> T getMetadata(String key, Class<T> type);

    /**
     * <p>Gets all metadata for this section.</p>
     *
     * @return A map of all metadata
     */
    Map<String, Object> getMetadata();

    /**
     * <p>Checks if metadata exists for the specified key.</p>
     *
     * @param key The metadata key to check
     * @return True if metadata exists for the key, false otherwise
     */
    boolean hasMetadata(String key);

    /**
     * <p>Removes metadata with the specified key.</p>
     *
     * @param key The metadata key to remove
     */
    void removeMetadata(String key);

    /**
     * <p>Clears all metadata from this section.</p>
     */
    void clearMetadata();

    /**
     * <p>Sets a comment for a configuration entry at the specified path.</p>
     *
     * <pre><code>
     * ConfigSection config = ...;
     * config.set("server.port", 8080);
     * config.setComment("server.port", "The port on which the server will listen");
     * </code></pre>
     *
     * @param path The path to associate the comment with
     * @param comment The comment text
     */
    void setComment(String path, String comment);

    /**
     * <p>Gets the comment associated with a configuration entry.</p>
     *
     * @param path The path to get the comment for
     * @return The comment, or null if no comment exists
     */
    String getComment(String path);

    /**
     * <p>Checks if a comment exists for the specified path.</p>
     *
     * @param path The path to check
     * @return True if a comment exists for the path, false otherwise
     */
    boolean hasComment(String path);

    /**
     * <p>Removes a comment from the specified path.</p>
     *
     * @param path The path to remove the comment from
     */
    void removeComment(String path);

    /**
     * <p>Gets all comments in this section and subsections.</p>
     *
     * @return A map of comments with their paths as keys
     */
    Map<String, String> getComments();

    /**
     * <p>Clears all comments from this section and subsections.</p>
     */
    void clearComments();

    /**
     * <p>Validates the content of this section.</p>
     *
     * <p>This method can be implemented by specific section types to ensure
     * the data contained is valid according to certain rules.</p>
     */
    void validate();

    /**
     * <p>Copies values from another ConfigSection into this one.</p>
     *
     * <p>This method will overwrite existing values with the same path.
     * Subsections will be copied recursively.</p>
     *
     * @param rootSection The section to copy values from
     */
    void copyFrom(ConfigSection rootSection);

    /**
     * <p>Converts this section to a List structure.</p>
     *
     * @return A List representation of this section
     */
    List<Object> toList();

    /**
     * <p>Converts this section to a Set structure.</p>
     *
     * @return A Set representation of this section
     */
    Set<Object> toSet();

    int size();

    SectionValueStore getValueStore();

    void fromMap(Map<String, Object> values);

    void setParent(ConfigSection owner);
}
