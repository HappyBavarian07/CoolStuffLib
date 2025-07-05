package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.configstuff.ConfigUpdater;
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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * LanguageManager class.
 */
public class LanguageManager {
    private static Logger logger;
    private final JavaPlugin plugin;
    private final File langFolder;
    private final String resourceDirectory;
    private final Map<String, LanguageFile> registeredLanguages;
    private final Map<String, Placeholder> placeholders;
    private final Map<String, LanguageCache> languageCaches; // New map for LanguageCache
    private final Map<String, Map<String, Object>> playerPathVariables = new HashMap<>();
    private final ExpressionEnginePool expressionEnginePool;
    private String prefix;
    private String currentLangName;
    private LanguageFile currentLang;
    private PerPlayerLanguageHandler playerLanguageHandler;

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
        this.placeholders = new LinkedHashMap<>();
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
    public void setPathExpressionVariable(String playerUUID, String path, String key, Object value) {
        String mapKey = playerUUID + ":" + path;
        if (!playerPathVariables.containsKey(mapKey)) {
            playerPathVariables.put(mapKey, new HashMap<>());
        }
        playerPathVariables.get(mapKey).put(key, value);
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
            Map<String, Object> variables = playerPathVariables.get(mapKey);
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                addVariableGlobally(entry.getKey(), entry.getValue(), 1);
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
        File[] fileArray = langFolder.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                LanguageFile languageFile = new LanguageFile(langFolder, resourceDirectory, file.getName().replace(".yml", ""));
                if (!registeredLanguages.containsValue(languageFile) && !languageFile.getLangName().equals("default")) {
                    if (log)
                        getLogger().log(Level.INFO, "Language: " + languageFile.getLangFile() + " successfully registered!");
                    registeredLanguages.put(languageFile.getLangName(), languageFile);
                    languageCaches.put(languageFile.getLangName(), new LanguageCache(languageFile.getLangName()));
                    addEngineForLanguage(languageFile.getLangName(), true, true);
                }
            }
        }
    }

    /**
     * LanguageManager.updateLangFiles() updates the registered language files. It
     * looks for the language file in the resource directory and if it is not found,
     * it looks for the language file specified in the Plugin.languageForUpdates
     * configuration option. If that is not found, it looks for the en.yml file. If
     * any of these files are found, it uses ConfigUpdater to update the language file.
     */
    public void updateLangFiles() {
        for (LanguageFile langFiles : getRegisteredLanguages().values()) {
            try {
                String resourceName = resourceDirectory + "/" + langFiles.getLangFile().getName();
                if (Utils.getResource(resourceName) == null) {
                    if (Utils.getResource(resourceDirectory + "/" + plugin.getConfig().getString("Plugin.languageForUpdates") + ".yml") != null) {
                        resourceName = resourceDirectory + "/" + plugin.getConfig().getString("Plugin.languageForUpdates") + ".yml";
                    } else {
                        resourceName = resourceDirectory + "/en.yml";
                    }
                }
                // "Test.Options", "Items.PlayerManager.TrollMenu.VillagerSounds.true.Options"
                ConfigUpdater.update(plugin, resourceName, langFiles.getLangFile(), new ArrayList<>());
            } catch (IOException e) {
                getLogger().log(Level.SEVERE, "Error updating language file: " + langFiles.getLangFile().getName(), e);
            } catch (NullPointerException e) {
                getLogger().log(Level.WARNING, "Language file not found: " + langFiles.getLangFile().getName(), e);
            }
        }
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
            getLang(langFiles, true).getLangConfig().reloadConfig();
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
     * Adds a placeholder to the LanguageManager.
     *
     * @param type        The type of placeholder.
     * @param key         The key associated with the placeholder.
     * @param value       The value to replace the placeholder with.
     * @param resetBefore Whether to reset all placeholders of the specified type before adding the new one.
     */
    public void addPlaceholder(PlaceholderType type, String key, Object value, boolean resetBefore) {
        if (resetBefore) resetPlaceholders(type, null);
        if (!placeholders.containsKey(key))
            placeholders.put(key, new Placeholder(key, value, type));
        else
            placeholders.replace(key, placeholders.get(key), new Placeholder(key, value, type));
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
        if (resetBefore) resetPlaceholders(PlaceholderType.ALL, null);
        this.placeholders.putAll(placeholders);
    }

    /**
     * Removes a placeholder from the LanguageManager. The placeholder must match the
     * specified type and key in order to be removed.
     *
     * @param type The type of placeholder to remove.
     * @param key  The key of the placeholder to remove.
     */
    public void removePlaceholder(PlaceholderType type, String key) {
        if (!placeholders.containsKey(key)) return;
        if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
            return;

        placeholders.remove(key);
    }

    /**
     * Removes the specified placeholders of the given type from the LanguageManager.
     *
     * @param type The type of placeholder to remove
     * @param keys The keys of the placeholders to remove
     */
    public void removePlaceholders(PlaceholderType type, List<String> keys) {
        for (String key : keys) {
            if (!placeholders.containsKey(key)) continue;
            if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
                continue;

            this.placeholders.remove(key);
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
        List<String> keysToRemove = new ArrayList<>();
        for (String key : placeholders.keySet()) {
            if (excludeKeys != null && excludeKeys.contains(key)) continue;
            if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
                continue;

            keysToRemove.add(key);
        }
        removePlaceholders(type, keysToRemove);
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
        List<String> keysToRemove = new ArrayList<>();
        for (String key : placeholders.keySet()) {
            if (includeKeys != null && !includeKeys.contains(key)) continue;
            if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
                continue;

            keysToRemove.add(key);
        }
        removePlaceholders(type, keysToRemove);
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
        List<String> keys = new ArrayList<>();
        for (String key : placeholders.keySet()) {
            if (!message.contains(key)) continue;
            if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
                continue;

            keys.add(key);
        }
        return keys;
    }

    /**
     * Replaces placeholders in the given message with their corresponding values.
     *
     * @param type    The type of placeholder to replace.
     * @param message The message to replace placeholders in.
     * @return The message with placeholders replaced.
     */
    public String replacePlaceholders(PlaceholderType type, String message) {
        for (String key : placeholders.keySet()) {
            if (!placeholders.get(key).getType().equals(type) && !placeholders.get(key).getType().equals(PlaceholderType.ALL))
                continue;

            message = placeholders.get(key).replace(message);
        }
        return message;
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

        LanguageFile lang = playerLanguageHandler.getPlayerLanguage(player.getUniqueId());
        if (lang == null) {
            if (currentLang) return getCurrentLang();
            else return getLang(langName, true);
        }
        return lang;
    }

    public <T> T getObjectFromLanguageCacheOrConfig(String path, String langName, Class<T> clazz) {
        LanguageCache langCache = getLanguageCache(langName);
        if (langCache.containsKey(path) && langCache.getData(path).getClass().isInstance(clazz)) {
            return clazz.cast(langCache.getData(path));
        } else {
            LanguageFile langFile = getLang(langName, true);
            LanguageConfig langConfig = langFile.getLangConfig();
            if (langConfig == null || langConfig.getConfig() == null) return getDefaultInstance(clazz);
            Object configObject = langConfig.getConfig().get(path);
            if (configObject == null || !langConfig.getConfig().contains(path))
                return getDefaultInstance(clazz);
            try {
                T obj = clazz.cast(configObject);
                langCache.addData(path, obj, true);
                return obj;
            } catch (ClassCastException e) {
                return null;
            }
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
        LanguageFile langFile = getLangOrPlayerLang(true, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return "null config";
        if ((langConfig.getConfig().getString("Messages." + path) == null || !langConfig.getConfig().contains("Messages." + path)) && !getLanguageCache(langName).containsKey("Messages." + path))
            return "null path: Messages." + path;

        String rawMessage = getObjectFromLanguageCacheOrConfig("Messages." + path, langName, String.class);

        String message = Utils.format(player, rawMessage, prefix);
        if (!placeholders.isEmpty()) {
            List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(message, PlaceholderType.MESSAGE));
            message = replacePlaceholders(PlaceholderType.MESSAGE, message);
            if (resetAfter) resetSpecificPlaceholders(PlaceholderType.MESSAGE, includedKeys);
        }
        message = parseEmbeddedExpressions(message, player, langName);
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
        applyPathExpressionVariables(player, path);
        LanguageFile langFile = getLangOrPlayerLang(false, langName, player);
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
        if ((langConfig.getConfig().getString("Items." + path) == null || !langConfig.getConfig().contains("Items." + path)) && !getLanguageCache(langName).containsKey("Items." + path)) {
            assert errorMeta != null;
            errorMeta.setDisplayName("Config Path not found!");
            errorMeta.setLore(Arrays.asList("If this happens often,", "please report to the Discord", "Path: Items." + path));
            error.setItemMeta(errorMeta);
            return error;
        }
        if (langConfig.getConfig().getBoolean("Items." + path + ".disabled", false) &&
                !Objects.equals(path, "General.DisabledItem")) {
            return this.getItem("General.DisabledItem", player, false);
        }
        ItemStack item;


        if (condition instanceof HeadMaterialCondition headCondition) {
            if (headCondition.isHead()) {
                item = headCondition.getHead().getAsItem();
            } else {
                item = Utils.createSkull(headCondition.getHeadValue(), headCondition.getHeadValue(), headCondition.isTexture());
            }
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
            Material material = getMaterial(materialString, player, langFile.getLangName());
            if (material == null) {
                assert errorMeta != null;
                errorMeta.setDisplayName("Material not found! (" + langConfig.getConfig().getString("Items." + path + ".material") + ")");
                errorMeta.setLore(Arrays.asList("If this happens,", "please change the Material from this Item", "to something existing", "Path: Items." + path + ".material"));
                error.setItemMeta(errorMeta);
                return error;
            }
            item = new ItemStack(material, 1);
        }
        String displayName = getObjectFromLanguageCacheOrConfig("Items." + path + ".displayName", langFile.getLangName(), String.class);
        List<String> lore = this.<List>getObjectFromLanguageCacheOrConfig("Items." + path + ".lore", langFile.getLangName(), List.class);
        List<String> loreWithPlaceholders = new ArrayList<>();
        List<String> includedKeys = new ArrayList<>();
        ItemMeta meta = item.getItemMeta();
        for (String s : lore) {
            includedKeys.addAll(getPlaceholderKeysInMessage(s, PlaceholderType.ITEM));
            String temp = replacePlaceholders(PlaceholderType.ITEM, s);
            loreWithPlaceholders.add(Utils.format(player, temp, prefix));
        }
        assert meta != null;
        meta.setLore(loreWithPlaceholders);
        assert displayName != null;
        includedKeys.addAll(getPlaceholderKeysInMessage(Utils.format(player, displayName, prefix), PlaceholderType.ITEM));
        meta.setDisplayName(replacePlaceholders(PlaceholderType.ITEM, Utils.format(player, displayName, prefix)));
        if (getObjectFromLanguageCacheOrConfig("Items." + path + ".enchanted", langFile.getLangName(), Boolean.class)) {
            meta.addEnchant(Enchantment.DURABILITY, 0, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        if (resetAfter) resetSpecificPlaceholders(PlaceholderType.ITEM, includedKeys);
        return item;
    }

    /**
     * Gets a Material from the specified material string.
     *
     * @param materialString The material string to get the Material from.
     * @param player         The player to use for placeholder replacements.
     * @param langName       The language name to use for placeholder replacements.
     * @return The Material from the specified material string, or BARRIER if the
     * material is invalid.
     */
    public Material getMaterial(String materialString, Player player, String langName) {
        if (materialString == null) {
            return Material.BARRIER;
        }

        if (materialString.startsWith("HEAD(") && materialString.endsWith(")")) {
            String headName = materialString.substring(5, materialString.length() - 1).trim();
            try {
                try {
                    Head head = Head.valueOf(headName);
                    return head.getAsItem().getType();
                } catch (IllegalArgumentException e) {
                    return Material.PLAYER_HEAD;
                }
            } catch (Exception e) {
                getLogger().warning("Invalid head: " + headName);
                return Material.BARRIER;
            }
        }

        try {
            MaterialCondition cond = getExpressionEngineFor(player, langName).parse(materialString, Material.BARRIER);
            return cond.getMaterial();
        } catch (Exception e) {
            if (materialString.contains("if") || materialString.contains("else")) {
                getLogger().warning("Error parsing conditional expression: " + materialString);
                getLogger().warning(e.getMessage());
            }
        }

        try {
            return Material.valueOf(materialString.toUpperCase());
        } catch (IllegalArgumentException e) {
            if (!materialString.equals("PLAYER_HEAD")) {
                getLogger().warning("Invalid material name: " + materialString);
            }
            return Material.BARRIER;
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
        LanguageFile langFile = getLangOrPlayerLang(false, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return "null config";
        if ((langConfig.getConfig().getString("MenuTitles." + path) == null || !langConfig.getConfig().contains("MenuTitles." + path)) && !getLanguageCache(langName).containsKey("MenuTitles." + path))
            return "null path: MenuTitles." + path;
        String title = getObjectFromLanguageCacheOrConfig("MenuTitles." + path, langName, String.class);
        List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(title, PlaceholderType.MENUTITLE));
        title = replacePlaceholders(PlaceholderType.MENUTITLE, title);
        resetSpecificPlaceholders(PlaceholderType.MENUTITLE, includedKeys);
        title = parseEmbeddedExpressions(Utils.format(player, title, prefix), player, langName);
        return title;
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
        LanguageFile langFile = getLangOrPlayerLang(false, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
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
        try {
            int i = Integer.parseInt(obj.toString());
            if (i == 0) return defaultValue;
        } catch (NumberFormatException ignored) {
        }

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
                result = engine.parse(expr, Object.class);
                matcher.appendReplacement(sb, result == null ? "null" : java.util.regex.Matcher.quoteReplacement(result.toString()));
            } catch (Exception e) {
                matcher.appendReplacement(sb, matcher.group(0));
                break;
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
