package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface ConfigFileHandler {
    void save(File file, Map<String, Object> data) throws IOException;
    Map<String, Object> load(File file) throws IOException;
}
