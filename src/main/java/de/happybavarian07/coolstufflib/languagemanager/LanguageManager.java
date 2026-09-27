package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.languagemanager.expressionengine.ExpressionEngine;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.ExpressionEnginePool;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.conditions.HeadMaterialCondition;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.FunctionCall;
import de.happybavarian07.coolstufflib.languagemanager.expressionengine.interfaces.MaterialCondition;
import de.happybavarian07.coolstufflib.utils.Head;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import de.happybavarian07.coolstufflib.service.api.Service;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;

/**
 * LanguageManager class.
 */
public class LanguageManager implements Service {
    private final UUID serviceId = UUID.randomUUID();
    private static Logger logger;
    private final JavaPlugin plugin;
    private final File langFolder;
    private final String resourceDirectory;
    private final Map<String, LanguageFile> registeredLanguages;
    private final Map<String, Placeholder> placeholders;
    private final Map<String, LanguageCache> languageCaches; // New map for LanguageCache
    private final Map<String, Map<String, Map.Entry<Object, Integer>>> playerPathVariables = new HashMap<>();
    private final ExpressionEnginePool expressionEnginePool;
    private String prefix;
    private String currentLangName;
    private LanguageFile currentLang;
    private PerPlayerLanguageHandler playerLanguageHandler;
    private volatile List<String> lastReport = List.of();

    // TODO LanguageManager Menu Item Identification Optimization: inside E:\InteliJ Programs\CoolStuffLib\Menu_Item_ID_System.md


    /**
     * Constructs a new LanguageManager object.
     *
     * @param plugin            The JavaPlugin instance
     * @param langFolder        The folder where language files are stored
     * @param resourceDirectory The resource directory of the plugin
     * @param prefix            The prefix for the language files
     */
    public LanguageManager(JavaPlugin plugin, File langFolder, String resourceDirectory, String prefix) {
        this.prefix = prefix;
        this.plugin = plugin;
        this.langFolder = langFolder;
        this.resourceDirectory = resourceDirectory;
        this.registeredLanguages = new LinkedHashMap<>();
        this.placeholders = Collections.synchronizedMap(new LinkedHashMap<>());
        this.languageCaches = new HashMap<>();
        // Initialize default engine for current language
        ExpressionEngine defaultEngine = new ExpressionEngine();
        registerHeadFunction(defaultEngine);
        registerLangFunction(defaultEngine);
        this.expressionEnginePool = new ExpressionEnginePool("default", defaultEngine);
    }

    public static Logger getLogger() {
        if (logger == null) {
            logger = Logger.getLogger("LanguageManager");
        }
        return logger;
    }

    // Adds a new engine for a language and registers HEAD instantly
    private void addEngineForLanguage(String languageName, boolean headFunction, boolean langFunction) {
        ExpressionEngine engine = new ExpressionEngine();
        if (headFunction) registerHeadFunction(engine);
        if (langFunction) registerLangFunction(engine);
        expressionEnginePool.addEngineForLanguage(languageName, engine);
    }

    // Register the HEAD function for an engine
    private void registerHeadFunction(ExpressionEngine engine) {
        engine.registerFunction("HEAD", (interpreter, args, type) -> {
            if (args.size() != 1) throw new RuntimeException("HEAD function expects exactly 1 argument (head name)");
            String headName = args.get(0).toString();
            return "HEAD(" + headName + ")";
        }, "string");
        engine.registerFunction("HEAD_TEXTURE", (interpreter, args, type) -> {
            if (args.size() != 1) throw new RuntimeException("HEAD_TEXTURE function expects exactly 1 argument (head texture)");
            String headName = args.get(0).toString();
            return "HEAD_TEXTURE(" + headName + ")";
        }, "string");
        engine.registerFunction("HEAD_OBJECT", (interpreter, args, type) -> {
            if (args.size() != 1) throw new RuntimeException("HEAD_OBJECT function expects exactly 1 argument (csl head enum name)");
            String headName = args.get(0).toString();
            return "HEAD_OBJECT(" + headName + ")";
        }, "string");
    }

    private void registerLangFunction(ExpressionEngine engine) {
        engine.registerFunction("lang", (interpreter, args, type) -> {
            if (args == null || args.size() != 1) throw new RuntimeException("lang(key) expects 1 argument");
            Object key = args.get(0);
            if (key == null) return "";
            return getCustomObject(key.toString(), null, "", false);
        }, "string", new Class<?>[]{String.class}, String.class);
    }

    private void handleVariablesSection(Player player, boolean clearBefore) {
        LanguageFile langFile = getLangOrPlayerLang(true, getCurrentLangName(), player);
        handleVariablesSection(langFile, clearBefore);
    }

    /**
     * <p>Adds a variable to the specified {@link ExpressionEngine}.</p>
     * <pre><code>
     * languageManager.addVariableForEngine(engine, "key", value);
     * </code></pre>
     *
     * @param engine the expression engine to add the variable to
     * @param key    the variable name
     * @param value  the variable value
     */
    public void addVariableForEngine(ExpressionEngine engine, String key, Object value) {
        addVariableForEngine(engine, key, value, -1);
    }

    /**
     * <p>Adds a variable to the specified {@link ExpressionEngine} with a specified number of uses.</p>
     * <pre><code>
     * languageManager.addVariableForEngine(engine, "key", value, uses);
     * </code></pre>
     *
     * @param engine the expression engine to add the variable to
     * @param key    the variable name
     * @param value  the variable value
     * @param uses   the number of uses for the variable, -1 for unlimited
     */
    public void addVariableForEngine(ExpressionEngine engine, String key, Object value, int uses) {
        if (engine != null) {
            engine.setVariable(key, value, uses);
        }
    }

    /**
     * <p>Adds a variable with the given key and value to all registered {@link ExpressionEngine} instances.</p>
     * <pre><code>
     * languageManager.addVariableGlobally("key", value);
     * </code></pre>
     *
     * @param key   the variable name
     * @param value the variable value
     */
    public void addVariableGlobally(String key, Object value) {
        addVariableGlobally(key, value, -1);
    }

    /**
     * <p>Adds a variable with the given key, value, and number of uses to all registered {@link ExpressionEngine} instances.</p>
     * <pre><code>
     * languageManager.addVariableGlobally("key", value, uses);
     * </code></pre>
     *
     * @param key   the variable name
     * @param value the variable value
     * @param uses  the number of uses for the variable, -1 for unlimited
     */
    public void addVariableGlobally(String key, Object value, int uses) {
        getExpressionEnginePool().getEngineIterator().forEachRemaining(engine -> {
            if (engine != null) {
                engine.setVariable(key, value, uses);
            }
        });
    }

    /**
     * <p>Removes a variable with the given key from all registered {@link ExpressionEngine} instances.</p>
     * <pre><code>
     * languageManager.removeVariableGlobally("key");
     * </code></pre>
     *
     * @param key the variable name to remove
     */
    public void removeVariableGlobally(String key) {
        getExpressionEnginePool().getEngineIterator().forEachRemaining(engine -> {
            if (engine != null) {
                engine.removeVariable(key);
            }
        });
    }

