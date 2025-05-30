package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.util.Map;

public interface AutoGenTemplate {
    void applyTo(Group root);

    /**
     * Returns the base path in the config where this template applies (e.g. "players.<uuid>").
     */
    default String getBasePath() { return ""; }

    /**
     * Returns the template as a nested map for structure inspection and repair.
     */
    default Map<String, Object> toMap() { return java.util.Collections.emptyMap(); }
}
