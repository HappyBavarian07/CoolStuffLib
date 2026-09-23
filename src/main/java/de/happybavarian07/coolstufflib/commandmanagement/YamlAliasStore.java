package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Stores aliases in their own section of a data file (default {@code CommandAliases} in
 * {@code data.yml}). Pass the plugin's already loaded data configuration so other code writing the
 * same file does not overwrite alias changes, or let the store load the file itself.</p>
 *
 * <pre><code>new YamlAliasStore(dataYml, dataFile, YamlAliasStore.DEFAULT_SECTION)</code></pre>
 */
public final class YamlAliasStore implements AliasStore {
    public static final String DEFAULT_SECTION = "CommandAliases";

    private final FileConfiguration data;
    private final File file;
    private final String sectionPath;

    public YamlAliasStore(File dataFile) {
        this(YamlConfiguration.loadConfiguration(dataFile), dataFile, DEFAULT_SECTION);
    }

    public YamlAliasStore(FileConfiguration data, File file, String sectionPath) {
        if (file.getName().equalsIgnoreCase("config.yml")) {
            throw new IllegalArgumentException("Aliases belong in the data file (e.g. data.yml), not config.yml; saving would drop its comments");
        }
        this.data = data;
        this.file = file;
        this.sectionPath = sectionPath;
    }

    @Override
    public synchronized List<CommandAlias> loadAll() {
        List<CommandAlias> aliases = new ArrayList<>();
        ConfigurationSection section = data.getConfigurationSection(sectionPath);
        if (section == null) return aliases;
        for (String name : section.getKeys(false)) {
            try {
                aliases.add(CommandAlias.fromSection(section, name));
            } catch (RuntimeException e) {
                CoolStuffLib.logError("Skipping invalid alias '" + name + "' in " + file.getName(), e);
            }
        }
        return aliases;
    }

    @Override
    public synchronized void save(CommandAlias alias) {
        String path = sectionPath + "." + alias.name();
        data.set(path, null);
        alias.writeTo(data.createSection(path));
        persist();
    }

    @Override
    public synchronized void delete(String name) {
        data.set(sectionPath + "." + name, null);
        persist();
    }

    private void persist() {
        try {
            data.save(file);
        } catch (IOException e) {
            CoolStuffLib.logError("Could not save command aliases to " + file.getName(), e);
        }
    }
}