    /**
     * <p>Removes a variable with the given key from the specified {@link ExpressionEngine}.</p>
     * <pre><code>
     * languageManager.removeVariableForEngine(engine, "key");
     * </code></pre>
     *
     * @param engine the expression engine to remove the variable from
     * @param key    the variable name to remove
     */
    public void removeVariableForEngine(ExpressionEngine engine, String key) {
        if (engine != null) {
            engine.removeVariable(key);
        }
    }

    private void handleVariablesSection(LanguageFile langFile, boolean clearBefore) {
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return;

        ConfigurationSection customSection = langConfig.getConfig().getConfigurationSection("CustomVariables");
        if (customSection == null) return;

        if (clearBefore) {
            ExpressionEngine engine = expressionEnginePool.getEngineForLanguage(langFile.getLangName());
            if (engine != null) {
                engine.clearVariables();
                engine.clearFunctions();
            }
        }

        for (String key : customSection.getKeys(false)) {
            Object value = customSection.get(key);

            ExpressionEngine engine = expressionEnginePool.getEngineForLanguage(langFile.getLangName());
            if (value instanceof String valueStr) {
                if (valueStr.startsWith("VARIABLE")) {
                    if (engine != null) {
                        engine.setVariable(key, valueStr.substring("VARIABLE".length()).trim());
                    }
                } else if (valueStr.startsWith("EXPR") || valueStr.startsWith("EXPRESSION")) {
                    String expr = valueStr.startsWith("EXPR") ?
                            valueStr.substring("EXPR".length()).trim() :
                            valueStr.substring("EXPRESSION".length()).trim();
                    if (engine != null) {
                        Object result = engine.parsePrimitive(expr);
                        engine.setVariable(key, result);
                    }
                } else if (valueStr.startsWith("FUNCTION")) {
                    String functionDef = valueStr.substring("FUNCTION".length()).trim();
                    if (engine != null) {
                        engine.getFunctionManager().registerFunction(functionDef);
                    }
                } else if (valueStr.contains("${") && valueStr.contains("}")) {
                    String parsedExpression = Utils.format(null, valueStr, prefix);
                    if (engine != null) {
                        engine.setVariable(key, parsedExpression);
                    }
                } else {
                    if (engine != null) {
                        engine.setVariable(key, valueStr);
                    }
                }
            } else {
                if (engine != null) {
                    engine.setVariable(key, value);
                }
            }
        }
    }


    /**
     * Sets an expression variable for a specific player and path.
     * These variables will be added to the parser when getItem is called with matching player and path.
     *
     * @param playerUUID The UUID of the player this variable is for
     * @param path       The path this variable is associated with
     * @param key        The variable key
     * @param value      The variable value
     */
    public void setPathExpressionVariable(String playerUUID, String path, String key, Object value, int uses) {
        String mapKey = playerUUID + ":" + path;
        if (!playerPathVariables.containsKey(mapKey)) {
            playerPathVariables.put(mapKey, new HashMap<>());
        }
        playerPathVariables.get(mapKey).put(key, new AbstractMap.SimpleEntry<>(value, uses));
    }

    /**
     * Removes an expression variable for a specific player and path.
     *
     * @param playerUUID The UUID of the player this variable is for
     * @param path       The path this variable is associated with
     * @param key        The variable key to remove
     */
    public void removePathExpressionVariable(String playerUUID, String path, String key) {
        String mapKey = playerUUID + ":" + path;
        if (playerPathVariables.containsKey(mapKey)) {
            playerPathVariables.get(mapKey).remove(key);
            if (playerPathVariables.get(mapKey).isEmpty()) {
                playerPathVariables.remove(mapKey);
            }
        }
    }

    /**
     * Removes all expression variables for a specific player and path.
     *
     * @param playerUUID The UUID of the player
     * @param path       The path
     */
    public void clearPathExpressionVariables(String playerUUID, String path) {
        String mapKey = playerUUID + ":" + path;
        playerPathVariables.remove(mapKey);
    }

    /**
     * Gets an expression variable for a specific player and path.
     *
     * @param playerUUID The UUID of the player
     * @param path       The path
     * @param key        The variable key
     * @return The variable value or null if not found
     */
    public Object getPathExpressionVariable(String playerUUID, String path, String key) {
        String mapKey = playerUUID + ":" + path;
        if (playerPathVariables.containsKey(mapKey)) {
            return playerPathVariables.get(mapKey).get(key);
        }
        return null;
    }

    /**
     * Applies path expression variables for a specific player and path.
     * This method will add the variables to the global expression engine pool.
     *
     * @param player The player for whom to apply the path expression variables
     * @param path   The path associated with the variables
     */
    private void applyPathExpressionVariables(Player player, String path) {
        if (player == null) return;

        String playerUUID = player.getUniqueId().toString();
        String mapKey = playerUUID + ":" + path;

        if (playerPathVariables.containsKey(mapKey)) {
            Map<String, Map.Entry<Object, Integer>> variables = playerPathVariables.get(mapKey);
            for (Map.Entry<String, Map.Entry<Object, Integer>> entry : variables.entrySet()) {
                addVariableGlobally(entry.getKey(), entry.getValue().getKey(), entry.getValue().getValue());
            }
        }
    }

    /**
     * Retrieves the ExpressionEnginePool associated with this LanguageManager.
     *
     * @return The ExpressionEnginePool associated with this LanguageManager.
     */
    public Object getExpressionVariable(String key, boolean peek) {
        return peek ? getExpressionEnginePool().peekVariable(key) : getExpressionEnginePool().getVariable(key);
    }

    /**
     * Retrieves the prefix of the LanguageManager.
     *
     * @return The prefix of the LanguageManager.
     */
    public String getPrefix() {
        return prefix;
    }

    /**
     * Provides the ability to set the prefix for the LanguageManager.
     *
     * @param prefix The prefix to be set for the LanguageManager.
     */
    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    /**
     * Gets the PerPlayerLanguageHandler associated with this LanguageManager.
     * <p>
     * Returns the PerPlayerLanguageHandler associated with this LanguageManager.
     *
     * @return The PerPlayerLanguageHandler associated with this LanguageManager.
     */
    public PerPlayerLanguageHandler getPLHandler() {
        return playerLanguageHandler;
    }

    /**
     * Provides the ability to set the PerPlayerLanguageHandler for the
     * LanguageManager.
     *
     * @param playerLanguageHandler The PerPlayerLanguageHandler to be set.
     */
    public void setPLHandler(PerPlayerLanguageHandler playerLanguageHandler) {
        this.playerLanguageHandler = playerLanguageHandler;
    }

    /**
     * Gets the language folder.
     * <p>
     * Returns the File object representing the language folder.
     *
     * @return The language folder.
     */
    public File getLangFolder() {
        return langFolder;
    }

    /**
     * Retrieves the resource directory of the LanguageManager.
     *
     * @return The resource directory of the LanguageManager.
     */
    public String getResourceDirectory() {
        return resourceDirectory;
    }

    /**
     * Retrieves the registered languages.
     *
     * @return A map of the registered languages, with the language name as the key and the language file as the value.
     */
    public Map<String, LanguageFile> getRegisteredLanguages() {
        return registeredLanguages;
    }

    /**
     * Retrieves the name of the current language.
     *
     * @return The name of the current language.
     */
    public String getCurrentLangName() {
        return currentLangName;
    }

