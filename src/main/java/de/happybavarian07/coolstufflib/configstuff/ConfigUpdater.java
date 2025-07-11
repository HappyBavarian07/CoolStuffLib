package de.happybavarian07.coolstufflib.configstuff;/*
 * @Author HappyBavarian07
 * @Date 28.04.2022 | 16:22
 */

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.configuration.serialization.ConfigurationSerializable;
import org.bukkit.plugin.Plugin;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A class to update/add new sections/keys to your config while keeping your current values and keeping your comments
 * Algorithm:
 * Read the new file and scan for comments and ignored sections, if ignored section is found it is treated as a comment.
 * Read and write each line of the new config, if the old config has value for the given key it writes that value in the new config.
 * If a key has an attached comment above it, it is written first.
 *
 * @author tchristofferson
 */
public class ConfigUpdater {

    /**
     * <p>Updates a YAML configuration file by merging new structure from a plugin resource
     * while preserving existing values and comments. This method intelligently combines
     * the latest configuration template with user customizations.</p>
     *
     * <p>The update process:</p>
     * <ul>
     * <li>Loads the new configuration template from plugin resources</li>
     * <li>Preserves all existing user values from the current file</li>
     * <li>Maintains comments and formatting where possible</li>
     * <li>Ignores specified sections to prevent overwriting sensitive data</li>
     * <li>Handles special version keys for language files</li>
     * </ul>
     *
     * <pre><code>
     * List&lt;String&gt; ignoredSections = Arrays.asList("database", "custom-settings");
     * ConfigUpdater.update(plugin, "config.yml", configFile, ignoredSections);
     * // Config file now has latest structure with preserved user values
     * </code></pre>
     *
     * @param plugin          the plugin instance providing access to bundled resources
     * @param resourceName    the name of the YAML resource file to use as template
     * @param toUpdate        the target configuration file to update
     * @param ignoredSections list of configuration section paths to preserve unchanged
     * @throws IOException if file reading, writing, or resource access fails
     */
    public static void update(Plugin plugin, String resourceName, File toUpdate, List<String> ignoredSections) throws IOException {
        DumperOptions dumperOptions = new DumperOptions();
        dumperOptions.setSplitLines(false);
        dumperOptions.setPrettyFlow(true);
        // TODO Add Stats like Counting how many lines got changed/needed to be changed,
        //  Errors, Null Lines, New Lines, etc.

        /*if (resourceName.contains("languages")) {
            dumperOptions.setDefaultScalarStyle(DumperOptions.ScalarStyle.SINGLE_QUOTED);
        }*/

        BufferedReader newReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(plugin.getResource(resourceName)), StandardCharsets.UTF_8));
        List<String> newLines = newReader.lines().collect(Collectors.toList());
        newReader.close();

        FileConfiguration oldConfig = YamlConfiguration.loadConfiguration(toUpdate);
        FileConfiguration newConfig = YamlConfiguration.loadConfiguration(new InputStreamReader(Objects.requireNonNull(plugin.getResource(resourceName))));
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(Files.newOutputStream(toUpdate.toPath()), StandardCharsets.UTF_8));

        List<String> ignoredSectionsArrayList = new ArrayList<>(ignoredSections);
        //ignoredSections can ONLY contain configurations sections
        //ignoredSectionsArrayList.removeIf(ignoredSection -> !newConfig.isConfigurationSection(ignoredSection));

        //System.out.println("Ignored Sections: " + ignoredSectionsArrayList);

        Yaml yaml = new Yaml(dumperOptions);
        Map<String, String> comments = parseComments(newLines, ignoredSectionsArrayList, oldConfig, yaml);
        write(newConfig, oldConfig, comments, ignoredSectionsArrayList, writer, yaml);
    }

    private static void write(FileConfiguration newConfig, FileConfiguration oldConfig, Map<String, String> comments, List<String> ignoredSections, BufferedWriter writer, Yaml yaml) throws IOException {
        writeSectionRecursive(newConfig, oldConfig, comments, ignoredSections, writer, yaml, "", 0);
        String danglingComments = comments.get(null);
        if (danglingComments != null) {
            writer.write(danglingComments);
        }
        writer.close();
    }

    private static void writeSectionRecursive(ConfigurationSection section, FileConfiguration oldConfig, Map<String, String> comments, List<String> ignoredSections, BufferedWriter writer, Yaml yaml, String path, int indent) throws IOException {
        Set<String> keys = section.getKeys(false);
        List<String> filteredKeys = new ArrayList<>(keys);
        filteredKeys.removeIf(key -> {
            String fullKey = path.isEmpty() ? key : path + "." + key;
            for (String ignored : ignoredSections) {
                if (fullKey.equals(ignored) || fullKey.startsWith(ignored + ".")) {
                    return true;
                }
            }
            return false;
        });
        for (String key : filteredKeys) {
            String fullKey = path.isEmpty() ? key : path + "." + key;
            String comment = comments.remove(fullKey);
            if (comment != null) writer.write(comment);
            Object value = section.get(key);
            String prefix = getPrefixSpaces(indent);
            if (value instanceof ConfigurationSection) {
                writer.write(prefix + key + ":\n");
                writeSectionRecursive((ConfigurationSection) value, oldConfig, comments, ignoredSections, writer, yaml, fullKey, indent + 1);
            } else if (value instanceof List) {
                writer.write(getListAsString((List) value, key, prefix, yaml));
            } else if (value instanceof ConfigurationSerializable) {
                writer.write(prefix + key + ": " + yaml.dump(((ConfigurationSerializable) value).serialize()));
            } else if (value instanceof String s) {
                s = s.replace("\n", "\\n");
                writer.write(prefix + key + ": " + yaml.dump(s));
            } else {
                writer.write(prefix + key + ": " + yaml.dump(value));
            }
        }
    }

    //Doesn't work with configuration sections, must be an actual object
    //Auto checks if it is serializable and writes to file
    private static void write(Object obj, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
        if (obj instanceof ConfigurationSerializable) {
            writer.write(prefixSpaces + actualKey + ": " + yaml.dump(((ConfigurationSerializable) obj).serialize()));
        } else if (obj instanceof String || obj instanceof Character) {
            if (obj instanceof String s) {
                obj = s.replace("\n", "\\n");
            }

            writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));
        } else if (obj instanceof List) {
            writeList((List) obj, actualKey, prefixSpaces, yaml, writer);
        } else {
            writer.write(prefixSpaces + actualKey + ": " + yaml.dump(obj));
        }
    }

    //Writes a configuration section
    private static void writeSection(BufferedWriter writer, String actualKey, String prefixSpaces, ConfigurationSection section) throws IOException {
        if (section.getKeys(false).isEmpty()) {
            writer.write(prefixSpaces + actualKey + ": {}");
        } else {
            writer.write(prefixSpaces + actualKey + ":");
        }

        writer.write("\n");
    }

    //Writes a list of any object
    private static void writeList(List list, String actualKey, String prefixSpaces, Yaml yaml, BufferedWriter writer) throws IOException {
        writer.write(getListAsString(list, actualKey, prefixSpaces, yaml));
    }

    private static String getListAsString(List list, String actualKey, String prefixSpaces, Yaml yaml) {
        StringBuilder builder = new StringBuilder(prefixSpaces).append(actualKey).append(":");

        if (list.isEmpty()) {
            builder.append(" []\n");
            return builder.toString();
        }

        builder.append("\n");

        for (int i = 0; i < list.size(); i++) {
            Object o = list.get(i);

            if (o instanceof String || o instanceof Character) {
                builder.append(prefixSpaces).append("- \"").append(o).append("\"");
            } else if (o instanceof List) {
                builder.append(prefixSpaces).append("- ").append(yaml.dump(o));
            } else {
                builder.append(prefixSpaces).append("- ").append(o);
            }

            if (i != list.size()) {
                builder.append("\n");
            }
        }

        return builder.toString();
    }

    //Key is the config key, value = comment and/or ignored sections
    //Parses comments, blank lines, and ignored sections
    private static Map<String, String> parseComments(List<String> lines, List<String> ignoredSections, FileConfiguration oldConfig, Yaml yaml) {
        Map<String, String> comments = new HashMap<>();
        StringBuilder builder = new StringBuilder();
        StringBuilder keyBuilder = new StringBuilder();
        int lastLineIndentCount = 0;

        outer:
        for (String line : lines) {
            if (line != null && line.trim().startsWith("-"))
                continue;

            if (line == null || line.trim().isEmpty() || line.trim().startsWith("#")) {
                builder.append(line).append("\n");
            } else {
                lastLineIndentCount = setFullKey(keyBuilder, line, lastLineIndentCount);

                for (String ignoredSection : ignoredSections) {
                    if (keyBuilder.toString().equals(ignoredSection)) {
                        Object value = oldConfig.get(keyBuilder.toString());

                        if (value instanceof ConfigurationSection)
                            appendSection(builder, (ConfigurationSection) value, new StringBuilder(getPrefixSpaces(lastLineIndentCount)), yaml);

                        continue outer;
                    }
                }

                if (!keyBuilder.isEmpty()) {
                    comments.put(keyBuilder.toString(), builder.toString());
                    builder.setLength(0);
                }
            }
        }

        if (!builder.isEmpty()) {
            comments.put(null, builder.toString());
        }

        return comments;
    }

    private static void appendSection(StringBuilder builder, ConfigurationSection section, StringBuilder prefixSpaces, Yaml yaml) {
        builder.append(prefixSpaces).append(getKeyFromFullKey(section.getCurrentPath())).append(":");
        Set<String> keys = section.getKeys(false);

        if (keys.isEmpty()) {
            builder.append(" {}\n");
            return;
        }

        builder.append("\n");
        prefixSpaces.append("  ");

        for (String key : keys) {
            Object value = section.get(key);
            String actualKey = getKeyFromFullKey(key);

            if (value instanceof ConfigurationSection) {
                appendSection(builder, (ConfigurationSection) value, prefixSpaces, yaml);
                prefixSpaces.setLength(prefixSpaces.length() - 2);
            } else if (value instanceof List) {
                builder.append(getListAsString((List) value, actualKey, prefixSpaces.toString(), yaml));
            } else {
                builder.append(prefixSpaces).append(actualKey).append(": ").append(yaml.dump(value));
            }
        }
    }

    //Counts spaces in front of key and divides by 2 since 1 indent = 2 spaces
    private static int countIndents(String s) {
        int spaces = 0;

        for (char c : s.toCharArray()) {
            if (c == ' ') {
                spaces += 1;
            } else {
                break;
            }
        }

        return spaces / 2;
    }

    //Ex. keyBuilder = key1.key2.key3 --> key1.key2
    private static void removeLastKey(StringBuilder keyBuilder) {
        String temp = keyBuilder.toString();
        String[] keys = temp.split("\\.");

        if (keys.length == 1) {
            keyBuilder.setLength(0);
            return;
        }

        temp = temp.substring(0, temp.length() - keys[keys.length - 1].length() - 1);
        keyBuilder.setLength(temp.length());
    }

    private static String getKeyFromFullKey(String fullKey) {
        String[] keys = fullKey.split("\\.");
        return keys[keys.length - 1];
    }

    //Updates the keyBuilder and returns configLines number of indents
    private static int setFullKey(StringBuilder keyBuilder, String configLine, int lastLineIndentCount) {
        int currentIndents = countIndents(configLine);
        String key = configLine.trim().split(":")[0];

        if (keyBuilder.isEmpty()) {
            keyBuilder.append(key);
        } else if (currentIndents == lastLineIndentCount) {
            //Replace the last part of the key with current key
            removeLastKey(keyBuilder);

            if (!keyBuilder.isEmpty()) {
                keyBuilder.append(".");
            }

            keyBuilder.append(key);
        } else if (currentIndents > lastLineIndentCount) {
            //Append current key to the keyBuilder
            keyBuilder.append(".").append(key);
        } else {
            int difference = lastLineIndentCount - currentIndents;

            for (int i = 0; i < difference + 1; i++) {
                removeLastKey(keyBuilder);
            }

            if (!keyBuilder.isEmpty()) {
                keyBuilder.append(".");
            }

            keyBuilder.append(key);
        }

        return currentIndents;
    }

    private static String getPrefixSpaces(int indents) {
        return "  ".repeat(Math.max(0, indents));
    }

    private static void appendPrefixSpaces(StringBuilder builder, int indents) {
        builder.append(getPrefixSpaces(indents));
    }
}
