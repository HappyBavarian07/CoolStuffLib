package de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.templates;

import de.happybavarian07.coolstufflib.configstuff.advanced.modules.autogen.misc.Group;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface AutoGenTemplate {
    void applyTo(Group root);

    /**
     * Returns the base path in the config where this template applies (e.g. "players.&lt;uuid&gt;").
     */
    default String getBasePath() { return ""; }

    /**
     * Returns the template as a nested map for structure inspection and repair.
     */
    default Map<String, Object> toMap() { return java.util.Collections.emptyMap(); }

    /**
     * Writes the template to the specified file.
     *
     * @param file The file to write the template to
     * @throws IOException If an error occurs while writing the file
     */
    void writeToFile(File file) throws IOException;
}