    /**
     * Gets the current language file.
     * <p>
     * Returns the LanguageFile object representing the current language.
     *
     * @return The current language file.
     */
    public LanguageFile getCurrentLang() {
        return currentLang;
    }

    /**
     * Sets the current language to the specified language file.
     *
     * @param currentLang The language file to set as the current language.
     * @param log         Whether or not to log the change.
     * @throws NullPointerException If the language file is not found.
     */
    public void setCurrentLang(LanguageFile currentLang, boolean log) throws NullPointerException {
        if (currentLang == null) {
            List<Map.Entry<String, LanguageFile>> list = new ArrayList<>(registeredLanguages.entrySet());
            Map.Entry<String, LanguageFile> firstInsertedEntry = list.get(0);
            this.currentLang = firstInsertedEntry.getValue();
            this.currentLangName = firstInsertedEntry.getValue().getLangName();
            throw new NullPointerException("Language not found!");
        } else {
            this.currentLangName = currentLang.getLangName();
            this.currentLang = currentLang;
        }
        handleVariablesSection(currentLang, true);
        if (log)
            getLogger().log(Level.INFO, "Current Language: " + currentLangName);
    }

    /**
     * Adds languages to the list.
     *
     * @param log Whether to log the language registration or not.
     */
    public void addLanguagesToList(boolean log) {
        Set<String> names = new TreeSet<>();
        File[] files = langFolder.listFiles();
        if (files != null) {
            for (File file : files) {
                String name = file.getName();
                if (name.startsWith("_") || name.startsWith(".")) continue;
                if (file.isDirectory()) names.add(name);
                else if (name.endsWith(".yml")) names.add(name.substring(0, name.length() - 4));
            }
        }
        for (String name : names) {
            if (name.equals("default") || registeredLanguages.containsKey(name)) continue;
            LanguageFile languageFile = new LanguageFile(langFolder, resourceDirectory, name);
            if (log) getLogger().log(Level.INFO, "Language: " + languageFile.getLangFile() + " successfully registered!");
            registeredLanguages.put(name, languageFile);
            languageCaches.put(name, new LanguageCache(name));
            addEngineForLanguage(name, true, true);
        }
        logReports();
    }

    /**
     * LanguageManager.updateLangFiles() updates the registered language files by
     * merging any keys that are in the jar defaults but missing from the owner's
     * files, using the language set by Plugin.languageForUpdates (or en).
     */
    public void updateLangFiles() {
        String updateLanguage = updateLanguage();
        for (LanguageFile languageFile : getRegisteredLanguages().values()) {
            languageFile.getLangConfig().update(updateLanguage);
        }
        logReports();
    }

    private String updateLanguage() {
        try {
            String language = plugin.getConfig().getString("Plugin.languageForUpdates");
            return language == null || language.isBlank() ? "en" : language;
        } catch (RuntimeException e) {
            return "en";
        }
    }

    /** Logs what changed since the last report (migration, added keys, problems) and returns the lines. */
    public List<String> logReports() {
        List<String> lines = new ArrayList<>();
        for (LanguageFile languageFile : registeredLanguages.values()) {
            for (String line : languageFile.getLangConfig().drainReport()) {
                lines.add(line);
                getLogger().log(line.startsWith("Problem") || line.startsWith("Could not") ? Level.WARNING : Level.INFO, line);
            }
        }
        lastReport = List.copyOf(lines);
        return lines;
    }

    public List<String> getLastReport() {
        return lastReport;
    }

    /**
     * Reloads all languages and updates the language files.
     *
     * @param messageReceiver The command sender to send the message to.
     * @param log             Whether to log the action or not.
     */
    public void reloadLanguages(CommandSender messageReceiver, Boolean log) {
        addLanguagesToList(log);
        updateLangFiles();
        for (String langFiles : registeredLanguages.keySet()) {
            if (messageReceiver != null) {
                addPlaceholder(PlaceholderType.MESSAGE, "%language%", getLang(langFiles, true).getLangFile(), true);
                messageReceiver.sendMessage(getMessage("Player.General.ReloadedLanguageFile", messageReceiver instanceof Player ? (Player) messageReceiver : null, true));
            }
        }
        setCurrentLang(getLang(plugin.getConfig().getString("Plugin.language"), true), log);
    }

    /**
     * Adds a new language to the list of registered languages.
     *
     * @param langFile The language file to be added.
     * @param langName The name of the language to be added.
     */
    public void addLang(LanguageFile langFile, String langName) {
        if (registeredLanguages.containsKey(langName) || langName.equals("default"))
            return;
        registeredLanguages.put(langName, langFile);
        languageCaches.put(langName, new LanguageCache(langName));
        addEngineForLanguage(langName, true, true);
        logReports();
        getLogger().log(Level.INFO, "Language: " + langFile.getLangFile() + " successfully registered!");
    }

    /**
     * Gets the LanguageFile object associated with the given language name.
     *
     * @param langName       The name of the language to get the LanguageFile object for.
     * @param throwException Whether or not to throw an exception if the language is
     *                       not found.
     * @return The LanguageFile object associated with the given language name, or null
     * if the language is not found and throwException is false.
     * @throws NullPointerException If the language is not found and throwException is
     *                              true.
     */
    public LanguageFile getLang(String langName, boolean throwException) throws NullPointerException {
        if (!registeredLanguages.containsKey(langName))
            if (throwException)
                throw new NullPointerException("Language: " + langName + " not found!");
            else
                return null;
        return registeredLanguages.get(langName);
    }

    /**
     * Removes a language from the list of registered languages.
     *
     * @param langName The name of the language to be removed.
     */
    public void removeLang(String langName) {
        if (!registeredLanguages.containsKey(langName))
            return;
        registeredLanguages.remove(langName);
    }

    /**
     * <p>Adds a new placeholder for message replacement.</p>
     *
     * <pre><code>lgm.addPlaceholder(PlaceholderType.MESSAGE, "%player%", player.getName(), false);</code></pre>
     *
     * @param type        The placeholder type
     * @param key         The placeholder key
     * @param value       The placeholder value
     * @param resetBefore Whether to reset existing placeholders before adding
     */
    public void addPlaceholder(PlaceholderType type, String key, Object value, boolean resetBefore) {
        synchronized (placeholders) {
            if (resetBefore) resetPlaceholders(type, null);
            if (!placeholders.containsKey(key))
                placeholders.put(key, new Placeholder(key, value, type));
            else
                placeholders.replace(key, placeholders.get(key), new Placeholder(key, value, type));
        }
    }

    /**
     * Adds the given placeholders to the LanguageManager. If resetBefore is true, all
     * existing placeholders will be reset before adding the new ones.
     *
     * @param placeholders The placeholders to add
     * @param resetBefore  Whether to reset existing placeholders before adding the new
     *                     ones
     */
    public void addPlaceholders(Map<String, Placeholder> placeholders, boolean resetBefore) {
        synchronized (this.placeholders) {
            if (resetBefore) resetPlaceholders(PlaceholderType.ALL, null);
            this.placeholders.putAll(placeholders);
        }
    }

