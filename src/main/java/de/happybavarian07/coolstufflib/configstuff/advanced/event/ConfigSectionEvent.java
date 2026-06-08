package de.happybavarian07.coolstufflib.configstuff.advanced.event;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;

/**
 * <p>Event fired when a configuration section is created or removed.</p>
 *
 * <p>This event tracks structural changes to the configuration hierarchy by notifying
 * listeners when sections (nested config containers) are added or deleted. Sections provide
 * hierarchical organization of related configuration values.</p>
 *
 * <pre><code>
 * // Subscribe to section events
 * eventBus.subscribe(ConfigSectionEvent.class, event -> {
 *     if (event.getType() == ConfigSectionEvent.Type.CREATED) {
 *         System.out.println("Section created: " + event.getFullPath());
 *         initializeSectionDefaults(event.getSection());
 *     }
 * });
 *
 * // Triggering the event
 * config.createSection("database.connection");
 * </code></pre>
 *
 * <p>Event types:</p>
 * <ul>
 *   <li>{@link Type#CREATED} - A new section was created</li>
 *   <li>{@link Type#REMOVED} - An existing section was deleted</li>
 * </ul>
 *
 * @see ConfigEventBus
 * @see ConfigEventListener
 * @see ConfigSection
 */
public class ConfigSectionEvent extends ConfigEvent {
    private final ConfigSection parentSection;
    private final String path;
    private final ConfigSection section;
    private final Type eventType;

    /**
     * Creates a section event indicating that a new section was created.
     *
     * <pre><code>
 * ConfigSectionEvent event = ConfigSectionEvent.sectionCreated(config, parent,
 *     "database", newSection);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration containing the section
     * @param parentSection the parent section; {@code null} for root-level sections
     * @param path the section name/key within its parent
     * @param section the newly created ConfigSection instance
     * @return a ConfigSectionEvent with Type.CREATED
     */
    public static ConfigSectionEvent sectionCreated(AdvancedConfig config, ConfigSection parentSection,
            String path, ConfigSection section) {
        return new ConfigSectionEvent(Type.CREATED, config, parentSection, path, section);
    }

    /**
     * Creates a section event indicating that an existing section was removed.
     *
     * <pre><code>
 * ConfigSectionEvent event = ConfigSectionEvent.sectionRemoved(config, parent,
 *     "temp", deletedSection);
 * eventBus.publish(event);
 * </code></pre>
     *
     * @param config the configuration from which the section was removed
     * @param parentSection the parent section that contained the deleted section; {@code null} for root-level
     * @param path the section name/key within its parent
     * @param section the ConfigSection instance that was deleted
     * @return a ConfigSectionEvent with Type.REMOVED
     */
    public static ConfigSectionEvent sectionRemoved(AdvancedConfig config, ConfigSection parentSection,
            String path, ConfigSection section) {
        return new ConfigSectionEvent(Type.REMOVED, config, parentSection, path, section);
    }

    private ConfigSectionEvent(Type eventType, AdvancedConfig config, ConfigSection parentSection,
            String path, ConfigSection section) {
        super(config);
        this.eventType = eventType;
        this.parentSection = parentSection;
        this.path = path;
        this.section = section;
    }

    /**
     * Gets the type of operation that triggered this event.
     *
     * @return the Type indicating whether a section was created or removed
     */
    public Type getType() {
        return eventType;
    }

    /**
     * Gets the source of this event.
     *
     * <p>Returns the parent section if present, otherwise returns the config.</p>
     *
     * @return the ConfigSection or AdvancedConfig that is the source
     */
    @Override
    public Object getSource() {
        return parentSection != null ? parentSection : getConfig();
    }

    /**
     * Gets the parent section containing the affected section.
     *
     * <pre><code>
 * ConfigSection parent = event.getParentSection();
 * if (parent != null) {
 *     System.out.println("Parent: " + parent.getName());
 * }
 * </code></pre>
     *
     * @return the parent ConfigSection, or {@code null} for root-level sections
     */
    public ConfigSection getParentSection() {
        return parentSection;
    }

    /**
     * Gets the path (name) of the affected section within its parent.
     *
     * <pre><code>
 * String path = event.getPath(); // Returns "database"
 * </code></pre>
     *
     * @return the section key/name relative to its parent
     */
    public String getPath() {
        return path;
    }

    /**
     * Gets the ConfigSection instance affected by this event.
     *
     * <pre><code>
 * ConfigSection section = event.getSection();
 * if (section != null) {
 *     section.set("host", "localhost");
 * }
 * </code></pre>
     *
     * @return the ConfigSection that was created or removed
     */
    public ConfigSection getSection() {
        return section;
    }

    /**
     * Gets the full hierarchical path to the affected section.
     *
     * <pre><code>
 * event.getFullPath(); // Returns "database.connection.pool"
 * </code></pre>
     *
     * <p>The path includes parent hierarchy when applicable, using dot notation.</p>
     *
     * @return the full path including parent prefix if present
     */
    public String getFullPath() {
        return parentSection != null && !parentSection.getFullPath().isEmpty()
                ? parentSection.getFullPath() + "." + path
                : path;
    }

    /**
     * Checks if this event can be cancelled.
     *
     * <p>All section events are cancellable by default, allowing listeners to
     * prevent section creation or removal.</p>
     *
     * @return {@code true} - section events can always be cancelled
     */
    @Override
    public boolean isCancellable() {
        return true; // Section events can be cancelled
    }

    /**
     * Enum representing the types of config section operations.
     *
     * <ul>
     *   <li>{@link #CREATED} - A new section was created in the configuration hierarchy</li>
     *   <li>{@link #REMOVED} - An existing section was deleted from the configuration</li>
     * </ul>
     */
    public enum Type {
        /** A new section was created. The section object is valid and accessible. */
        CREATED,
        /** A section was removed. The section object may be inaccessible after removal. */
        REMOVED
    }
}
