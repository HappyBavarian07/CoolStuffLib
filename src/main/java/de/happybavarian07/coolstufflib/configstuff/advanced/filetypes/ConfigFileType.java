package de.happybavarian07.coolstufflib.configstuff.advanced.filetypes;

import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.handlers.*;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.interfaces.ConfigFileHandler;

public enum ConfigFileType {
    YAML(YamlConfigFileHandler.class, "yml"),
    JSON(JsonConfigFileHandler.class, "json"),
    JSON5(Json5ConfigFileHandler.class, "json5"),
    PROPERTIES(PropertiesConfigFileHandler.class, "properties"),
    INI(IniConfigFileHandler.class, "ini"),
    TOML(TomlConfigFileHandler.class, "toml"),
    MEMORY(InMemoryConfigFileHandler.class, "NONE");

    private final Class<? extends ConfigFileHandler> handlerClass;
    private final String fileExtension;

    ConfigFileType(Class<? extends ConfigFileHandler> handlerClass, String fileExtension) {
        this.handlerClass = handlerClass;
        this.fileExtension = fileExtension;
    }

    public ConfigFileHandler createHandler() {
        try {
            return handlerClass.getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Failed to create ConfigFileHandler for " + this.name(), e);
        }
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
