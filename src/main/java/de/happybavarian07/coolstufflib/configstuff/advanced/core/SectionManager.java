package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigEventBus;
import de.happybavarian07.coolstufflib.configstuff.advanced.event.ConfigSectionEvent;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.internal.SectionKind;

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
     * Creates a configuration section at the given path with DEFAULT kind.
     *
     * @param path  the dot-separated path where the section should be created
     * @return the created or existing section
     */
    public ConfigSection createSection(String path, AdvancedConfig config) {
        return createSection(path, SectionKind.DEFAULT, config);
    }

    /**
     * Creates a configuration section with explicit type at the given path.
     *
     * <pre><code>
     * manager.createSection("inventory.items", SectionKind.LIST, config);
     * </code></pre>
     *
     * @param path  the dot-separated path for the new section
     * @param kind  the type of section to create (DEFAULT, MAP, LIST, SET)
     * @param config the configuration context for events
     * @return the created or existing section
     */
    public <T extends ConfigSection> T createSection(String path, SectionKind kind, AdvancedConfig config) {
        T section = rootSection.createSection(path, kind, true);
        return publishEventAndReturnT(section, path, config);
    }

    private <T extends ConfigSection> T publishEventAndReturnT(T section, String path, AdvancedConfig config) {
        String[] parts = path.split("\\.");
        String sectionName = parts[parts.length - 1];
        ConfigSection parentSection = parts.length > 1 ? rootSection.getSection(String.join(".", java.util.Arrays.copyOf(parts, parts.length - 1))) : rootSection;
        eventBus.publish(ConfigSectionEvent.sectionCreated(config, parentSection, sectionName, section));
        return section;
    }
}
