package de.happybavarian07.coolstufflib.configstuff.advanced.core;

import java.util.HashMap;
import java.util.Map;

/**
 * <p>Thread-safe manager for configuration path comments that provides storage and
 * retrieval operations for documentation strings associated with configuration keys.
 * Uses read-write locking to ensure concurrent access safety.</p>
 *
 * <p>This manager provides:</p>
 * <ul>
 * <li>Thread-safe comment storage with read-write locks</li>
 * <li>Path-based comment association</li>
 * <li>Null-safe operations for comment removal</li>
 * <li>Integration with configuration locking mechanisms</li>
 * </ul>
 *
 * <pre><code>
 * ConfigCommentManager manager = new ConfigCommentManager(lockManager);
 * manager.setComment("database.host", "Primary database server address");
 * String comment = manager.getComment("database.host");
 * </code></pre>
 */
public class ConfigCommentManager {
    private final Map<String, String> comments = new HashMap<>();
    private final ConfigLockManager lockManager;

    public ConfigCommentManager(ConfigLockManager lockManager) {
        this.lockManager = lockManager;
    }

    /**
     * <p>Sets or removes a comment for the specified configuration path.
     * If the comment is null, the existing comment will be removed.</p>
     *
     * <pre><code>
     * manager.setComment("server.port", "Port for HTTP connections");
     * manager.setComment("old.setting", null); // Removes comment
     * </code></pre>
     *
     * @param path the configuration path to associate the comment with
     * @param comment the comment text, or null to remove existing comment
     */
    public void setComment(String path, String comment) {
        lockManager.lockValuesWrite();
        try {
            if (comment == null) comments.remove(path);
            else comments.put(path, comment);
        } finally {
            lockManager.unlockValuesWrite();
        }
    }

    /**
     * <p>Retrieves the comment associated with the specified configuration path.</p>
     *
     * <pre><code>
     * String comment = manager.getComment("database.timeout");
     * if (comment != null) {
     *     // Display or process the comment
     * }
     * </code></pre>
     *
     * @param path the configuration path to get the comment for
     * @return the comment text, or null if no comment exists for this path
     */
    public String getComment(String path) {
        lockManager.lockValuesRead();
        try {
            return comments.get(path);
        } finally {
            lockManager.unlockValuesRead();
        }
    }

    /**
     * <p>Removes the comment associated with the specified configuration path.
     * No effect if no comment exists for the given path.</p>
     *
     * <pre><code>
     * manager.removeComment("deprecated.setting");
     * // Comment is now removed for this path
     * </code></pre>
     *
     * @param path the configuration path to remove the comment from
     */
    public void removeComment(String path) {
        lockManager.lockValuesWrite();
        try {
            comments.remove(path);
        } finally {
            lockManager.unlockValuesWrite();
        }
    }

    /**
     * <p>Checks whether a comment exists for the specified configuration path.</p>
     *
     * <pre><code>
     * if (manager.hasComment("feature.enabled")) {
     *     String comment = manager.getComment("feature.enabled");
     *     // Process the existing comment
     * }
     * </code></pre>
     *
     * @param path the configuration path to check for comment existence
     * @return true if a comment exists for this path, false otherwise
     */
    public boolean hasComment(String path) {
        lockManager.lockValuesRead();
        try {
            return comments.containsKey(path);
        } finally {
            lockManager.unlockValuesRead();
        }
    }

    public void setBulkComments(Map<String, String> comments) {
        if (comments == null) return;
        lockManager.lockValuesWrite();
        try {
            this.comments.clear();
            this.comments.putAll(comments);
        } finally {
            lockManager.unlockValuesWrite();
        }
    }

    public Map<String, String> getAllComments() {
        lockManager.lockValuesRead();
        try {
            return new HashMap<>(comments);
        } finally {
            lockManager.unlockValuesRead();
        }
    }
}