    /**
     * <p>Removes a placeholder by key and type.</p>
     *
     * <pre><code>lgm.removePlaceholder(PlaceholderType.MESSAGE, "%player%");</code></pre>
     *
     * @param type The placeholder type
     * @param key  The placeholder key
     */
    public void removePlaceholder(PlaceholderType type, String key) {
        synchronized (placeholders) {
            if (!placeholders.containsKey(key)) return;
            if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                return;

            placeholders.remove(key);
        }
    }

    /**
     * Removes the specified placeholders of the given type from the LanguageManager.
     *
     * @param type The type of placeholder to remove
     * @param keys The keys of the placeholders to remove
     */
    public void removePlaceholders(PlaceholderType type, List<String> keys) {
        synchronized (placeholders) {
            for (String key : keys) {
                if (!placeholders.containsKey(key)) continue;
                if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                    continue;

                this.placeholders.remove(key);
            }
        }
    }

    /**
     * Removes all placeholders of the specified type, excluding the keys specified in
     * the excludeKeys list. If the excludeKeys list is null, all placeholders of the
     * specified type will be removed.
     *
     * @param type        The type of placeholders to reset.
     * @param excludeKeys A list of keys to exclude from removal, or null to remove all placeholders of the specified type.
     */
    public void resetPlaceholders(PlaceholderType type, @Nullable List<String> excludeKeys) {
        synchronized (placeholders) {
            List<String> keysToRemove = new ArrayList<>();
            for (String key : placeholders.keySet()) {
                if (excludeKeys != null && excludeKeys.contains(key)) continue;
                if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                    continue;

                keysToRemove.add(key);
            }
            removePlaceholders(type, keysToRemove);
        }
    }

    /**
     * Resets specific placeholders of a given type. If includeKeys is provided, only those keys will be reset.
     * It iterates through the placeholders and checks if the type matches the given type or is of type ALL.
     * If it does, the key is added to the list of keys to remove. Finally, the method calls removePlaceholders
     * to remove the placeholders.
     *
     * @param type        The type of placeholders to reset.
     * @param includeKeys A list of keys to include in removal or null to reset all placeholders of the specified type.
     */
    public void resetSpecificPlaceholders(PlaceholderType type, @Nullable List<String> includeKeys) {
        synchronized (placeholders) {
            List<String> keysToRemove = new ArrayList<>();
            for (String key : placeholders.keySet()) {
                if (includeKeys != null && !includeKeys.contains(key)) continue;
                if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                    continue;

                keysToRemove.add(key);
            }
            removePlaceholders(type, keysToRemove);
        }
    }

    /**
     * Retrieves a map of all the placeholders in the LanguageManager.
     *
     * @return A map of all the placeholders in the LanguageManager.
     */
    public Map<String, Placeholder> getPlaceholders() {
        return placeholders;
    }

    /**
     * Retrieves the {@link LanguageCache} object associated with the given language name.
     *
     * @param langName The name of the language to get the {@link LanguageCache} object for.
     * @return The {@link LanguageCache} object associated with the given language name.
     */
    public LanguageCache getLanguageCache(String langName) {
        return languageCaches.get(langName);
    }

    /**
     * Gets a list of placeholder keys found in a given message of a specified type.
     * <p>
     * Checks each key in the placeholders map to see if it is present in the message
     * and if its type matches the specified type or is of type {@link PlaceholderType}.ALL.
     * If both conditions are met, the key is added to the list of keys.
     *
     * @param message The message to search for placeholder keys
     * @param type    The type of placeholder to search for
     * @return A list of placeholder keys found in the message of the specified type
     */
    private List<String> getPlaceholderKeysInMessage(String message, PlaceholderType type) {
        synchronized (placeholders) {
            List<String> keys = new ArrayList<>();
            for (String key : placeholders.keySet()) {
                if (!message.contains(key)) continue;
                if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                    continue;

                keys.add(key);
            }
            return keys;
        }
    }

    /**
     * Replaces placeholders in the given message with their corresponding values.
     *
     * @param type    The type of placeholder to replace.
     * @param message The message to replace placeholders in.
     * @return The message with placeholders replaced.
     */
    public String replacePlaceholders(PlaceholderType type, String message) {
        synchronized (placeholders) {
            for (String key : placeholders.keySet()) {
                if (!placeholders.get(key).type().equals(type) && !placeholders.get(key).type().equals(PlaceholderType.ALL))
                    continue;

                message = placeholders.get(key).replace(message);
            }
            return message;
        }
    }

