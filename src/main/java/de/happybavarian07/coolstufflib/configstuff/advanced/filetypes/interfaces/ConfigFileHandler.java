package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * <p>Interface for handling serialization and deserialization of configuration data
 * to and from different file formats such as YAML, JSON, Properties, TOML, etc.</p>
 *
 * <p>File handlers provide format-specific operations including:</p>
 * <ul>
 *   <li>Loading configuration data from files</li>
 *   <li>Saving configuration data to files</li>
 *   <li>Comment preservation for supported formats</li>
 *   <li>File type detection and validation</li>
 * </ul>
 *
 * <pre><code>
 * ConfigFileHandler handler = new YamlConfigFileHandler();
 * Map&lt;String, Object&gt; data = handler.load(new File("config.yml"));
 *
 * data.put("server.port", 8080);
 * handler.save(new File("config.yml"), data);
 * </code></pre>
 */
public interface ConfigFileHandler {

    /**
     * <p>Saves configuration data to a file without comments.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; config = Map.of(
     *     "server.port", 8080,
     *     "database.url", "jdbc:mysql://localhost/db"
     * );
     * handler.save(new File("config.yml"), config);
     * </code></pre>
     *
     * @param file the target file to save to
     * @param data the configuration data to save
     * @throws IOException if file operations fail
     */
    void save(File file, Map<String, Object> data) throws IOException;

    /**
     * <p>Saves configuration data to a file with comments for supported formats.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; config = Map.of("server.port", 8080);
     * Map&lt;String, String&gt; comments = Map.of("server.port", "Web server port");
     * handler.save(new File("config.yml"), config, comments);
     * </code></pre>
     *
     * @param file the target file to save to
     * @param data the configuration data to save
     * @param comments the comments to include with the data
     * @throws IOException if file operations fail
     */
    void save(File file, Map<String, Object> data, Map<String, String> comments) throws IOException;

    /**
     * <p>Loads configuration data from a file.</p>
     *
     * <pre><code>
     * Map&lt;String, Object&gt; config = handler.load(new File("config.yml"));
     * Integer port = (Integer) config.get("server.port");
     * </code></pre>
     *
     * @param file the source file to load from
     * @return the loaded configuration data
     * @throws IOException if file operations fail
     */
    Map<String, Object> load(File file) throws IOException;

    /**
     * <p>Loads comments from a file for formats that support comment preservation.</p>
     *
     * <pre><code>
     * Map&lt;String, String&gt; comments = handler.loadComments(new File("config.yml"));
     * String portComment = comments.get("server.port");
     * </code></pre>
     *
     * @param file the source file to load comments from
     * @return map of configuration paths to their comments
     * @throws IOException if file operations fail
     */
    Map<String, String> loadComments(File file) throws IOException;

    /**
     * <p>Indicates whether this handler supports comment preservation.</p>
     *
     * <pre><code>
     * if (handler.supportsComments()) {
     *     Map&lt;String, String&gt; comments = handler.loadComments(file);
     * }
     * </code></pre>
     *
     * @return true if comments are supported, false otherwise
     */
    boolean supportsComments();

    /**
     * <p>Determines if this handler can process the given file based on its extension and format.</p>
     *
     * <pre><code>
     * if (yamlHandler.canHandle(new File("config.yml"))) {
     *     Map&lt;String, Object&gt; data = yamlHandler.load(file);
     * }
     * </code></pre>
     *
     * @param file the file to check compatibility with
     * @return true if this handler can process the file, false otherwise
     */
    boolean canHandle(File file);

    /**
     * <p>Gets the file extension pattern that this handler supports.</p>
     *
     * <pre><code>
     * String pattern = handler.getFileExtension();
     * // For YAML handler: ".*\.(yml|yaml)$"
     * </code></pre>
     *
     * @return the regex pattern for supported file extensions
     */
    String getFileExtension();

    /**
     * <p>Indicates whether this handler has been used to load or save a file.</p>
     *
     * <pre><code>
     * if (handler.wasCalled()) {
     *     // Handler was used
     * }
     * </code></pre>
     *
     * @return true if load or save was called, false otherwise
     */
    boolean wasCalled();
}
