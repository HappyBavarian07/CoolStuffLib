package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces;

import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.AdvancedConfig;
import java.io.File;
import java.io.IOException;
import java.util.Map;

public interface ConfigFileHandler {
    void save(AdvancedConfig config, File file) throws IOException;
    Map<String, Object> load(File file) throws IOException;
}