    /**
     * Replaces placeholders in an {@link ItemStack} with the corresponding values.
     *
     * @param player The {@link Player} to use for placeholder replacements.
     * @param item   The {@link ItemStack} to replace placeholders in.
     * @return The {@link ItemStack} with placeholders replaced.
     */
    public ItemStack replacePlaceholders(Player player, ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        List<String> lore = meta.getLore();
        List<String> loreWithPlaceholders = new ArrayList<>();
        assert lore != null;
        for (String s : lore) {
            String temp = replacePlaceholders(PlaceholderType.ITEM, s);
            loreWithPlaceholders.add(Utils.format(player, temp, prefix));
        }
        meta.setLore(loreWithPlaceholders);
        meta.setDisplayName(replacePlaceholders(PlaceholderType.ITEM, Utils.format(player, meta.getDisplayName(), prefix)));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Replaces placeholders in an ItemStack with the given Placeholders.
     *
     * @param player       The player to format the ItemStack for.
     * @param item         The ItemStack to replace the placeholders in.
     * @param placeholders The Placeholders to replace.
     * @return The ItemStack with the placeholders replaced.
     */
    public ItemStack replacePlaceholders(Player player, ItemStack item, Map<String, Placeholder> placeholders) {
        ItemMeta meta = item.getItemMeta();
        assert meta != null;
        List<String> lore = meta.getLore();
        List<String> loreWithPlaceholders = new ArrayList<>();
        assert lore != null;
        for (String s : lore) {
            String temp = replacePlaceholders(s, placeholders);
            loreWithPlaceholders.add(Utils.format(player, temp, prefix));
        }
        meta.setLore(loreWithPlaceholders);
        meta.setDisplayName(replacePlaceholders(Utils.format(player, meta.getDisplayName(), prefix), placeholders));
        item.setItemMeta(meta);
        return item;
    }

    /**
     * Replaces placeholders in the given message with their corresponding values.
     *
     * @param message      The message to replace placeholders in.
     * @param placeholders A map of placeholder names to their corresponding Placeholder objects.
     * @return The message with all placeholders replaced.
     */
    public String replacePlaceholders(String message, Map<String, Placeholder> placeholders) {
        for (String key : placeholders.keySet()) {
            message = placeholders.get(key).replace(message);
        }
        return message;
    }

    /**
     * Returns a new empty {@link HashMap} of {@link String} and {@link Placeholder}.
     *
     * @return a new empty {@link HashMap} of {@link String} and {@link Placeholder}.
     */
    public Map<String, Placeholder> getNewPlaceholderMap() {
        return new HashMap<>();
    }

    /**
     * This method retrieves a LanguageFile object from the LanguageManager. If the
     * currentLang parameter is true, the current language is returned. If the
     * langName parameter is not null, the language specified by the langName
     * parameter is returned. If the player parameter is not null, the language
     * associated with the player is returned. If the langName parameter is null and
     * the currentLang parameter is false, the current language is returned.
     *
     * @param currentLang Whether to return the current language.
     * @param langName    The name of the language to return.
     * @param player      The player to return the language for.
     * @return The LanguageFile object.
     */
    public LanguageFile getLangOrPlayerLang(boolean currentLang, String langName, @Nullable Player player) {
        if (player == null && currentLang) return getCurrentLang();
        if (player == null) return getLang(langName, true);
        if (langName == null && currentLang) return getCurrentLang();
        if (playerLanguageHandler == null) return currentLang ? getCurrentLang() : getLang(langName, true);

        LanguageFile lang = playerLanguageHandler.getPlayerLanguage(player.getUniqueId());
        if (lang == null) {
            if (currentLang) return getCurrentLang();
            else return getLang(langName, true);
        }
        return lang;
    }

    public <T> T getObjectFromLanguageCacheOrConfig(String path, String langName, Class<T> clazz) {
        LanguageFile langFile = getLang(langName, true);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return getDefaultInstance(clazz);
        Object configObject = langConfig.getConfig().get(path);
        if (configObject == null) return getDefaultInstance(clazz);
        try {
            return clazz.cast(configObject);
        } catch (ClassCastException e) {
            return null;
        }
    }

    private <T> T getDefaultInstance(Class<T> clazz) {
        if (clazz == Boolean.class) {
            return clazz.cast(Boolean.FALSE);
        } else if (clazz == Integer.class) {
            return clazz.cast(Integer.valueOf("0")); // we have to do this to get around the cast method complaining about needing an object to cast.
        } else if (clazz == Double.class) {
            return clazz.cast(Double.valueOf("0.0")); // we have to do this to get around the cast method complaining about needing an object to cast.
        } else if (clazz == String.class) {
            return clazz.cast("");
        } else if (clazz == List.class) {
            return clazz.cast(new ArrayList<>());
        } else if (clazz == Map.class) {
            return clazz.cast(new HashMap<>());
        }
        return null;
    }

    /** The first language along language → LanguageParent → server language that has the key; {@code start} if none. */
    LanguageFile languageWith(String fullPath, LanguageFile start) {
        Set<String> seen = new HashSet<>();
        LanguageFile language = start;
        while (language != null && seen.add(language.getLangName())) {
            FileConfiguration config = language.getLangConfig().getConfig();
            if (config.contains(fullPath)) return language;
            String parent = config.getString("LanguageParent");
            language = parent == null ? null : getLang(parent, false);
        }
        LanguageFile server = getCurrentLang();
        if (server != null && seen.add(server.getLangName()) && server.getLangConfig().getConfig().contains(fullPath)) return server;
        return start;
    }

    static String stripRoot(String path, String root) {
        return path.startsWith(root + ".") ? path.substring(root.length() + 1) : path;
    }

    /** A message's text: a plain value, the plural variant for {@code count}, or the {@code text} of a rich entry. */
    static @Nullable String textOf(ConfigurationSection config, String fullPath, @Nullable Long count) {
        Object value = config.get(fullPath);
        if (value == null || value instanceof List<?>) return null;
        if (!(value instanceof ConfigurationSection section)) return value.toString();
        if (count != null) {
            String variant = count == 0 && section.isString("zero") ? "zero" : count == 1 && section.isString("one") ? "one" : "other";
            if (section.isString(variant)) return section.getString(variant);
        }
        if (section.isString("text")) return section.getString("text");
        return section.isString("other") ? section.getString("other") : null;
    }

    String applyLocal(String text, Map<String, ?> local, @Nullable Long count, PlaceholderType type) {
        if (count != null && !local.containsKey("%count%")) text = new Placeholder("%count%", String.valueOf(count), type).replace(text);
        for (Map.Entry<String, ?> entry : local.entrySet()) {
            text = new Placeholder(entry.getKey(), String.valueOf(entry.getValue()), type).replace(text);
        }
        return text;
    }

    String renderMessage(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, @Nullable Long count, boolean resetAfter) {
        path = stripRoot(path, "Messages");
        applyPathExpressionVariables(player, path);
        String fullPath = "Messages." + path;
        LanguageFile langFile = languageWith(fullPath, start);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return "null config";
        String raw = textOf(langConfig.getConfig(), fullPath, count);
        if (raw == null) return "null path: " + fullPath;
        return renderText(raw, fullPath, player, langFile.getLangName(), local, count, resetAfter);
    }

    String renderText(String raw, String fullPath, @Nullable Player player, String langName, Map<String, ?> local,
                      @Nullable Long count, boolean resetAfter) {
        String message = Utils.format(player, raw, prefix);
        message = applyLocal(message, local, count, PlaceholderType.MESSAGE);
        synchronized (placeholders) {
            if (!placeholders.isEmpty()) {
                List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(message, PlaceholderType.MESSAGE));
                message = replacePlaceholders(PlaceholderType.MESSAGE, message);
                if (resetAfter) resetSpecificPlaceholders(PlaceholderType.MESSAGE, includedKeys);
            }
        }
        return parseEmbeddedExpressions(message, player, langName);
    }

    /**
     * Gets a message from the specified path in the language file for the specified
     * player.
     *
     * @param path       The path of the message in the language file.
     * @param player     The player to get the message for.
     * @param resetAfter Whether or not to reset the message after it is retrieved.
     * @return The message from the specified path in the language file.
     */
    public String getMessage(String path, Player player, boolean resetAfter) {
        return getMessage(path, player, getCurrentLangName(), resetAfter);
    }

    /**
     * Gets a message from the language file.
     *
     * @param path       The path of the message in the language file.
     * @param player     The player to format the message for.
     * @param langName   The name of the language file to get the message from.
     * @param resetAfter Whether to reset the placeholders after the message is
     *                   retrieved.
     * @return The formatted message.
     */
    public String getMessage(String path, Player player, String langName, boolean resetAfter) {
        return renderMessage(path, player, getLangOrPlayerLang(true, langName, player), Map.of(), null, resetAfter);
    }

    /**
     * <p>Like {@link #getMessage(String, Player, boolean)}, but returns {@code fallback} (unformatted) when the
     * path does not exist in the language file instead of a "null path" marker.</p>
     */
    public String getMessageOrDefault(String path, Player player, String fallback, boolean resetAfter) {
        String message = getMessage(path, player, resetAfter);
        if (message == null || message.startsWith("null path:") || message.equals("null config")) return fallback;
        return message;
    }

    /**
     * Gets the permission message for a given permission.
     *
     * @param player     The player to get the message for.
     * @param permission The permission to get the message for.
     * @return The permission message.
     */
    public String getPermissionMessage(Player player, String permission) {
        addPlaceholder(PlaceholderType.MESSAGE, "%permission%", permission, true);
        return getMessage("Player.General.NoPermissions", player, true);
    }

    /**
     * Gets an item from the specified path in the language file for the specified
     * player.
     *
     * @param path       The path of the item in the language file.
     * @param player     The player to get the item for.
     * @param resetAfter Whether or not to reset the item after it has been retrieved.
     * @return The item from the specified path in the language file.
     */
    public ItemStack getItem(String path, Player player, boolean resetAfter) {
        return getItem(path, player, getCurrentLangName(), resetAfter);
    }

    public ItemStack getItem(String path, Player player, boolean resetAfter, MaterialCondition condition) {
        return getItem(path, player, getCurrentLangName(), resetAfter, condition);
    }

