package de.happybavarian07.coolstufflib.configstuff.advanced;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;

import java.io.File;
import java.util.logging.Level;

public class ConfigLogger {
    private static final String LOG_FILE = "AdvancedConfigSystemLog.log";
    private static PluginFileLogger logger;
    private static final String PREFIX = "AdvancedConfigSystem";

    private ConfigLogger() {
    }

    public static void initialize(File rootDirectory) {
        if (logger == null) {
            logger = new PluginFileLogger(rootDirectory, LOG_FILE);
        }
    }

    public static void log(Level level, String individualPrefix, String message, boolean sendToConsole) {
        if (logger != null) {
            String fullPrefix = individualPrefix != null ? PREFIX + " - " + individualPrefix : PREFIX;
            logger.writeToLog(level, message, fullPrefix, sendToConsole);
        }
    }

    public static void info(String message, String individualPrefix, boolean sendToConsole) {
        log(Level.INFO, individualPrefix, message, sendToConsole);
    }

    public static void warning(String message, String individualPrefix, boolean sendToConsole) {
        log(Level.WARNING, individualPrefix, message, sendToConsole);
    }

    public static void severe(String message, String individualPrefix, boolean sendToConsole) {
        log(Level.SEVERE, individualPrefix, message, sendToConsole);
    }

    public static void config(String message, String individualPrefix, boolean sendToConsole) {
        log(Level.CONFIG, individualPrefix, message, sendToConsole);
    }
}
