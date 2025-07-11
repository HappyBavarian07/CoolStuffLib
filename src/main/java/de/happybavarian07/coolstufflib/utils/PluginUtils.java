package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.SimpleCommandMap;
import org.bukkit.event.Event;
import org.bukkit.plugin.*;

import java.io.*;
import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.NotDirectoryException;
import java.util.*;
import java.util.logging.Level;

public class PluginUtils {
    private final CoolStuffLib coolStuffLib;

    public PluginUtils() {
        this.coolStuffLib = CoolStuffLib.getLib();
    }

    public static void copyURLToFile(URL sourceUrl, File destinationFile) throws IOException {
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        OutputStream outputStream = null;

        try {
            // Open a connection to the URL
            connection = (HttpURLConnection) sourceUrl.openConnection();
            connection.connect();

            // Check if the response code indicates success (HTTP 200)
            int responseCode = connection.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("HTTP response code: " + responseCode);
            }

            // Open input and output streams
            inputStream = connection.getInputStream();
            outputStream = new FileOutputStream(destinationFile);

            // Copy data from the input stream to the output stream
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            // Flush and close the output stream
            outputStream.flush();
        } finally {
            // Close the streams and disconnect the connection
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (outputStream != null) {
                try {
                    outputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    public void load(Plugin plugin) {
        load(plugin.getName(), plugin.getDescription().getVersion());
    }

    public void load(String name, String version) {

        Plugin target;

        File pluginDir = new File("plugins");

        if (!pluginDir.isDirectory()) {
            return;
        }

        File pluginFile = new File(pluginDir, name + "-" + version + ".jar");

        if (!pluginFile.isFile()) {
            for (File f : pluginDir.listFiles()) {
                if (f.getName().endsWith(".jar")) {
                    try {
                        PluginDescriptionFile desc = coolStuffLib.getJavaPluginUsingLib().getPluginLoader().getPluginDescription(f);
                        if (desc.getName().equalsIgnoreCase(name)) {
                            pluginFile = f;
                            break;
                        }
                    } catch (InvalidDescriptionException e) {
                        return;
                    }
                }
            }
        }

        try {
            target = Bukkit.getPluginManager().loadPlugin(pluginFile);
        } catch (InvalidDescriptionException | InvalidPluginException e) {
            e.printStackTrace();
            return;
        }
        Bukkit.getPluginManager().enablePlugin(target);
    }

    public Plugin load(File pluginFile) throws InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        coolStuffLib.getPluginFileLogger().writeToLog(Level.WARNING,
                "Loaded Plugin File \"" + pluginFile + "\"", LogPrefix.ACTIONSLOGGER_PLUGIN, true);
        return Bukkit.getPluginManager().loadPlugin(pluginFile);
    }

    public List<Plugin> getAllPlugins() {
        List<Plugin> plugins = new ArrayList<>();
        for (String pluginName : getPluginNames(false)) {
            plugins.add(getPluginByName(pluginName));
        }
        return plugins;
    }

    public Plugin getPluginByName(String name) {
        if (name == null) return null;
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            if (name.equalsIgnoreCase(plugin.getName())) {
                return plugin;
            }
        }
        return null;
        //throw new NullPointerException("Plugin: " + name + " is null!");
    }

    public List<String> getPluginNames(boolean fullName) {
        List<String> plugins = new ArrayList<>();
        for (Plugin plugin : Bukkit.getPluginManager().getPlugins()) {
            plugins.add(fullName ? plugin.getDescription().getFullName() : plugin.getName());
        }
        return plugins;
    }

    public void unload(Plugin plugin) {
        String name = plugin.getName();
        PluginManager pluginManager = Bukkit.getPluginManager();
        SimpleCommandMap commandMap = null;
        List<Plugin> plugins = null;
        Map<String, Plugin> names = null;
        Map<String, Command> commands = null;
        Map<Event, SortedSet<RegisteredListener>> listeners = null;
        boolean reloadlisteners = true;

        pluginManager.disablePlugin(plugin);

        try {
            Field pluginsField = Bukkit.getPluginManager().getClass().getDeclaredField("plugins");
            pluginsField.setAccessible(true);
            plugins = (List<Plugin>) pluginsField.get(pluginManager);
            Field lookupNamesField = Bukkit.getPluginManager().getClass().getDeclaredField("lookupNames");
            lookupNamesField.setAccessible(true);
            names = (Map<String, Plugin>) lookupNamesField.get(pluginManager);
            try {
                Field listenersField = Bukkit.getPluginManager().getClass().getDeclaredField("listeners");
                listenersField.setAccessible(true);
                listeners = (Map<Event, SortedSet<RegisteredListener>>) listenersField.get(pluginManager);
            } catch (Exception e) {
                reloadlisteners = false;
            }

            Field commandMapField = Bukkit.getPluginManager().getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            commandMap = (SimpleCommandMap) commandMapField.get(pluginManager);
            Field knownCommandsField = SimpleCommandMap.class.getDeclaredField("knownCommands");
            knownCommandsField.setAccessible(true);
            commands = (Map<String, Command>) knownCommandsField.get(commandMap);

        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }

        pluginManager.disablePlugin(plugin);

        if (plugins != null)
            plugins.remove(plugin);

        if (names != null)
            names.remove(name);

        if (listeners != null && reloadlisteners) {
            for (SortedSet<RegisteredListener> set : listeners.values()) {
                set.removeIf(value -> value.getPlugin() == plugin);
            }
        }

        if (commandMap != null) {
            assert commands != null;
            for (Iterator<Map.Entry<String, Command>> it = commands.entrySet().iterator(); it.hasNext(); ) {
                Map.Entry<String, Command> entry = it.next();
                if (entry.getValue() instanceof PluginCommand) {
                    PluginCommand c = (PluginCommand) entry.getValue();
                    if (c.getPlugin() == plugin) {
                        c.unregister(commandMap);
                        it.remove();
                    }
                }
            }
        }

        // Attempt to close the classloader to unlock any handles on the plugin's jar file.
        ClassLoader cl = plugin.getClass().getClassLoader();

        if (cl instanceof URLClassLoader) {

            try {

                Field pluginField = cl.getClass().getDeclaredField("plugin");
                pluginField.setAccessible(true);
                pluginField.set(cl, null);

                Field pluginInitField = cl.getClass().getDeclaredField("pluginInit");
                pluginInitField.setAccessible(true);
                pluginInitField.set(cl, null);

            } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException ex) {
                ex.printStackTrace();
            }

            try {

                ((URLClassLoader) cl).close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }

        }

        // Will not work on processes started with the -XX:+DisableExplicitGC flag, but lets try it anyway.
        // This tries to get around the issue where Windows refuses to unlock jar files that were previously loaded into the JVM.
        System.gc();
        this.coolStuffLib.getPluginFileLogger().writeToLog(Level.WARNING,
                "Unloaded the Plugin \"" + plugin + "\"", LogPrefix.ACTIONSLOGGER_PLUGIN, true);
    }

