package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import java.util.Map;

/**
 * Interface for config file handlers that support hierarchical (dotted-path) keys.
 */
public interface HierarchicalConfigFileHandler extends ConfigFileHandler {
    /**
     * Set a value in the config map using a dotted path, creating nested maps as needed.
     * @param root The root map (representing the config structure)
     * @param path The dotted path (e.g., "a.b.c")
     * @param value The value to set
     */
    void setValueByPath(Map<String, Object> root, String path, Object value);

    /**
     * Get a value from the config map using a dotted path.
     * @param root The root map (representing the config structure)
     * @param path The dotted path (e.g., "a.b.c")
     * @return The value or null if not found
     */
    Object getValueByPath(Map<String, Object> root, String path);
}
