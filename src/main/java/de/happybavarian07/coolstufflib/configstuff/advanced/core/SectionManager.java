package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigSectionEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Manager for configuration sections that provides hierarchical organization
 * of configuration data with event-driven section lifecycle management.
 * Handles creation, retrieval, and removal of configuration sections.</p>
 *
 * <p>This manager provides:</p>
 * <ul>
 * <li>Hierarchical section management with root section access</li>
 * <li>Event-driven section creation and removal notifications</li>
 * <li>Custom section creation with flexible implementations</li>
 * <li>Section existence checking and key enumeration</li>
 * </ul>
 *
 * <pre><code>
 * SectionManager manager = new SectionManager(rootSection, eventBus);
 * ConfigSection dbSection = manager.createSection("database", config);
 * boolean exists = manager.hasSection("database");
 * </code></pre>
 */
public class SectionManager {
    private final BaseConfigSection rootSection;
    private final ConfigEventBus eventBus;

    public SectionManager(BaseConfigSection rootSection, ConfigEventBus eventBus) {
        this.rootSection = rootSection;
        this.eventBus = eventBus;
    }

    public ConfigSection getRootSection() {
        return rootSection;
    }

    /**
     * <p>Retrieves a configuration section by its path, creating parent sections
     * as needed if they don't exist.</p>
     *
     * <pre><code>
     * ConfigSection section = manager.getSection("database.connection");
     * </code></pre>
     *
     * @param path the section path to retrieve
     * @return the configuration section at the specified path
     */
    public ConfigSection getSection(String path) {
        return rootSection.getSection(path);
    }

    /**
     * <p>Creates a new configuration section at the specified path and publishes
     * a section creation event with the associated configuration context.</p>
     *
     * <pre><code>
     * ConfigSection newSection = manager.createSection("cache.settings", config);
     * </code></pre>
     *
     * @param path the path where the new section should be created
     * @param config the configuration context for the section creation event
     * @return the newly created configuration section
     */
    public ConfigSection createSection(String path, AdvancedConfig config) {
        ConfigSection section = rootSection.createSection(path);
        return publishEventAndReturnT(path, config, section);
    }

    /**
     * <p>Checks whether a configuration section exists at the specified path.</p>
     *
     * <pre><code>
     * if (manager.hasSection("database")) {
     *     ConfigSection dbSection = manager.getSection("database");
     * }
     * </code></pre>
     *
     * @param path the section path to check for existence
     * @return true if a section exists at this path, false otherwise
     */
    public boolean hasSection(String path) {
        return rootSection.hasSection(path);
    }

    /**
     * <p>Removes a configuration section at the specified path and publishes
     * a section removal event with detailed parent/child context.</p>
     *
     * <pre><code>
     * manager.removeSection("deprecated.settings", config);
     * </code></pre>
     *
     * @param path the path of the section to remove
     * @param config the configuration context for the section removal event
     */
    public void removeSection(String path, AdvancedConfig config) {
        ConfigSection section = rootSection.getSection(path);
        if (section != null) {
            ConfigSection parentSection = path.contains(".") ? rootSection.getSection(path.substring(0, path.lastIndexOf('.'))) : rootSection;
            String sectionName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
            rootSection.removeSection(path);
            eventBus.publish(ConfigSectionEvent.sectionRemoved(config, parentSection, sectionName, section));
        }
    }

    /**
     * <p>Retrieves configuration keys from the root section with optional deep traversal
     * for comprehensive key enumeration across nested sections.</p>
     *
     * <pre><code>
     * List&lt;String&gt; allKeys = manager.getKeys(true);
     * List&lt;String&gt; topLevelKeys = manager.getKeys(false);
     * </code></pre>
     *
     * @param deep whether to include keys from nested sections recursively
     * @return a list of configuration keys based on the traversal depth
     */
    public List<String> getKeys(boolean deep) {
        return new ArrayList<>(rootSection.getKeys(deep));
    }

    /**
     * <p>Creates a custom configuration section with a specific type implementation
     * at the given path, allowing for specialized section behavior with type safety.</p>
     *
     * <pre><code>
     * SpecializedSection section = manager.createCustomSection("special",
     *     SpecializedSection.class, config);
     * </code></pre>
     *
     * @param path the path where the custom section should be created
     * @param sectionType the specific section class type to instantiate
     * @param config the configuration context for the section creation event
     * @param <T> the section type parameter extending ConfigSection
     * @return the created custom configuration section of the specified type
     */
    public <T extends ConfigSection> T createCustomSection(String path, Class<T> sectionType, AdvancedConfig config) {
        T section = (T) rootSection.createCustomSection(path, sectionType);
        return publishEventAndReturnT(path, config, section);
    }

    private <T extends ConfigSection> T publishEventAndReturnT(String path, AdvancedConfig config, T section) {
        ConfigSection parentSection = path.contains(".") ? rootSection.getSection(path.substring(0, path.lastIndexOf('.'))) : rootSection;
        String sectionName = path.contains(".") ? path.substring(path.lastIndexOf('.') + 1) : path;
        eventBus.publish(ConfigSectionEvent.sectionCreated(config, parentSection, sectionName, section));
        return section;
    }
}
