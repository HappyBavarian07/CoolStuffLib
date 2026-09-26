package de.happybavarian07.coolstufflib.logging;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.utils.PluginFileLogger;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * <p>Centralized logger for configuration-related events.</p>
 */
public final class ConfigLogger {

    private static final String LOG_FILE = "config.log";
    private static final Logger FALLBACK = Logger.getLogger("CoolStuffLib-Config");
    private static PluginFileLogger logger;
    private static boolean initialized = false;

    private ConfigLogger() {
    }

    /**
     * <p>Initializes the logger.</p>
     *
     * @param rootDirectory The root directory
     */
    public static synchronized void initialize(File rootDirectory) {
        if (initialized) {
            return;
        }

        try {
            logger = new PluginFileLogger(CoolStuffLib.getLib().getJavaPluginUsingLib(), LOG_FILE);
        } catch (RuntimeException e) {
            logger = new PluginFileLogger(rootDirectory, LOG_FILE);
        }

        initialized = true;
    }

    /**
     * <p>Logs an info message.</p>
     *
     * @param message The message
     * @param source  The log source
     * @param console Whether to log to console
     */
    public static void info(String message, String source, boolean console) {
        write(Level.INFO, message, source, console);
    }

    /**
     * <p>Logs a warning message.</p>
     *
     * @param message The warning
     * @param source  The log source
     * @param console Whether to log to console
     */
    public static void warning(String message, String source, boolean console) {
        write(Level.WARNING, message, source, console);
    }

    /**
     * <p>Logs an error message.</p>
     *
     * @param message The error message
     * @param source  The log source
     * @param console Whether to log to console
     */
    public static void error(String message, String source, boolean console) {
        write(Level.SEVERE, message, source, console);
    }

    /**
     * <p>Logs an error with stack trace.</p>
     *
     * @param message   The error message
     * @param throwable The exception
     * @param source    The log source
     * @param console   Whether to log to console
     */
    public static void error(String message, Throwable throwable, String source, boolean console) {
        StringBuilder fullMessage = new StringBuilder();
        fullMessage.append(message)
                .append(" - ")
                .append(throwable.getClass().getName())
                .append(": ")
                .append(throwable.getMessage());
        for (StackTraceElement element : throwable.getStackTrace()) {
            fullMessage.append(System.lineSeparator()).append("    at ").append(element.toString());
        }
        write(Level.SEVERE, fullMessage.toString(), source, console);
    }

    public static void debug(String message, String source) {
        if (!isDebugEnabled()) {
            return;
        }

        write(Level.FINE, "[DEBUG] " + message, source, true);
    }

    public static boolean isDebugEnabled() {
        if (CoolStuffLib.getLib() == null) {
            return false;
        }
        return CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getBoolean("Config.Debug", false);
    }

    /**
     * Without {@link #initialize(File)} the logger writes to the plugin's {@code config.log} once CoolStuffLib is set
     * up, and to the console before that.
     */
    private static synchronized PluginFileLogger fileLogger() {
        if (!initialized) {
            try {
                if (CoolStuffLib.getLib() != null) {
                    logger = new PluginFileLogger(CoolStuffLib.getLib().getJavaPluginUsingLib(), LOG_FILE);
                    initialized = true;
                }
            } catch (RuntimeException ignored) {
                // Not set up yet: fall back to the console
            }
        }
        return logger;
    }

    private static void write(Level level, String message, String source, boolean console) {
        PluginFileLogger file = fileLogger();
        if (file != null) {
            file.writeToLog(level, message, source, console);
        } else {
            FALLBACK.log(level, "[" + source + "] " + message);
        }
    }

    /**
     * @return the file logger, or {@code null} if neither {@link #initialize(File)} was called nor CoolStuffLib is set up
     */
    public static PluginFileLogger getLogger() {
        return fileLogger();
    }

    public static void warn(String s, String persistentBackupModule, boolean b) {
        write(Level.WARNING, s, persistentBackupModule, b);
    }
}
