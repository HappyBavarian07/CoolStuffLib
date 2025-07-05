package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * <p>Asynchronous logging utility for plugin startup messages with thread-safe message queuing
 * and formatted console output. Provides a fluent API for creating structured startup logs
 * with spacers, colors, and formatted messages.</p>
 *
 * <p>This logger provides:</p>
 * <ul>
 * <li>Thread-safe message queuing with asynchronous processing</li>
 * <li>Configurable spacer formatting for visual separation</li>
 * <li>Color support for enhanced console output readability</li>
 * <li>Fluent API for chaining multiple log operations</li>
 * <li>Integration with CoolStuffLib's language management system</li>
 * </ul>
 *
 * <pre><code>
 * StartUpLogger logger = StartUpLogger.create();
 * logger.spacer()
 *       .message("Plugin initialization started")
 *       .coloredMessage(ChatColor.GREEN, "All systems ready")
 *       .spacer();
 * </code></pre>
 */
public class StartUpLogger {
    private final BlockingQueue<String[]> messageQueue;
    private final Thread messageQueueThread;
    private final String SPACER_FORMAT;
    private final ConsoleCommandSender sender = Bukkit.getConsoleSender();
    private boolean enabled;

    /**
     * <p>Constructs a new StartUpLogger with asynchronous message processing capabilities.
     * Initializes the message queue and starts a daemon thread for processing log messages.</p>
     *
     * <pre><code>
     * StartUpLogger logger = new StartUpLogger();
     * logger.message("Logger initialized successfully");
     * </code></pre>
     */
    public StartUpLogger() {
        messageQueue = new LinkedBlockingQueue<>();
        enabled = true;
        SPACER_FORMAT = CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getString("Plugin.StartUpLogger.Spacer_Format",
                "+-------------------------------------------------------------+");
        messageQueueThread = new Thread(() -> {
            while (true) {
                if (!enabled) continue;
                try {
                    String[] messages = messageQueue.take();
                    if (messages.length == 0) continue;
                    for (String message : messages) {
                        if (message == null || message.isEmpty()) continue;

                        sender.sendMessage(message);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }, "StartUpLogger Message Queue Thread");
        messageQueueThread.setDaemon(true);
        messageQueueThread.start();
    }

    /**
     * <p>Creates and returns a new StartUpLogger instance with default configuration.</p>
     *
     * <pre><code>
     * StartUpLogger logger = StartUpLogger.create();
     * logger.spacer().message("Plugin starting...").spacer();
     * </code></pre>
     *
     * @return a new StartUpLogger instance
     */
    public static StartUpLogger create() {
        return new StartUpLogger();
    }

    /**
     * <p>Adds a formatted spacer line to the message queue for visual separation in console output.</p>
     *
     * <pre><code>
     * logger.spacer()
     *       .message("Section content")
     *       .spacer();
     * </code></pre>
     *
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger spacer() {
        addMessageToQueue(SPACER_FORMAT);
        return this;
    }

    /**
     * <p>Gets the configured spacer format string without adding it to the message queue.</p>
     *
     * <pre><code>
     * String spacerFormat = logger.getSpacer();
     * // Returns: "+-------------------------------------------------------------+"
     * </code></pre>
     *
     * @return the spacer format string
     */
    public String getSpacer() {
        return SPACER_FORMAT;
    }

    /**
     * <p>Adds a colored spacer line to the message queue using the specified ChatColor.</p>
     *
     * <pre><code>
     * logger.coloredSpacer(ChatColor.RED)
     *       .message("Error section")
     *       .coloredSpacer(ChatColor.RED);
     * </code></pre>
     *
     * @param color the ChatColor to apply to the spacer
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger coloredSpacer(ChatColor color) {
        addMessageToQueue(color + SPACER_FORMAT);
        return this;
    }

    /**
     * <p>Gets a colored spacer format string without adding it to the message queue.</p>
     *
     * <pre><code>
     * String redSpacer = logger.getColoredSpacer(ChatColor.RED);
     * </code></pre>
     *
     * @param color the ChatColor to apply to the spacer
     * @return the colored spacer format string
     */
    public String getColoredSpacer(ChatColor color) {
        return color + SPACER_FORMAT;
    }

    /**
     * <p>Adds an empty line to the message queue for additional spacing in console output.</p>
     *
     * <pre><code>
     * logger.message("First section")
     *       .emptySpacer()
     *       .message("Second section");
     * </code></pre>
     *
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger emptySpacer() {
        addMessageToQueue("");
        return this;
    }

    /**
     * <p>Gets an empty string for spacing purposes without adding it to the message queue.</p>
     *
     * <pre><code>
     * String empty = logger.getEmptySpacer();
     * // Returns: ""
     * </code></pre>
     *
     * @return an empty string
     */
    public String getEmptySpacer() {
        return "";
    }

    /**
     * <p>Adds a formatted message to the queue with automatic prefix application from the language manager.</p>
     *
     * <pre><code>
     * logger.message("Plugin loaded successfully")
     *       .message("Configuration initialized");
     * </code></pre>
     *
     * @param message the message content to log
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger message(String message) {
        addMessageToQueue(Utils.format(null, getMessageWithFormat(message),
                CoolStuffLib.getLib().getLanguageManager().getPrefix() != null ? CoolStuffLib.getLib().getLanguageManager().getPrefix() : "[CoolStuffLib]"));
        return this;
    }

    /**
     * <p>Gets a formatted message string with prefix application without adding it to the queue.</p>
     *
     * <pre><code>
     * String formatted = logger.getMessage("System ready");
     * // Returns: "[CoolStuffLib] System ready"
     * </code></pre>
     *
     * @param message the message content to format
     * @return the formatted message string
     */
    public String getMessage(String message) {
        return Utils.format(null, getMessageWithFormat(message),
                CoolStuffLib.getLib().getLanguageManager().getPrefix() != null ? CoolStuffLib.getLib().getLanguageManager().getPrefix() : "[CoolStuffLib]");
    }

    /**
     * <p>Adds a colored message to the queue with the specified ChatColor applied.</p>
     *
     * <pre><code>
     * logger.coloredMessage(ChatColor.GREEN, "Success: All modules loaded")
     *       .coloredMessage(ChatColor.YELLOW, "Warning: Default config used");
     * </code></pre>
     *
     * @param color the ChatColor to apply to the message
     * @param message the message content to log
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger coloredMessage(ChatColor color, String message) {
        addMessageToQueue(color + message);
        return this;
    }

    /**
     * <p>Gets a colored message string without adding it to the queue.</p>
     *
     * <pre><code>
     * String greenMessage = logger.getColoredMessage(ChatColor.GREEN, "Operation successful");
     * </code></pre>
     *
     * @param color the ChatColor to apply to the message
     * @param message the message content to format
     * @return the colored message string
     */
    public String getColoredMessage(ChatColor color, String message) {
        return color + getMessageWithFormat(message);
    }

    /**
     * <p>Adds a data client message structure to the queue, including optional header, footer,
     * and title, all formatted with the specified ChatColor.</p>
     *
     * <pre><code>
     * logger.dataClientMessage(ChatColor.BLUE, "Data sync initiated", true, true);
     * </code></pre>
     *
     * @param color the ChatColor to apply to the message
     * @param message the main message content
     * @param headerAndFooter whether to include header and footer spacers
     * @param title whether to include the title line
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger dataClientMessage(ChatColor color, String message, boolean headerAndFooter, boolean title) {
        String[] finalMessage = new String[4];
        if (headerAndFooter)
            finalMessage[0] = getColoredSpacer(color);
        else
            finalMessage[0] = "";
        if (title)
            finalMessage[1] = getColoredMessage(color, "Java Socket Bungeecord Data Sync System (short: JSBDSS):");
        else
            finalMessage[1] = "";
        finalMessage[2] = getColoredMessage(color, message);
        if (headerAndFooter)
            finalMessage[3] = getColoredSpacer(color);
        else
            finalMessage[3] = "";
        addMessageToQueue(finalMessage);
        return this;
    }

    /**
     * <p>Adds a data client message structure to the queue, including optional header, footer,
     * and title, with multiple message lines support, all formatted with the specified ChatColor.</p>
     *
     * <pre><code>
     * logger.dataClientMessage(ChatColor.RED, true, true, "Error connecting to server", "Retrying in 10 seconds...");
     * </code></pre>
     *
     * @param color the ChatColor to apply to the message
     * @param headerAndFooter whether to include header and footer spacers
     * @param title whether to include the title line
     * @param messages the additional message lines
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger dataClientMessage(ChatColor color, boolean headerAndFooter, boolean title, String... messages) {
        String[] finalMessage = new String[4 + messages.length];
        if (headerAndFooter)
            finalMessage[0] = getColoredSpacer(color);
        else
            finalMessage[0] = "";
        if (title)
            finalMessage[1] = getColoredMessage(color, "Java Socket Bungeecord Data Sync System (short: JSBDSS):");
        else
            finalMessage[1] = "";
        int count = 0;
        for (String message : messages) {
            finalMessage[2 + count] = getColoredMessage(color, message);
            count++;
        }
        if (headerAndFooter)
            finalMessage[2 + messages.length] = getColoredSpacer(color);
        else
            finalMessage[3 + messages.length] = "";
        addMessageToQueue(finalMessage);
        return this;
    }

    /**
     * <p>Adds a raw message to the queue without formatting or prefixing.</p>
     *
     * <pre><code>
     * logger.rawMessage("This is a raw log message");
     * </code></pre>
     *
     * @param message the raw message content to log
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger rawMessage(String message) {
        addMessageToQueue(message);
        return this;
    }

    /**
     * <p>Adds multiple messages to the queue, automatically formatting each message
     * with the configured message format and language manager prefix.</p>
     *
     * <pre><code>
     * logger.messages("Plugin enabled", "Loading configurations", "Initialization complete");
     * </code></pre>
     *
     * @param messages the message content array to log
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger messages(String... messages) {
        for (String message : messages)
            addMessageToQueue(Utils.format(null, getMessageWithFormat(message),
                    CoolStuffLib.getLib().getLanguageManager().getPrefix() != null ? CoolStuffLib.getLib().getLanguageManager().getPrefix() : "[CoolStuffLib]"));
        return this;
    }

    /**
     * <p>Adds multiple raw messages to the queue without formatting or prefixing.</p>
     *
     * <pre><code>
     * logger.rawMessages("Raw message 1", "Raw message 2", "Raw message 3");
     * </code></pre>
     *
     * @param messages the raw message content array to log
     * @return this StartUpLogger instance for method chaining
     */
    public StartUpLogger rawMessages(String... messages) {
        addMessageToQueue(messages);
        return this;
    }

    private String getMessageWithFormat(String message) {
        String MESSAGE_FORMAT = CoolStuffLib.getLib().getJavaPluginUsingLib().getConfig().getString("Plugin.StartUpLogger.Message_Format",
                "|------------------------------------------------------------------|");
        final int messageSpacerLength = MESSAGE_FORMAT.length();
        final int messageLength = message.replaceAll("§([a-fA-F0-9]|r|l|m|n|o|k)", "").length();

        if (messageLength > messageSpacerLength - 2) return message;

        final int partLength = (messageSpacerLength - messageLength) / 2;

        final String startPart = MESSAGE_FORMAT.substring(0, partLength);
        final String endPart = MESSAGE_FORMAT.substring(messageSpacerLength - partLength, messageSpacerLength);

        return startPart + message + endPart;
    }

    /**
     * <p>Adds messages to the internal queue for asynchronous processing.</p>
     *
     * <pre><code>
     * logger.addMessageToQueue("Message 1", "Message 2", "Message 3");
     * </code></pre>
     *
     * @param message the message array to add to the queue
     */
    public void addMessageToQueue(String... message) {
        if (enabled) {
            messageQueue.add(message);
        }
    }

    /**
     * <p>Adds a single message to the queue for immediate processing or adds to queue based on system state.</p>
     *
     * <pre><code>
     * logger.addMessageToQueue("Single log message");
     * </code></pre>
     *
     * @param message the single message to add to the queue
     */
    public void addMessageToQueue(String message) {
        if (enabled) {
            sender.sendMessage(message);
        }
    }

    /**
     * <p>Enables the message processing system and resumes the message queue thread.</p>
     *
     * <pre><code>
     * logger.enableMessageSystem();
     * logger.message("System re-enabled");
     * </code></pre>
     */
    public void enableMessageSystem() {
        if (isMessageSystemEnabled()) return;
        enabled = true;
        messageQueueThread.resume();
    }

    /**
     * <p>Checks if the message processing system is currently enabled.</p>
     *
     * <pre><code>
     * if (logger.isMessageSystemEnabled()) {
     *     logger.message("System is active");
     * }
     * </code></pre>
     *
     * @return true if the message system is enabled, false otherwise
     */
    public boolean isMessageSystemEnabled() {
        return enabled;
    }

    /**
     * <p>Disables the message processing system and suspends the message queue thread.</p>
     *
     * <pre><code>
     * logger.disableMessageSystem();
     * // No messages will be processed until re-enabled
     * </code></pre>
     */
    public void disableMessageSystem() {
        if (!isMessageSystemEnabled()) return;
        enabled = false;
        messageQueueThread.suspend();
    }
}