package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 02.10.2021 | 13:15
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

public class PluginFileLogger {
    private final JavaPlugin plugin;
    private final File logFile;
    private final PluginFileLogger instance;
    private final Logger logger;

    public PluginFileLogger(JavaPlugin javaPluginUsingThisLib) {
        instance = this;
        plugin = javaPluginUsingThisLib;
        logFile = new File(plugin.getDataFolder(), "plugin.log");
        logger = plugin.getLogger();
        createLogFile();
    }

    public PluginFileLogger(JavaPlugin javaPluginUsingThisLib, String logFileName) {
        instance = this;
        plugin = javaPluginUsingThisLib;
        logFile = new File(plugin.getDataFolder(), logFileName);
        logger = plugin.getLogger();
        createLogFile();
    }

    public PluginFileLogger(File dataFolder, String logFileName) {
        instance = this;
        plugin = null;
        logFile = new File(dataFolder, logFileName);
        logger = Logger.getLogger("PluginFileLogger-" + logFileName);
        createLogFile();
    }

    public PluginFileLogger writeToLog(Level record, String stringToLog, LogPrefix logPrefix, boolean sendToConsole) {
        if (!logPrefix.isEnabled()) return instance;
        return writeToLog(record, stringToLog, logPrefix.getLogPrefix(), sendToConsole);
    }

    public PluginFileLogger writeToLog(Level record, String stringToLog, String logPrefix, boolean sendToConsole) {
        if (plugin != null && !plugin.getConfig().getBoolean("Plugin.LogActions.enabled", true)) return instance;
        String time = new SimpleDateFormat("HH:mm:ss").format(new Date());
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true))) {
            bw.write("[" + time + " " + record + "]: [" + logPrefix + "] " + stringToLog);
            bw.newLine();
        } catch (IOException e) {
            logger.log(Level.WARNING, "Could not write to log file " + logFile, e);
        }
        if (sendToConsole) logger.log(record, "[" + logPrefix + "] " + stringToLog);
        return instance;
    }

    public File getLogFile() {
        return logFile;
    }

    public void createLogFile() {
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                logger.log(Level.WARNING, "Could not create log file " + logFile, e);
            }
        }
    }
}