    public void reload(Plugin plugin) {
        if (plugin != null) {
            unload(plugin);
            load(plugin);
            this.coolStuffLib.getPluginFileLogger().writeToLog(Level.WARNING,
                    "Reloaded the Plugin \"" + plugin + "\"", LogPrefix.ACTIONSLOGGER_PLUGIN, true);
        } else {
            throw new NullPointerException("Plugin is null!");
        }
    }

    public Plugin downloadPluginFromSpiget(int resourceID, String fileName, Boolean enableAfterStart) throws IOException, InvalidPluginException, InvalidDescriptionException, UnknownDependencyException {
        File pluginDir = new File("plugins");

        if (!pluginDir.isDirectory()) {
            throw new NotDirectoryException("No Plugins Directory found!");
        }

        File pluginFile = new File(pluginDir, fileName + ".jar");
        URL downloadURL = new URL("https://api.spiget.org/v2/resources/" + resourceID + "/download");
        copyURLToFile(downloadURL, pluginFile);
        Plugin target = load(pluginFile);
        if (enableAfterStart) {
            Bukkit.getPluginManager().enablePlugin(target);
        }
        coolStuffLib.getPluginFileLogger().writeToLog(Level.WARNING,
                "Installed the Plugin \"" + resourceID + "\" under the Name \"" +
                        "/plugins/" + fileName + ".jar" + "\"", LogPrefix.ACTIONSLOGGER_PLUGIN, true);
        return target;
    }
}