    public ItemStack getItem(String path, Player player, String langName, boolean resetAfter) {
        return getItem(path, player, langName, resetAfter, null);
    }

    /**
     * Retrieves an ItemStack based on the provided path, player, language, and reset flag.
     *
     * @param path       The path to the item configuration.
     * @param player     The player for whom the item is intended.
     * @param langName   The name of the language.
     * @param resetAfter A boolean flag indicating whether to reset placeholders after use.
     * @return An ItemStack based on the specified parameters.
     */
    public ItemStack getItem(String path, Player player, String langName, boolean resetAfter, MaterialCondition condition) {
        return renderItem(path, player, getLangOrPlayerLang(false, langName, player), Map.of(), resetAfter, condition);
    }

    ItemStack renderItem(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, boolean resetAfter,
                         @Nullable MaterialCondition condition) {
        path = stripRoot(path, "Items");
        LanguageFile langFile = languageWith("Items." + path, start);
        String langName = langFile.getLangName();
        LanguageConfig langConfig = langFile.getLangConfig();
        ItemStack error = new ItemStack(Material.BARRIER);
        ItemMeta errorMeta = error.getItemMeta();
        if (langConfig == null || langConfig.getConfig() == null) {
            assert errorMeta != null;
            errorMeta.setDisplayName("Language Config not found!");
            errorMeta.setLore(Arrays.asList("If this happens often,", "please report to the Discord"));
            error.setItemMeta(errorMeta);
            return error;
        }
        if (langConfig.getConfig().getString("Items." + path) == null || !langConfig.getConfig().contains("Items." + path)) {
            assert errorMeta != null;
            errorMeta.setDisplayName("Config Path not found!");
            errorMeta.setLore(Arrays.asList("If this happens often,", "please report to the Discord", "Path: Items." + path));
            error.setItemMeta(errorMeta);
            return error;
        }
        if (langConfig.getConfig().getBoolean("Items." + path + ".disabled", false) &&
                !Objects.equals(path, "General.DisabledItem")) {
            return renderItem("General.DisabledItem", player, start, local, false, null);
        }
        ItemStack item;

        applyPathExpressionVariables(player, path);
        if (condition instanceof HeadMaterialCondition headCondition) {
            if (headCondition.isHead()) {
                item = headCondition.getHead().getAsItem();
            } else {
                item = Utils.createSkull(headCondition.getHeadValue(), headCondition.getHeadValue(), headCondition.isTexture());
            }
        } else if (condition != null) {
            Material material = condition.getMaterial();
            if (material == null) {
                assert errorMeta != null;
                errorMeta.setDisplayName("Material not found! (" + langConfig.getConfig().getString("Items." + path + ".material") + ")");
                errorMeta.setLore(Arrays.asList("If this happens,", "please change the Material from this Item", "to something existing", "Path: Items." + path + ".material"));
                error.setItemMeta(errorMeta);
                return error;
            }
            item = new ItemStack(material);
        } else {
            String materialString = "";
            if (getObjectFromLanguageCacheOrConfig("Items." + path + ".material", langFile.getLangName(), String.class) != null) {
                materialString = getObjectFromLanguageCacheOrConfig("Items." + path + ".material", langFile.getLangName(), String.class);
            } else if (getObjectFromLanguageCacheOrConfig("Items." + path + ".material", langFile.getLangName(), List.class) != null) {
                List<?> materialList = getObjectFromLanguageCacheOrConfig("Items." + path + ".material", langFile.getLangName(), List.class);
                StringBuilder materialBuilder = new StringBuilder();
                for (Object materialObj : materialList) {
                    if (materialObj instanceof String materialStr) {
                        materialBuilder.append(materialStr).append("\n");
                    }
                }
                materialString = materialBuilder.toString().trim();
            }
            ItemStack headItem = parseMaterialStringToItem(materialString, player, langFile.getLangName());
            if (headItem == null) {
                assert errorMeta != null;
                errorMeta.setDisplayName("Head Item not found! (" + langConfig.getConfig().getString("Items." + path + ".material") + ")");
                errorMeta.setLore(Arrays.asList("If this happens,", "please change the Material from this Item", "to something existing", "Path: Items." + path + ".material"));
                error.setItemMeta(errorMeta);
                return error;
            }
            item = headItem;
        }
        String displayName = getObjectFromLanguageCacheOrConfig("Items." + path + ".displayName", langFile.getLangName(), String.class);
        List<String> lore = this.getObjectFromLanguageCacheOrConfig("Items." + path + ".lore", langFile.getLangName(), List.class);
        List<String> loreWithPlaceholders = new ArrayList<>();
        List<String> includedKeys = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        for (String s : lore == null ? List.<String>of() : lore) {
            String withLocal = applyLocal(s, local, null, PlaceholderType.ITEM);
            includedKeys.addAll(getPlaceholderKeysInMessage(withLocal, PlaceholderType.ITEM));
            String temp = replacePlaceholders(PlaceholderType.ITEM, withLocal);
            loreWithPlaceholders.add(Utils.format(player, temp, prefix));
        }
        assert meta != null;
        meta.setLore(loreWithPlaceholders);
        assert displayName != null;
        String formattedName = applyLocal(Utils.format(player, displayName, prefix), local, null, PlaceholderType.ITEM);
        includedKeys.addAll(getPlaceholderKeysInMessage(formattedName, PlaceholderType.ITEM));
        meta.setDisplayName(replacePlaceholders(PlaceholderType.ITEM, formattedName));
        if (getObjectFromLanguageCacheOrConfig("Items." + path + ".enchanted", langFile.getLangName(), Boolean.class)) {
            meta.addEnchant(Enchantment.UNBREAKING, 1, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        if (resetAfter) resetSpecificPlaceholders(PlaceholderType.ITEM, includedKeys);
        return item;
    }

    /**
     * Gets a Item from the specified material string.
     *
     * @param materialString The material string to get the Material from.
     * @param player         The player to use for placeholder replacements.
     * @param langName       The language name to use for placeholder replacements.
     * @return The Item from the specified material string, or MHF_Question if the
     * material is invalid.
     */
    public ItemStack parseMaterialStringToItem(String materialString, Player player, String langName) {
        if (materialString == null) {
            return Utils.createSkull("MHF_Question", "MHF_Question", false);
        }

        if (materialString.startsWith("HEAD_OBJECT(") && materialString.endsWith(")")) {
            String headName = materialString.substring(5, materialString.length() - 1).trim();
            try {
                try {
                    Head head = Head.valueOf(headName);
                    return head.getAsItem();
                } catch (IllegalArgumentException e) {
                    return Utils.createSkull(headName, headName, false);
                }
            } catch (Exception e) {
                getLogger().warning("Invalid head: " + headName);
                return Utils.createSkull("MHF_Question", "MHF_Question", false);
            }
        } else if (materialString.startsWith("HEAD_TEXTURE(") && materialString.endsWith(")")) {
            String headTexture = materialString.substring(13, materialString.length() - 1).trim();
            try {
                return Utils.createSkull(headTexture, headTexture, true);
            } catch (Exception e) {
                getLogger().warning("Invalid head texture: " + headTexture);
                return Utils.createSkull("MHF_Question", "MHF_Question", false);
            }
        } else if (materialString.startsWith("HEAD(") && materialString.endsWith(")")) {
            String headValue = materialString.substring(11, materialString.length() - 1).trim();
            try {
                return Utils.createSkull(headValue, headValue, false);
            } catch (Exception e) {
                getLogger().warning("Invalid head value: " + headValue);
                return Utils.createSkull("MHF_Question", "MHF_Question", false);
            }
        }

        try {
            String expr = materialString;
            if ((expr.startsWith("EXPR(") && expr.endsWith(")")) || (expr.startsWith("EXPRESSION(") && expr.endsWith(")"))) {
                int open = expr.indexOf('(');
                expr = expr.substring(open + 1, expr.length() - 1).trim();
            }
            MaterialCondition cond = getExpressionEngineFor(player, langName).parse(expr, Material.BARRIER);
            if (cond instanceof HeadMaterialCondition headCondition) {
                if (headCondition.isHead()) {
                    return headCondition.getHead().getAsItem();
                } else {
                    return Utils.createSkull(headCondition.getHeadValue(), headCondition.getHeadValue().substring(0, 15), headCondition.isTexture());
                }
            } else if (cond != null) {
                Material material = cond.getMaterial();
                if (material == null) {
                    getLogger().warning("Invalid material condition: " + materialString);
                    return new ItemStack(Material.BARRIER);
                }
                return new ItemStack(material);
            } else {
                getLogger().warning("Invalid material condition: " + materialString);
                return new ItemStack(Material.BARRIER);
            }
        } catch (Exception e) {
            if (materialString.contains("if") || materialString.contains("else")) {
                getLogger().warning("Error parsing conditional expression: " + materialString);
                getLogger().warning(e.getMessage());
            }
        }

        try {
            Material material = Material.valueOf(materialString.toUpperCase(Locale.ROOT));
            if (material == Material.PLAYER_HEAD) {
                return Utils.createSkull("MHF_Question", "MHF_Question", false);
            }
            return new ItemStack(material);
        } catch (IllegalArgumentException e) {
            if (!materialString.equals("PLAYER_HEAD")) {
                getLogger().warning("Invalid material name: " + materialString);
            }
            return Utils.createSkull("MHF_Question", "MHF_Question", false);
        }
    }

    /**
     * Gets the menu title for the given path in the language specified by the given
     * language name.
     *
     * @param path   The path of the menu title to get.
     * @param player The player to get the language name from.
     * @return The menu title for the given path in the language specified by the given
     * language name.
     */
    public String getMenuTitle(String path, Player player) {
        return getMenuTitle(path, player, getCurrentLangName());
    }

    /**
     * Retrieves a menu title from the language file based on the provided path.
     * The language file can be either the specified language or the player's language.
     * The title is then formatted with the player's language and the prefix, and
     * placeholders are replaced in the title.
     *
     * @param path     The path to the menu title in the language configuration.
     * @param player   The player for whom the menu title is intended.
     * @param langName The name of the language to use.
     * @return The formatted menu title for the player.
     */
    public String getMenuTitle(String path, Player player, String langName) {
        return renderTitle(path, player, getLangOrPlayerLang(false, langName, player), Map.of());
    }

    String renderTitle(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local) {
        path = stripRoot(path, "MenuTitles");
        applyPathExpressionVariables(player, path);
        LanguageFile langFile = languageWith("MenuTitles." + path, start);
        String langName = langFile.getLangName();
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null) return "null config";
        if (langConfig.getConfig().getString("MenuTitles." + path) == null || !langConfig.getConfig().contains("MenuTitles." + path))
            return "null path: MenuTitles." + path;
        String title = applyLocal(getObjectFromLanguageCacheOrConfig("MenuTitles." + path, langName, String.class), local, null, PlaceholderType.MENUTITLE);
        List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(title, PlaceholderType.MENUTITLE));
        title = replacePlaceholders(PlaceholderType.MENUTITLE, title);
        resetSpecificPlaceholders(PlaceholderType.MENUTITLE, includedKeys);
        return parseEmbeddedExpressions(Utils.format(player, title, prefix), player, langName);
    }

