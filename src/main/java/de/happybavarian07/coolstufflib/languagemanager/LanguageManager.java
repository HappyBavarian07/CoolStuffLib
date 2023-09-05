package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.configupdater.ConfigUpdater;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
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

/**
 * LanguageManager class.
 */
public class LanguageManager {
    private final JavaPlugin plugin;
    private final File langFolder;
    private final String resourceDirectory;
    private final Map<String, LanguageFile> registeredLanguages;
    private final Map<String, Placeholder> placeholders;
    private String prefix;
    private String currentLangName;
    private LanguageFile currentLang;
    private PerPlayerLanguageHandler playerLanguageHandler;

    /**
     * Constructs a new LanguageManager object.
     *
     * @param plugin The JavaPlugin instance
     * @param langFolder The folder where language files are stored
     * @param resourceDirectory The resource directory of the plugin
     * @param prefix The prefix for the language files
     */
    public LanguageManager(JavaPlugin plugin, File langFolder, String resourceDirectory, String prefix) {
        this.prefix = prefix;
        this.plugin = plugin;
        this.langFolder = langFolder;
        this.resourceDirectory = resourceDirectory;
        this.registeredLanguages = new LinkedHashMap<>();
        this.placeholders = new LinkedHashMap<>();
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
     * 
     * Returns the PerPlayerLanguageHandler associated with this LanguageManager.
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
     * 
     * Returns the File object representing the language folder.
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
     * 
     * Returns the LanguageFile object representing the current language.
     */
    public LanguageFile getCurrentLang() {
        return currentLang;
    }

    /**
     * Sets the current language to the specified language file.
     *
     * @param currentLang The language file to set as the current language.
     * @param log Whether or not to log the change.
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
        if (log)
            plugin.getLogger().log(Level.INFO, "Current Language: " + currentLangName);
    }

    /**
     * Adds languages to the list.
     *
     * @param log Whether to log the language registration or not
     */
    public void addLanguagesToList(boolean log) {
        File[] fileArray = langFolder.listFiles();
        if (fileArray != null) {
            for (File file : fileArray) {
                LanguageFile languageFile = new LanguageFile(plugin, langFolder, resourceDirectory, file.getName().replace(".yml", ""));
                if (!registeredLanguages.containsValue(languageFile) && !languageFile.getLangName().equals("default"))
                    this.registeredLanguages.put(languageFile.getLangName(), languageFile);
                if (log && !languageFile.getLangName().equals("default"))
                    plugin.getLogger().log(Level.INFO, "Language: " + languageFile.getLangFile() + " successfully registered!");
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
                if (plugin.getResource(resourceName) == null) {
                    if (plugin.getResource(resourceDirectory + "/" + plugin.getConfig().getString("Plugin.languageForUpdates") + ".yml") != null) {
                        resourceName = resourceDirectory + "/" + plugin.getConfig().getString("Plugin.languageForUpdates") + ".yml";
                    } else {
                        resourceName = resourceDirectory + "/en.yml";
                    }
                }
                // "Test.Options", "Items.PlayerManager.TrollMenu.VillagerSounds.true.Options"
                ConfigUpdater.update(plugin, resourceName, langFiles.getLangFile(), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Reloads all languages and updates the language files.
     * 
     * @param messageReceiver The command sender to send the message to.
     * @param log Whether to log the action or not.
     */
    public void reloadLanguages(CommandSender messageReceiver, Boolean log) {
        addLanguagesToList(log);
        updateLangFiles();
        for (String langFiles : registeredLanguages.keySet()) {
            getLang(langFiles, true).getLangConfig().reloadConfig();
            if (messageReceiver != null) {
                addPlaceholder(PlaceholderType.MESSAGE, "%language%", getLang(langFiles, true).getLangFile(), true);
                messageReceiver.sendMessage(getMessage("Player.General.ReloadedLanguageFile", null, true));
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
        plugin.getLogger().log(Level.INFO, "Language: " + langFile.getLangFile() + " successfully registered!");
    }

    /**
     * Gets the LanguageFile object associated with the given language name.
     * 
     * @param langName The name of the language to get the LanguageFile object for.
     * @param throwException Whether or not to throw an exception if the language is
     * not found.
     * @return The LanguageFile object associated with the given language name, or null
     * if the language is not found and throwException is false.
     * @throws NullPointerException If the language is not found and throwException is
     * true.
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
     * Adds a placeholder to the LanguageManager. The placeholder type, key, and value
     * must be specified. If resetBefore is true, all placeholders of the specified
     * type will be reset before adding the new placeholder.
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
     * @param resetBefore Whether to reset existing placeholders before adding the new
     * ones
     * @return void
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
     * @param key The key of the placeholder to remove.
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
     * This method resets specific placeholders of a given type. If includeKeys is
     * provided, only those keys will be reset. It iterates through the placeholders
     * and checks if the type matches the given type or is of type ALL. If it does,
     * the key is added to the list of keys to remove. Finally, the method calls
     * removePlaceholders to remove the placeholders.
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
     * Gets a list of placeholder keys found in a given message of a specified type.
     * 
     * Checks each key in the placeholders map to see if it is present in the message
     * and if its type matches the specified type or is of type PlaceholderType.ALL.
     * If both conditions are met, the key is added to the list of keys.
     * 
     * @param message The message to search for placeholder keys
     * @param type The type of placeholder to search for
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
     * @param type The type of placeholder to replace.
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
     * @param item The {@link ItemStack} to replace placeholders in.
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
     * @param player The player to format the ItemStack for.
     * @param item The ItemStack to replace the placeholders in.
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
     * @param message The message to replace placeholders in.
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
     */
    public LanguageFile getLangOrPlayerLang(boolean currentLang, String langName, @Nullable Player player) {
        if (player == null && currentLang) return getCurrentLang();
        if (player == null) return getLang(langName, true);
        if (langName == null && currentLang) return getCurrentLang();

        LanguageFile langFile = playerLanguageHandler.getPlayerLanguage(player.getUniqueId());
        if (langFile == null) {
            if (currentLang) return getCurrentLang();
            else return getLang(langName, true);
        }
        return langFile;
    }

    /**
     * Gets a message from the specified path in the language file for the specified
     * player.
     * 
     * @param path The path of the message in the language file.
     * @param player The player to get the message for.
     * @param resetAfter Whether or not to reset the message after it is retrieved.
     * @return The message from the specified path in the language file.
     */
    public String getMessage(String path, Player player, boolean resetAfter) {
        return getMessage(path, player, getCurrentLangName(), resetAfter);
    }

    /**
     * Gets a message from the language file.
     * 
     * @param path The path of the message in the language file.
     * @param player The player to format the message for.
     * @param langName The name of the language file to get the message from.
     * @param resetAfter Whether to reset the placeholders after the message is
     * retrieved.
     * @return The formatted message.
     */
    public String getMessage(String path, Player player, String langName, boolean resetAfter) {
        LanguageFile langFile = getLangOrPlayerLang(true, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return "null config";
        if (langConfig.getConfig().getString("Messages." + path) == null || !langConfig.getConfig().contains("Messages." + path))
            return "null path: Messages." + path;

        String message = Utils.format(player, langConfig.getConfig().getString("Messages." + path), prefix);
        if (placeholders.isEmpty()) return message;

        List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(message, PlaceholderType.MESSAGE));
        message = replacePlaceholders(PlaceholderType.MESSAGE, message);
        if (resetAfter) resetSpecificPlaceholders(PlaceholderType.MESSAGE, includedKeys);
        return message;
    }

    /**
     * Gets the permission message for a given permission.
     *
     * @param player The player to get the message for.
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
     * @param path The path of the item in the language file.
     * @param player The player to get the item for.
     * @param resetAfter Whether or not to reset the item after it has been retrieved.
     * @return The item from the specified path in the language file.
     */
    public ItemStack getItem(String path, Player player, boolean resetAfter) {
        return getItem(path, player, getCurrentLangName(), resetAfter);
    }

    /**
     * LanguageManager class provides a method to get an ItemStack from a given path.
     * The path is specified in the language configuration file. The method takes in a
     * path, a player, a language name and a boolean value as parameters. The language
     * name is used to get the language configuration file. If the language
     * configuration file is not found, an ItemStack with a barrier material is
     * returned. If the path is not found in the language configuration file, an
     * ItemStack with a barrier material is returned. If the path is found but the
     * item is disabled, the general disabled item is returned. If the material
     * specified in the language configuration file is not found, an ItemStack with a
     * barrier material is returned. The display name and lore of the item is taken
     * from the language configuration file. Placeholders in the display name and lore
     * are replaced with the corresponding values. If the item is set to be enchanted,
     * the item is enchanted with the durability enchantment and the enchantment is
     * hidden. If the boolean value is set to true, the placeholders used in the
     * display name and lore are reset. The method returns the ItemStack.
     */
    public ItemStack getItem(String path, Player player, String langName, boolean resetAfter) {
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
        if (langConfig.getConfig().getString("Items." + path) == null || !langConfig.getConfig().contains("Items." + path)) {
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
        Material material = Material.matchMaterial(langConfig.getConfig().getString("Items." + path + ".material"));
        if (material == null) {
            assert errorMeta != null;
            errorMeta.setDisplayName("Material not found! (" + langConfig.getConfig().getString("Items." + path + ".material") + ")");
            errorMeta.setLore(Arrays.asList("If this happens,", "please change the Material from this Item", "to something existing", "Path: Items." + path + ".material"));
            error.setItemMeta(errorMeta);
            return error;
        }
        String displayName = langConfig.getConfig().getString("Items." + path + ".displayName");
        List<String> lore = langConfig.getConfig().getStringList("Items." + path + ".lore");
        List<String> loreWithPlaceholders = new ArrayList<>();
        List<String> includedKeys = new ArrayList<>();
        item = new ItemStack(material, 1);
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
        if (langConfig.getConfig().getBoolean("Items." + path + ".enchanted", false)) {
            meta.addEnchant(Enchantment.DURABILITY, 0, true);
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        item.setItemMeta(meta);
        if (resetAfter) resetSpecificPlaceholders(PlaceholderType.ITEM, includedKeys);
        return item;
    }

    /**
     * Gets the menu title for the given path in the language specified by the given
     * language name.
     * 
     * @param path The path of the menu title to get.
     * @param player The player to get the language name from.
     * @param langName The language name to get the menu title in.
     * @return The menu title for the given path in the language specified by the given
     * language name.
     */
    public String getMenuTitle(String path, Player player) {
        return getMenuTitle(path, player, getCurrentLangName());
    }

    /**
     * Provides a method to get the menu title from the language file based on the
     * given path. The language file is either the specified language or the player's
     * language. The title is then formatted with the player's language and the
     * prefix. Placeholders are also replaced in the title.
     */
    public String getMenuTitle(String path, Player player, String langName) {
        LanguageFile langFile = getLangOrPlayerLang(false, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return "null config";
        if (langConfig.getConfig().getString("MenuTitles." + path) == null || !langConfig.getConfig().contains("MenuTitles." + path))
            return "null path: MenuTitles." + path;
        String title = langConfig.getConfig().getString("MenuTitles." + path);
        List<String> includedKeys = new ArrayList<>(getPlaceholderKeysInMessage(title, PlaceholderType.MENUTITLE));
        title = replacePlaceholders(PlaceholderType.MENUTITLE, title);
        resetSpecificPlaceholders(PlaceholderType.MENUTITLE, includedKeys);
        return Utils.format(player, title, prefix);
    }

    /**
     * Retrieve a custom object from the specified path.
     * 
     * @param path The path to the custom object.
     * @param player The player to retrieve the custom object for, or null for global.
     * @param defaultValue The default value to return if the custom object is not
     * found.
     * @param resetAfter Whether or not to reset the custom object after retrieval.
     * @return The custom object, or the default value if not found.
     */
    public <T> T getCustomObject(String path, @Nullable Player player, T defaultValue, boolean resetAfter) {
        return getCustomObject(path, player, null, defaultValue, resetAfter);
    }

    /**
     * Gets a custom object from the language file.
     * 
     * @param path The path to the object in the language file.
     * @param player The player to use for placeholders.
     * @param langName The language name to use.
     * @param defaultValue The default value to return if the object is not found.
     * @param resetAfter Whether to reset the placeholders after getting the object.
     * @return The object from the language file, or the default value if not found.
     */
    public <T> T getCustomObject(String path, @Nullable Player player, String langName, T defaultValue, boolean resetAfter) {
        LanguageFile langFile = getLangOrPlayerLang(false, langName, player);
        LanguageConfig langConfig = langFile.getLangConfig();
        if (langConfig == null || langConfig.getConfig() == null)
            return null;
        if (langConfig.getConfig().get(path) == null || !langConfig.getConfig().contains(path))
            return null;
        if (langConfig.getConfig().get(path) == null) return null;

        T obj;
        try {
            obj = (T) langConfig.getConfig().get(path);
            if (obj instanceof String) {
                obj = (T) replacePlaceholders(PlaceholderType.CUSTOM, Utils.format(player, obj.toString(), prefix));
                if (resetAfter)
                    resetSpecificPlaceholders(PlaceholderType.CUSTOM,
                            getPlaceholderKeysInMessage((String) langConfig.getConfig().get(path), PlaceholderType.CUSTOM));
            }
        } catch (ClassCastException e) {
            e.printStackTrace();
            return defaultValue;
        }
        if (obj == null) obj = defaultValue;
        return obj;
    }
}
