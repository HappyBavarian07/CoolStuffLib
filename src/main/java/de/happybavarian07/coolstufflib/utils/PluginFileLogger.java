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
import java.util.Calendar;
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
        if (plugin != null && !plugin.getConfig().getBoolean("Plugin.LogActions.enabled", true) || !logPrefix.isEnabled()) return instance;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            Date d = Calendar.getInstance().getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String prefix = "[" + sdf.format(d) + " " + record + "]: [" + logPrefix.getLogPrefix() + "] ";
            bw.write(prefix + stringToLog);
            bw.newLine();
            bw.close();
            if (sendToConsole)
                logger.log(record, "[" + logPrefix + "] " + stringToLog);
            return instance;
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            if (sendToConsole)
                logger.log(record, "[" + logPrefix + "] " + stringToLog);
            return instance;
        }
    }

    public PluginFileLogger writeToLog(Level record, String stringToLog, String logPrefix, boolean sendToConsole) {
        if (plugin != null && !plugin.getConfig().getBoolean("Plugin.LogActions.enabled", true)) return instance;
        try {
            BufferedWriter bw = new BufferedWriter(new FileWriter(logFile, true));
            Date d = Calendar.getInstance().getTime();
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
            String prefix = "[" + sdf.format(d) + " " + record + "]: [" + logPrefix + "] ";
            bw.write(prefix + stringToLog);
            bw.newLine();
            bw.close();
            if (sendToConsole)
                logger.log(record, "[" + logPrefix + "] " + stringToLog);
            return instance;
        } catch (IOException fileNotFoundException) {
            fileNotFoundException.printStackTrace();
            if (sendToConsole)
                logger.log(record, "[" + logPrefix + "] " + stringToLog);
            return instance;
        }
    }

    public File getLogFile() {
        return logFile;
    }

    public void createLogFile() {
        if (!logFile.exists()) {
            try {
                logFile.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