    /**
     * Retrieve a custom object from the specified path.
     *
     * @param path         The path to the custom object.
     * @param player       The player to retrieve the custom object for, or null for global.
     * @param defaultValue The default value to return if the custom object is not found.
     * @param resetAfter   Whether to reset the custom object after retrieval.
     * @param <T>          The type of the custom object.
     * @return The custom object, or the default value if not found.
     */
    public <T> T getCustomObject(String path, @Nullable Player player, T defaultValue, boolean resetAfter) {
        return getCustomObject(path, player, getCurrentLangName(), defaultValue, resetAfter);
    }

    /**
     * Gets a custom object from the language file.
     *
     * @param path         The path to the object in the language file.
     * @param player       The player to use for placeholders.
     * @param langName     The language name to use.
     * @param defaultValue The default value to return if the object is not found.
     * @param resetAfter   Whether to reset the placeholders after getting the object.
     * @param <T>          The type of the object.
     * @return The object from the language file, or the default value if not found.
     */
    @SuppressWarnings("unchecked")
    public <T> T getCustomObject(String path, @Nullable Player player, String langName, T defaultValue, boolean resetAfter) {
        LanguageFile start = getLangOrPlayerLang(false, langName, player);
        LanguageFile langFile = languageWith(path, start);
        if (!langFile.getLangConfig().getConfig().contains(path) && !path.startsWith("Messages.")) {
            LanguageFile withRoot = languageWith("Messages." + path, start);
            if (withRoot.getLangConfig().getConfig().contains("Messages." + path)) {
                path = "Messages." + path;
                langFile = withRoot;
            }
        }
        langName = langFile.getLangName();
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return defaultValue;
        if (!langConfig.getConfig().contains(path))
            return defaultValue;
        T obj;
        try {
            obj = (T) getObjectFromLanguageCacheOrConfig(path, langName, defaultValue.getClass());
        } catch (ClassCastException e) {
            return defaultValue;
        }
        if (obj == null ||
                (obj instanceof String && ((String) obj).isEmpty()) ||
                (obj instanceof List && ((List<?>) obj).isEmpty()) ||
                (obj instanceof Map && ((Map<?, ?>) obj).isEmpty()))
            return defaultValue;

        if (obj instanceof String) {
            obj = (T) replacePlaceholders(PlaceholderType.CUSTOM, Utils.format(player, obj.toString(), prefix));
            if (resetAfter)
                resetSpecificPlaceholders(PlaceholderType.CUSTOM,
                        getPlaceholderKeysInMessage((String) langConfig.getConfig().get(path), PlaceholderType.CUSTOM));
            obj = (T) parseEmbeddedExpressions(obj.toString(), player, langName);
        }
        if (obj == null) obj = defaultValue;
        return obj;
    }

    public MessageBuilder message(String path) {
        return new MessageBuilder(this, path);
    }

    public ItemBuilder item(String path) {
        return new ItemBuilder(this, path);
    }

    public TitleBuilder title(String path) {
        return new TitleBuilder(this, path);
    }

    /** An explicit language wins over the viewer's language for the new API. */
    LanguageFile startFor(@Nullable String language, @Nullable Player viewer, boolean messages) {
        if (language != null) return getLang(language, true);
        return getLangOrPlayerLang(messages, getCurrentLangName(), viewer);
    }

