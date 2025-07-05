package de.happybavarian07.coolstufflib.logging;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;

import java.io.File;
import java.util.logging.Level;

public final class ConfigLogger {

    private static final String LOG_FILE = "config.log";
    private static PluginFileLogger logger;
    private static boolean initialized = false;

    private ConfigLogger() {
        // Private constructor to prevent instantiation
    }

    public static void initialize(File rootDirectory) {
        if (initialized) {
            return;
        }

        if (CoolStuffLib.getLib() != null) {
            logger = new PluginFileLogger(CoolStuffLib.getLib().getJavaPluginUsingLib(), LOG_FILE);
        } else {
            logger = new PluginFileLogger(rootDirectory, LOG_FILE);
        }

        initialized = true;
    }

    public static void info(String message, String source, boolean console) {
        checkInitialized();
        logger.writeToLog(Level.INFO, message, source, console);
    }

    public static void warning(String message, String source, boolean console) {
        checkInitialized();
        logger.writeToLog(Level.WARNING, message, source, console);
    }

    public static void error(String message, String source, boolean console) {
        checkInitialized();
        logger.writeToLog(Level.SEVERE, message, source, console);
    }

    public static void error(String message, Throwable throwable, String source, boolean console) {
        checkInitialized();
        StringBuilder fullMessage = new StringBuilder();
        fullMessage.append(message)
            .append(" - ")
            .append(throwable.getClass().getName())
            .append(": ")
            .append(throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
            fullMessage.append(System.lineSeparator()).append("    at ").append(element.toString());
        }
        logger.writeToLog(Level.SEVERE, fullMessage.toString(), source, console);
    }

    public static void debug(String message, String source) {
        if (!isDebugEnabled()) {
            return;
        }

        checkInitialized();
        logger.writeToLog(Level.FINE, "[DEBUG] " + message, source, true);
    }

    public static boolean isDebugEnabled() {
        if (CoolStuffLib.getLib() == null) {
            return false;
        }
        return CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Config.Debug", false);
    }

    private static void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("ConfigLogger is not initialized");
        }
    }

    public static PluginFileLogger getLogger() {
        checkInitialized();
        return logger;
    }

    public static void warn(String s, String persistentBackupModule, boolean b) {
        checkInitialized();
        logger.writeToLog(Level.WARNING, s, persistentBackupModule, b);
    }
}
