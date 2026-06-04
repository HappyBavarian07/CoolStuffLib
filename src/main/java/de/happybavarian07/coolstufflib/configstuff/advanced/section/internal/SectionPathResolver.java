package de.happybavarian07.coolstufflib.configstuff.advanced.section.internal;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.BaseConfigSection;

/**
 * Helper class for parsing section paths and resolving parent sections.
 * Separates path manipulation logic from the main ConfigSection classes.
 */
public final class SectionPathResolver {
    
    private SectionPathResolver() {
        // Utility class - no instantiation
    }

    /**
     * Parses a dotted path into [parentPath, sectionName].
     *
     * Examples:
     * "database" → ["", "database"]  (direct child)
     * "db.connection" → ["db", "connection"]
     * "a.b.c.d" → ["a.b.c", "d"]
     *
     * @param path The dotted path to parse
     * @return Array of exactly 2 strings: [parentPath, sectionName]
     */
    public static String[] parsePath(String path) {
        if (path == null || path.isEmpty()) {
            throw new IllegalArgumentException("Path must not be null or empty");
        }
        
        int lastDot = path.lastIndexOf('.');
        if (lastDot < 0) {
            return new String[]{"", path};  // direct child no parent
        }
        return new String[]{path.substring(0, lastDot), path.substring(lastDot + 1)};
    }

    /**
     * Gets or creates the parent section for a given child path.
     * Creates intermediate sections with DEFAULT kind as needed.
     *
     * @param root The root section to start from
     * @param childPath The full path of the child (e.g., "a.b.c")
     * @return The parent section for the final component of childPath
     */
    public static ConfigSection resolveParent(ConfigSection root, String childPath) {
        String[] parsed = parsePath(childPath);
        if (parsed[0].isEmpty()) {
            return root;  // Direct child this should be the parent
        }

        // navigate/create parent chain
        String[] parentParts = parsed[0].split("\\.");
        ConfigSection current = root;
        
        for (String part : parentParts) {
            if (current instanceof BaseConfigSection baseCurrent) {
                ConfigSection child = baseCurrent.getMutableSubSections().get(part);
                if (child == null) {
                    // Create intermediate section with default kind
                    child = new BaseConfigSection(part);
                    attachSection(baseCurrent, part, child);
                }
                current = child;
            } else {
                // Fallback for non-BaseConfigSection implementations
                ConfigSection child = current.getSection(part);
                if (child != null) {
                    current = child;
                } else {
                    throw new IllegalStateException(
                        "Cannot navigate through non-BaseConfigSection: " + current.getClass().getName()
                    );
                }
            }
        }
        return current;
    }

    /**
     * Attaches a section to its parent in the hierarchy.
     *
     * @param parent The parent section
     * @param name The name/key for this child
     * @param section The child section to attach
     */
    public static void attachSection(ConfigSection parent, String name, ConfigSection section) {
        if (!(parent instanceof BaseConfigSection baseParent)) {
            throw new IllegalArgumentException("Parent must be a BaseConfigSection: " + 
                parent.getClass().getName());
        }

        // Set parent reference on child
        if (section instanceof BaseConfigSection baseChild) {
            baseChild.setParent(parent);
        }

        // Register in hierarchy manager
        baseParent.getMutableSubSections().put(name, section);
    }

    /**
     * Gets just the final component name from a path.
     * "a.b.c" → "c"
     *
     * @param path The path to extract the name from
     * @return The last component after the final dot
     */
    public static String getSectionName(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(lastDot + 1) : path;
    }

    /**
     * Gets just the parent path from a full path.
     * "a.b.c" → "a.b"
     * "c" → ""
     *
     * @param path The full path
     * @return The parent path (empty string if direct child)
     */
    public static String getParentPath(String path) {
        int lastDot = path.lastIndexOf('.');
        return lastDot >= 0 ? path.substring(0, lastDot) : "";
    }
}