    List<String> renderLines(String path, @Nullable Player player, LanguageFile start, Map<String, ?> local, @Nullable Long count) {
        String fullPath = "Messages." + stripRoot(path, "Messages");
        LanguageFile langFile = languageWith(fullPath, start);
        Object value = langFile.getLangConfig().getConfig().get(fullPath);
        if (!(value instanceof List<?> list)) return List.of(renderMessage(path, player, start, local, count, false));
        List<String> lines = new ArrayList<>();
        for (Object line : list) lines.add(renderText(String.valueOf(line), fullPath, player, langFile.getLangName(), local, count, false));
        return lines;
    }

    void send(String path, CommandSender sender, LanguageFile start, Map<String, ?> local, @Nullable Long count) {
        Player player = sender instanceof Player p ? p : null;
        String fullPath = "Messages." + stripRoot(path, "Messages");
        LanguageFile langFile = languageWith(fullPath, start);
        ConfigurationSection rich = langFile.getLangConfig().getConfig().getConfigurationSection(fullPath);
        boolean isRich = rich != null && (rich.contains("text") || rich.contains("actionbar") || rich.contains("title")
                || rich.contains("subtitle") || rich.contains("sound"));
        if (!isRich || rich.isString("text")) sender.sendMessage(renderMessage(path, player, start, local, count, false));
        if (!isRich || player == null) return;
        String lang = langFile.getLangName();
        if (rich.isString("actionbar")) {
            String bar = renderText(rich.getString("actionbar"), fullPath + ".actionbar", player, lang, local, count, false);
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, net.md_5.bungee.api.chat.TextComponent.fromLegacyText(bar));
        }
        if (rich.isString("title") || rich.isString("subtitle")) {
            String title = rich.isString("title") ? renderText(rich.getString("title"), fullPath + ".title", player, lang, local, count, false) : "";
            String subtitle = rich.isString("subtitle") ? renderText(rich.getString("subtitle"), fullPath + ".subtitle", player, lang, local, count, false) : "";
            player.sendTitle(title, subtitle, 10, 70, 20);
        }
        if (rich.isString("sound")) playSound(player, rich.getString("sound"));
    }

    /** {@code sound} is a Minecraft sound key with optional volume and pitch: {@code "entity.villager.no 0.5 1"}. */
    static void playSound(Player player, String sound) {
        String[] parts = sound.trim().split("\\s+");
        try {
            float volume = parts.length > 1 ? Float.parseFloat(parts[1]) : 1f;
            float pitch = parts.length > 2 ? Float.parseFloat(parts[2]) : 1f;
            player.playSound(player.getLocation(), parts[0], volume, pitch);
        } catch (NumberFormatException e) {
            getLogger().warning("Invalid sound in a language file: " + sound);
        }
    }

    public ExpressionEnginePool getExpressionEnginePool() {
        return expressionEnginePool;
    }

    public void registerGlobalFunction(String functionName, FunctionCall function, String defaultType) {
        if (expressionEnginePool != null) {
            expressionEnginePool.registerGlobalFunction(functionName, function, defaultType);
        }
    }

    public void registerFunction(String languageName, String functionName, FunctionCall function, String
            defaultType) {
        if (expressionEnginePool != null) {
            expressionEnginePool.registerFunctionForLanguage(languageName, functionName, function, defaultType);
        }
    }

    public void unregisterFunction(String languageName, String functionName) {
        if (expressionEnginePool != null) {
            expressionEnginePool.unregisterFunctionForLanguage(languageName, functionName);
        }
    }

    public void unregisterGlobalFunction(String functionName) {
        if (expressionEnginePool != null) {
            expressionEnginePool.unregisterGlobalFunction(functionName);
        }
    }

    /**
     * Retrieves the ExpressionEngine for a specific player and language.
     * If the player is null, it retrieves the engine for the specified language.
     * If the languageName is null, it retrieves the default language engine.
     *
     * @param player       The player to get the ExpressionEngine for (nullable).
     * @param languageName The name of the language to get the ExpressionEngine for (nullable).
     * @return The ExpressionEngine for the specified player and language, or null if not available.
     */
    public ExpressionEngine getExpressionEngineFor(Player player, String languageName) {
        if (expressionEnginePool == null) return null;
        if (player != null && languageName != null) {
            return expressionEnginePool.getEngineForPlayer(player, languageName);
        }
        if (languageName != null) {
            return expressionEnginePool.getEngineForLanguage(languageName);
        }
        return expressionEnginePool.getDefaultLanguageEngine();
    }

    /**
     * Parses all embedded EXPRESSION(expr) or EXPR(expr) segments in the input string,
     * evaluates them, and replaces them with their results. If parsing fails, leaves
     * the original text and stops further parsing.
     *
     * @param input        The input string possibly containing embedded expressions.
     * @param player       The player context for per-player language (nullable).
     * @param languageName The language context (nullable, uses default if null).
     * @return The string with all embedded expressions evaluated and replaced.
     */
    public String parseEmbeddedExpressions(String input, Player player, String languageName) {
        if (input == null) return null;
        String regex = "(?i)(EXPRESSION|EXPR)\\(([^)]*)\\)";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(regex);
        java.util.regex.Matcher matcher = pattern.matcher(input);
        StringBuilder sb = new StringBuilder();
        ExpressionEngine engine = getExpressionEngineFor(player, languageName);
        while (matcher.find()) {
            String expr = matcher.group(2);
            Object result;
            try {
                synchronized (engine) {
                    result = engine.parse(expr, Object.class);
                }
                matcher.appendReplacement(sb, result == null ? "null" : Matcher.quoteReplacement(result.toString()));
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group(0));
                break;
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    @Override
    public UUID id() {
        return serviceId;
    }

    @Override
    public String serviceName() {
        return "language-manager";
    }

    /**
     * <p>Initializes the language manager and loads all registered language files.</p>
     * <pre><code>languageManager.init().join();</code></pre>
     *
     * @return a future that completes when initialization is done.
     */
    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            addLanguagesToList(true);
            if (registeredLanguages.isEmpty()) {
                getLogger().log(Level.SEVERE, "No language files found! Using default fallback.");
            } else {
                LanguageFile defaultLang = registeredLanguages.values().iterator().next();
                setCurrentLang(defaultLang, true);
            }
        });
    }

    /**
     * <p>Clears all language resources, caches, and placeholders.</p>
     * <pre><code>languageManager.shutdown().join();</code></pre>
     *
     * @return a future that completes when shutdown is done.
     */
    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(() -> {
            registeredLanguages.clear();
            languageCaches.clear();
            placeholders.clear();
        });
    }

    public LanguageFileMigrator createMigratorForLanguage(String langName) {
        LanguageFile langFile = getLang(langName, true);
        File langConfigFile = langFile.getLangFile();
        String resourceName = resourceDirectory + "/" + langFile.getLangFile().getName();
        InputStream resourceStream = plugin.getResource(resourceName);
        if (resourceStream == null) {
            resourceStream = plugin.getResource(resourceDirectory + "/en.yml");
        }
        return new LanguageFileMigrator(langConfigFile, resourceStream);
    }
}
