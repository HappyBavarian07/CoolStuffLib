package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:24
 */

import com.google.common.base.Strings;
import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.ListSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.MapSection;
import de.happybavarian07.coolstufflib.configstuff.advanced.section.SetSection;
import de.happybavarian07.coolstufflib.logging.ConfigLogger;
import de.happybavarian07.coolstufflib.menusystem.Menu;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;
import de.happybavarian07.coolstufflib.menusystem.misc.ConfirmationMenu;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class Utils {
    public static String chat(String s) {
        return ChatColor.translateAlternateColorCodes('&', s);
    }

    public static String format(Player player, String message, String prefix) {
        try {
            String withColor = ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
            if (!CoolStuffLib.getLib().isPlaceholderAPIEnabled()) return withColor;
            return PlaceholderAPI.setPlaceholders(player, withColor);
        } catch (Exception e) {
            return ChatColor.translateAlternateColorCodes('&', message.replace("%prefix%", prefix));
        }
    }

    public static List<String> emptyList() {
        List<String> list = new ArrayList<>();
        list.add("");
        list.add("");
        return list;
    }

    /**
     * Gets a menu by its class name.
     * The menu must be in the menu package.
     * If the menu is not found, it will return null.
     *
     * @param menuPackage The package of the menu
     * @param className   The name of the class
     * @param player      The player to open the menu for
     * @return The menu
     */
    public static Menu getMenuByClassName(String menuPackage, String className, Player player) {
        //String menuPackage = "de.happybavarian07.adminpanel.menusystem.menu";
        String fullClassName = menuPackage + "." + className;

        try {
            Class<?> clazz = Class.forName(fullClassName);
            if (Menu.class.isAssignableFrom(clazz)) {
                return (Menu) clazz.getDeclaredConstructor(PlayerMenuUtility.class).newInstance(CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId()));
            } else {
                System.err.println("The class does not extend Menu: " + fullClassName);
                return null;
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + fullClassName);
            if (CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Class not found: " + fullClassName, LogPrefix.ERROR, true);
            return null;
        } catch (IllegalAccessException | InstantiationException e) {
            System.err.println("Error creating an instance: " + fullClassName);
            if (CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Error creating an instance: " + fullClassName, LogPrefix.ERROR, true);
            return null;
        } catch (InvocationTargetException | NoSuchMethodException e) {
            if (CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Error invoking constructor: " + fullClassName, LogPrefix.ERROR, true);
            throw new RuntimeException("Error invoking constructor: " + fullClassName, e);
        }
    }

    /**
     * Opens a confirmation menu for the player to confirm an action.
     * If the player confirms the action, the methodToExecuteAfter will be executed.
     * If the player cancels the action, the menuToOpenAfter will be opened.
     *
     * @param reason               The reason for the confirmation
     * @param menuToOpenAfter      The menu to open after the confirmation
     * @param menuPackage          The package of the menu
     * @param methodToExecuteAfter The method to execute after the confirmation
     * @param objectToInvokeOn     The object to invoke the method on
     * @param methodArgs           The arguments for the method
     * @param exceptionsToCatch    The exceptions to catch
     * @param player               The player to open the menu for
     */
    public static void openConfirmationMenu(String reason,
                                            String menuToOpenAfter,
                                            String menuPackage,
                                            Method methodToExecuteAfter,
                                            Object objectToInvokeOn,
                                            List<Object> methodArgs,
                                            List<Class<? extends Exception>> exceptionsToCatch,
                                            Player player) {
        PlayerMenuUtility playerMenuUtility = CoolStuffLib.getLib().getPlayerMenuUtility(player.getUniqueId());
        ConfirmationMenu confirmationMenu = new ConfirmationMenu(playerMenuUtility);
        playerMenuUtility.setData("ConfirmationMenu_MenuToOpenAfter", menuToOpenAfter, true);
        playerMenuUtility.setData("ConfirmationMenu_MenuPackage", menuPackage, true);
        playerMenuUtility.setData("ConfirmationMenu_Reason", reason, true);
        playerMenuUtility.setData("ConfirmationMenu_MethodToExecuteAfter", methodToExecuteAfter, true);
        playerMenuUtility.setData("ConfirmationMenu_ObjectToInvokeMethodOn", objectToInvokeOn, true);
        for (int i = 0; i < methodArgs.size(); i++) {
            playerMenuUtility.setData("ConfirmationMenu_MethodArgs_" + i, methodArgs.get(i), true);
        }
        playerMenuUtility.setData("ConfirmationMenu_ExceptionsToCatch", exceptionsToCatch, true);
        confirmationMenu.open();
    }

    /**
     * <p>Creates a player skull item with a custom head texture or player name.</p>
     * <pre><code>
     * ItemStack skull = Utils.createSkull("textureStringOrPlayerName", "Display Name", true);
     * </code></pre>
     *
     * @param headValue the head texture string or player name
     * @param name      the display name for the skull item
     * @param isTexture true if headValue is a texture string, false if it is a player name
     * @return the created ItemStack representing the skull
     */
    public static ItemStack createSkull(String headValue, String name, boolean isTexture) {
        ItemStack head = new ItemStack(Utils.legacyServer() ? Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")) : Material.PLAYER_HEAD, 1);
        if (headValue.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta == null) return head;

        try {
            Head headEnum = Head.valueOf(headValue);
            return headEnum.getAsItem();
        } catch (IllegalArgumentException ignored) {
        }

        meta.setDisplayName(Utils.chat(name));
        if (!isTexture) {
            meta.setOwningPlayer(Bukkit.getOfflinePlayer(headValue));
            head.setItemMeta(meta);
            return head;
        }

        return applyItemStackToProfile(headValue, head, meta);
    }

    @NotNull
    public static ItemStack applyItemStackToProfile(String headValue, ItemStack head, SkullMeta meta) {
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");
        try {
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + headValue));
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        meta.setOwnerProfile(profile);
        head.setItemMeta(meta);
        return head;
    }

    /**
     * Creates a skull with a head texture and a name from a Head Object.
     * <br>
     * This method can only handle values from {@link Head}
     *
     * @param headTexture Element from the Head Enum with Head String Values
     * @param name        The name of the skull
     * @return The skull
     */
    public static ItemStack createSkull(Head headTexture, String name) {
        ItemStack head = new ItemStack(legacyServer() ? Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")) : Material.PLAYER_HEAD, 1);
        if (headTexture.getTexture().isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Utils.chat(name));
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");

        try {
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + headTexture.getTexture()));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        meta.setOwnerProfile(profile);

        head.setItemMeta(meta);
        return head;
    }

    /**
     * Gets a resource from the classloader.
     * If the resource is not found, it will return null.
     *
     * @param filename The name of the resource
     * @return The InputStream of the resource or null if not found
     */
    @Nullable
    public static InputStream getResource(@NotNull String filename) {
        try {
            URL url = Utils.class.getClassLoader().getResource(filename);

            if (url == null) {
                return null;
            }

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    /**
     * Save a resource from the classloader to a file.
     * If the file already exists, it will be overwritten.
     *
     * @param configFolder The folder where the resource should be saved
     * @param resourcePath The path of the resource
     * @param replace      If true, the file will be overwritten if it already exists
     */
    public static void saveResource(File configFolder, @NotNull String resourcePath, boolean replace) {
        if (resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found in " + Utils.class.getName() + "'s classloader");
        }

        File outFile = new File(configFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(configFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) {
            outDir.mkdirs();
        }

        try {
            if (!outFile.exists() || replace) {
                OutputStream out = new FileOutputStream(outFile);
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                out.close();
                in.close();
            } else {
                throw new IllegalStateException("Could not save " + outFile.getName() + " to " + outFile + " because " + outFile.getName() + " already exists.");
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save " + outFile.getName() + " to " + outFile, ex);
        }
    }

    /**
     * Zips multiple files into a single zip file.
     *
     * @param files   The files to zip
     * @param zipFile The name of the zip file to create
     * @throws IOException If an I/O error occurs
     */
    public static void zipFiles(File[] files, String zipFile, File baseDir) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : files) {
                if (!file.exists()) continue;
                if (file.isDirectory()) {
                    zipDirIntoZipFile(zos, file, baseDir);
                    continue;
                }
                writeZipContentToStream(zos, file, baseDir);
            }
            zos.finish();
        }
    }

    private static void zipDirIntoZipFile(ZipOutputStream zos, File directory, File baseDir) throws IOException {
        if (!directory.isDirectory()) return;
        File[] filesInDir = directory.listFiles();
        if (filesInDir != null) {
            for (File fileInDir : filesInDir) {
                if (fileInDir.isDirectory()) {
                    zipDirIntoZipFile(zos, fileInDir, baseDir);
                } else {
                    writeZipContentToStream(zos, fileInDir, baseDir);
                }
            }
        }
    }

    private static void writeZipContentToStream(ZipOutputStream zos, File fileInDir, File baseDir) throws IOException {
        try (FileInputStream fis = new FileInputStream(fileInDir)) {
            String entryName = baseDir.toPath().relativize(fileInDir.toPath()).toString().replace(File.separatorChar, '/');
            ZipEntry zipEntry = new ZipEntry(entryName);
            zos.putNextEntry(zipEntry);
            byte[] bytes = new byte[1024];
            int length;
            while ((length = fis.read(bytes)) >= 0) {
                zos.write(bytes, 0, length);
            }
            zos.closeEntry();
        }
    }

    /**
     * Unzips a zip file to a specified directory.
     *
     * @param zipFilePath The path to the zip file
     * @param destDir     The destination directory where the files will be unzipped
     * @param replace     If true, existing files will be replaced
     */
    public static void unzipFiles(String zipFilePath, String destDir, boolean replace) {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        FileInputStream fis;
        byte[] buffer = new byte[1024];
        try {
            fis = new FileInputStream(zipFilePath);
            ZipInputStream zis = new ZipInputStream(fis);
            ZipEntry ze = zis.getNextEntry();
            while (ze != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                if (ze.isDirectory()) {
                    new File(newFile.getParent()).mkdirs();
                } else {
                    if (replace) {
                        new File(newFile.getParent()).mkdirs();
                    } else {
                        if (newFile.exists()) {
                            ze = zis.getNextEntry();
                            continue;
                        }
                    }
                }
                FileOutputStream fos = new FileOutputStream(newFile);
                int len;
                while ((len = zis.read(buffer)) > 0) {
                    fos.write(buffer, 0, len);
                }
                fos.close();
                zis.closeEntry();
                ze = zis.getNextEntry();
            }
            //close last ZipEntry
            zis.closeEntry();
            zis.close();
            fis.close();
        } catch (IOException e) {
            CoolStuffLib.getLib().getPluginFileLogger()
                    .writeToLog(Level.SEVERE, "Failed to unzip files: " + e.getMessage(), LogPrefix.ERROR, true);
        }
    }

    public static Map<String, Object> flatten(ConfigTypeConverterRegistry registry, String prefix, Object value) {
        Map<String, Object> map = new HashMap<>();
        if (value instanceof Map m && m.containsKey("__type__")) {
            for (Object k : m.keySet()) {
                String key = String.valueOf(k);
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, m.get(key)));
            }
        } else if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                String key = entry.getKey();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, entry.getValue()));
            }
        } else if (value instanceof Collection) {
            int index = 0;
            for (Object item : (Collection<?>) value) {
                String newPrefix = prefix + "." + index++;
                map.putAll(flatten(registry, newPrefix, item));
            }
        } else if (value instanceof ConfigSection section) {
            Map<String, Object> sectionMap = section.toSerializableMap();
            for (Map.Entry<String, Object> entry : sectionMap.entrySet()) {
                String key = entry.getKey();
                String newPrefix = prefix.isEmpty() ? key : prefix + "." + key;
                map.putAll(flatten(registry, newPrefix, entry.getValue()));
            }
        } else if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            map.put(prefix, registry.tryToSerialized(value));
        } else if (value == null || Strings.isNullOrEmpty(value.toString())) {
            map.put(prefix, null);
        } else {
            map.put(prefix, registry.tryToSerialized(value));
        }
        return map;
    }

    public static Object unflatten(ConfigTypeConverterRegistry registry, Map<String, String> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("\\.");
            insertUnflattened(result, parts, 0, entry.getValue(), registry);
        }
        Object converted = convertMapsToLists(result);
        if (converted instanceof Map) {
            return (Map<String, Object>) converted;
        } else {
            Map<String, Object> fallback = new HashMap<>();
            fallback.put("root", converted);
            return fallback;
        }
    }

    private static Object convertMapsToLists(Object obj) {
        if (obj instanceof Map<?, ?> m) {
            // Check for map with only integer keys
            List<Integer> intKeys = new ArrayList<>();
            for (Object k : m.keySet()) {
                if (k instanceof String s && s.matches("\\d+")) {
                    intKeys.add(Integer.parseInt(s));
                } else {
                    // Not all keys are integers, recurse
                    Map<String, Object> newMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        newMap.put(String.valueOf(e.getKey()), convertMapsToLists(e.getValue()));
                    }
                    return newMap;
                }
            }
            // All keys are integers, convert to list
            List<Object> list = new ArrayList<>();
            int max = intKeys.stream().max(Integer::compareTo).orElse(-1);
            for (int i = 0; i <= max; i++) {
                list.add(convertMapsToLists(m.get(String.valueOf(i))));
            }
            return list;
        } else if (obj instanceof List<?> l) {
            List<Object> newList = new ArrayList<>();
            for (Object o : l) newList.add(convertMapsToLists(o));
            return newList;
        }
        return obj;
    }

    public static Map<String, Object> unflattenObjectMap(ConfigTypeConverterRegistry registry, Map<String, Object> map) {
        boolean isFlat = true;
        for (String key : map.keySet()) {
            if (!key.contains(".")) {
                isFlat = false;
                break;
            }
        }
        Map<String, Object> nested = isFlat ? (Map<String, Object>) unflatten(registry, toStringMapIfNeeded(map)) : map;
        processSectionsRecursive(nested);
        return nested;
    }

    public static List<Object> unflattenObjectList(ConfigTypeConverterRegistry registry, Map<String, Object> map) {
        Object converted = convertMapsToLists(unflatten(registry, toStringMapIfNeeded(map)));
        if (converted instanceof List) {
            return (List<Object>) converted;
        } else {
            List<Object> fallback = new ArrayList<>();
            fallback.add(converted);
            return fallback;
        }
    }

    public static Map<String, Object> convertMapsToListsMap(Map<String, Object> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map) {
                Object converted = convertMapsToLists(value);
                result.put(entry.getKey(), converted);
            } else {
                result.put(entry.getKey(), value);
            }
        }
        return result;
    }

    private static void processSectionsRecursive(Map<String, Object> map) {
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof Map<?, ?> m && m.containsKey("__type__")) {
                String type = String.valueOf(m.get("__type__"));
                if ("ListSection".equals(type)) {
                    ListSection section = new ListSection("");
                    Object items = m.get("__items");
                    if (items instanceof List<?>) {
                        section.fromList((List<?>) items);
                    } else if (items instanceof Map<?, ?> itemMap) {
                        List<Object> list = new ArrayList<>();
                        TreeMap<Integer, Object> ordered = new TreeMap<>();
                        for (Map.Entry<?, ?> e : itemMap.entrySet()) {
                            String k = String.valueOf(e.getKey());
                            if (k.matches("\\d+")) {
                                ordered.put(Integer.parseInt(k), e.getValue());
                            }
                        }
                        list.addAll(ordered.values());
                        section.fromList(list);
                    } else {
                        TreeMap<Integer, Object> ordered = new TreeMap<>();
                        for (Map.Entry<?, ?> e : m.entrySet()) {
                            String k = String.valueOf(e.getKey());
                            if (k.startsWith("__items.")) {
                                String idxStr = k.substring(8);
                                try {
                                    int idx = Integer.parseInt(idxStr);
                                    ordered.put(idx, e.getValue());
                                } catch (NumberFormatException ignored) {
                                }
                            }
                        }
                        if (!ordered.isEmpty()) {
                            section.fromList(new ArrayList<>(ordered.values()));
                        }
                    }
                    entry.setValue(section);
                } else if ("MapSection".equals(type)) {
                    MapSection section = new MapSection("");
                    Map<String, Object> subMap = new HashMap<>();
                    for (Map.Entry<?, ?> e : m.entrySet()) {
                        String k = String.valueOf(e.getKey());
                        if (!"__type__".equals(k)) {
                            subMap.put(k, e.getValue());
                        }
                    }
                    processSectionsRecursive(subMap);
                    section.fromMap(subMap);
                    entry.setValue(section);
                } else if ("SetSection".equals(type)) {
                    SetSection section = new SetSection("");
                    Object items = m.get("__items");
                    if (items instanceof List<?>) {
                        section.fromSet(new HashSet<>((List<?>) items));
                    }
                    entry.setValue(section);
                }
            } else if (value instanceof Map<?, ?> subMap) {
                processSectionsRecursive((Map<String, Object>) subMap);
            }
        }
    }

    private static Map<String, String> toStringMapIfNeeded(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Object v = entry.getValue();
            result.put(entry.getKey(), v == null ? null : v instanceof String ? (String) v : v.toString());
        }
        return result;
    }

    private static Map<String, String> toStringMap(Map<String, Object> map) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            result.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
        }
        return result;
    }

    private static void insertUnflattened(Map<String, Object> current, String[] parts, int index, Object value, ConfigTypeConverterRegistry registry) {
        String part = parts[index];
        int last = parts.length - 1;
        int dotIdx = part.indexOf('[');
        if (dotIdx != -1 && part.endsWith("]")) {
            String key = part.substring(0, dotIdx);
            String idxStr = part.substring(dotIdx + 1, part.length() - 1);
            int idx;
            try {
                idx = Integer.parseInt(idxStr);
            } catch (NumberFormatException e) {
                current.put(part, convertValue(registry, value));
                return;
            }
            List<Object> list = (List<Object>) current.computeIfAbsent(key, k -> new ArrayList<>());
            while (list.size() <= idx) list.add(null);
            if (index == last) {
                list.set(idx, convertValue(registry, value));
            } else {
                Object next = list.get(idx);
                if (!(next instanceof Map)) {
                    next = new HashMap<String, Object>();
                    list.set(idx, next);
                }
                insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
            }
            return;
        }
        if (index == last) {
            current.put(part, convertValue(registry, value));
            return;
        }
        Object next = current.computeIfAbsent(part, k -> new HashMap<String, Object>());
        if (!(next instanceof Map)) {
            next = new HashMap<String, Object>();
            current.put(part, next);
        }
        insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
    }

    public static Object convertValue(ConfigTypeConverterRegistry registry, Object value) {
        if (value instanceof String stringValue) {
            try {
                if (stringValue.matches("^-?\\d+$")) {
                    return Integer.parseInt(stringValue);
                } else if (stringValue.matches("^-?\\d+\\.\\d+$")) {
                    return Double.parseDouble(stringValue);
                } else if ("true".equalsIgnoreCase(stringValue) || "false".equalsIgnoreCase(stringValue)) {
                    return Boolean.parseBoolean(stringValue);
                } else if (stringValue.matches("^-?\\d+L$")) {
                    return Long.parseLong(stringValue.substring(0, stringValue.length() - 1));
                } else if (stringValue.matches("^-?\\d+\\.\\d+F$")) {
                    return Float.parseFloat(stringValue.substring(0, stringValue.length() - 1));
                } else if (stringValue.matches("^-?\\d+[SCB]$")) {
                    char typeChar = Character.toUpperCase(stringValue.charAt(stringValue.length() - 1));
                    switch (typeChar) {
                        case 'S':
                            return Short.parseShort(stringValue.substring(0, stringValue.length() - 1));
                        case 'C':
                            return stringValue.charAt(0);
                        case 'B':
                            return Byte.parseByte(stringValue.substring(0, stringValue.length() - 1));
                    }
                }
                Object converted = registry.tryFromSerialized(stringValue);
                return Objects.requireNonNullElse(converted, stringValue);
            } catch (NumberFormatException ignored) {
            }
        }
        return registry.tryFromSerialized(value);
    }

    public static Map<String, String> parseMap(String mapStr) {
        Map<String, String> map = new HashMap<>();
        String[] entries = mapStr.split(",");
        for (String entry : entries) {
            String[] keyValue = entry.split("=");
            map.put(keyValue[0].trim(), keyValue[1].trim());
        }
        return map;
    }

    public static boolean legacyServer() {
        String serverVersion = Bukkit.getServer().getVersion();
        return serverVersion.contains("1.12") ||
                serverVersion.contains("1.11") ||
                serverVersion.contains("1.10") ||
                serverVersion.contains("1.9") ||
                serverVersion.contains("1.8") ||
                serverVersion.contains("1.7");
    }

    /**
     * Recursively converts an object to a plain Java type using the converter registry.
     * Handles Map, Iterable, and custom objects.
     */
    public static Object recursiveConvertForSerialization(Object value, ConfigTypeConverterRegistry registry) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey(), recursiveConvertForSerialization(entry.getValue(), registry));
            }
            return result;
        }
        if (value instanceof Iterable<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object v : list) result.add(recursiveConvertForSerialization(v, registry));
            return result;
        }
        Object serialized = registry.tryToSerialized(value);
        if (serialized != null && serialized != value) {
            if (serialized instanceof Map<?, ?> || serialized instanceof Iterable<?>) {
                return recursiveConvertForSerialization(serialized, registry);
            }
            return serialized;
        }
        return value;
    }

    /**
     * Recursively converts a plain Java type to a custom object using the converter registry.
     * Handles Map, Iterable, and custom types.
     */
    public static Object recursiveConvertFromSerialization(Object value, ConfigTypeConverterRegistry registry) {
        if (value instanceof Map<?, ?> map) {
            Map<Object, Object> result = new LinkedHashMap<>();
            for (Map.Entry<?, ?> entry : map.entrySet()) {
                result.put(entry.getKey(), recursiveConvertFromSerialization(entry.getValue(), registry));
            }
            return result;
        }
        if (value instanceof Iterable<?> list) {
            List<Object> result = new ArrayList<>();
            for (Object v : list) result.add(recursiveConvertFromSerialization(v, registry));
            return result;
        }
        Object converted = registry.tryFromSerialized(value);
        return converted != null ? converted : value;
    }

    public static String getFileExtension(File file) {
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public static boolean isValidPath(String s) {
        if (s == null || s.isEmpty()) return false;
        if (s.contains("..")) return false; // Prevent directory traversal
        boolean invalidChars = s.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != '/' && c != '.');
        if (invalidChars) return false;
        boolean startsWithSlash = s.startsWith("/") || s.startsWith("\\");
        boolean endsWithSlash = s.endsWith("/") || s.endsWith("\\");
        if (startsWithSlash || endsWithSlash) return false;
        boolean startsWithDot = s.startsWith(".");
        boolean endsWithDot = s.endsWith(".");
        return !startsWithDot && !endsWithDot;
    }

    public static String sanitize(String helloWorld) {
        if (helloWorld == null) return "";
        String sanitized = helloWorld.replaceAll("[^a-zA-Z0-9_ ]+", "_");
        sanitized = sanitized.trim().replaceAll(" +", "_").toLowerCase(Locale.ROOT);
        return sanitized.replaceAll("^_+|_+$", "");
    }

    public static boolean parseBoolean(String aTrue) {
        if (aTrue == null || aTrue.isEmpty()) return false;
        String lowerCase = aTrue.toLowerCase(Locale.ROOT);
        return lowerCase.equals("true") || lowerCase.equals("yes") || lowerCase.equals("1") ||
                lowerCase.equals("on") || lowerCase.equals("enabled");
    }

    public static Number parseNumber(String number) {
        if (number == null || number.isEmpty()) return null;
        try {
            if (number.matches("^-?\\d+$")) {
                return Integer.parseInt(number);
            } else if (number.matches("^-?\\d+\\.\\d+$")) {
                return Double.parseDouble(number);
            } else if (number.matches("^-?\\d+L$")) {
                return Long.parseLong(number.substring(0, number.length() - 1));
            } else if (number.matches("^-?\\d+\\.\\d+F$")) {
                return Float.parseFloat(number.substring(0, number.length() - 1));
            } else if (number.matches("^-?\\d+[SCB]$")) {
                char typeChar = Character.toUpperCase(number.charAt(number.length() - 1));
                switch (typeChar) {
                    case 'S':
                        return Short.parseShort(number.substring(0, number.length() - 1));
                    case 'B':
                        return Byte.parseByte(number.substring(0, number.length() - 1));
                }
            }
            return Double.parseDouble(number);
        } catch (NumberFormatException e) {
            if (CoolStuffLib.getLib() != null) {
                CoolStuffLib.getLib().getPluginFileLogger()
                        .writeToLog(Level.WARNING, "Failed to parse number: " + number + ". Returning 0.", LogPrefix.ERROR, true);
            }
            return null;
        }
    }

    public static void createDirectories(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            if (!file.exists()) {
                file.mkdirs();
            }
        } else {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
        }
    }

    public static void copyFile(File file, File file1) {
        if (file == null || file1 == null) return;
        if (!file.exists()) {
            CoolStuffLib.getLib().getPluginFileLogger()
                    .writeToLog(Level.WARNING, "File " + file.getName() + " does not exist. Cannot copy.", LogPrefix.ERROR, true);
            return;
        }
        createDirectories(file1);
        try (InputStream in = new FileInputStream(file); OutputStream out = new FileOutputStream(file1)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            CoolStuffLib.getLib().getPluginFileLogger()
                    .writeToLog(Level.SEVERE, "Failed to copy file: " + e.getMessage(), LogPrefix.ERROR, true);
        }
    }

    public static void deleteDirectory(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) {
                    deleteDirectory(f);
                }
            }
        }
        if (!file.delete()) {
            CoolStuffLib.getLib().getPluginFileLogger()
                    .writeToLog(Level.WARNING, "Failed to delete file: " + file.getAbsolutePath(), LogPrefix.ERROR, true);
        }
    }

    public static String joinPath(String separator, String... strings) {
        if (strings == null || strings.length == 0) return "";
        StringBuilder sb = new StringBuilder();
        for (String string : strings) {
            if (string != null && !string.isEmpty()) {
                if (!sb.isEmpty()) sb.append(separator);
                sb.append(string);
            }
        }
        return sb.toString();
    }

    public static String[] splitPath(String separator, String s) {
        if (s == null || s.isEmpty()) return new String[0];
        String[] parts = s.split(Pattern.quote(separator));
        List<String> result = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) {
                result.add(part);
            }
        }
        return result.toArray(new String[0]);
    }

    public static String formatDuration(long i, TimeUnit inputUnit) {
        // Format the duration in a human-readable way like "1d 2h 3m 4s"
        long seconds = inputUnit.toSeconds(i);
        long days = seconds / 86400;
        seconds %= 86400;
        long hours = seconds / 3600;
        seconds %= 3600;
        long minutes = seconds / 60;
        seconds %= 60;
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append("d ");
        if (hours > 0) sb.append(hours).append("h ");
        if (minutes > 0) sb.append(minutes).append("m ");
        if (seconds > 0 || sb.isEmpty()) sb.append(seconds).append("s");
        return sb.toString().trim();
    }


    public static void logMalformedLine(File file, int lineNum, String line, String message) {
        StringBuilder context = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            int current = 1;
            String l;
            while ((l = reader.readLine()) != null) {
                if (current >= lineNum - 2 && current <= lineNum + 2) {
                    context.append(current).append(": ").append(l).append("\n");
                }
                if (current > lineNum + 2) break;
                current++;
            }
        } catch (Exception ignored) {
        }
        ConfigLogger.error("Malformed line in file: " + file.getPath() + " at line " + lineNum + ": " + message + "\nContext:\n" + context, null, "TomlConfigFileHandler", true);
    }

    public static void logError(String message, Exception e, String source, boolean console) {
        ConfigLogger.error(message, e, source, console);
    }

    public static String logPrefix(String testMessage) {
        String prefix = "§c[§6CoolStuffLib§c] §7";
        if (testMessage.startsWith("§c")) {
            prefix = "§c[§6CoolStuffLib§c] §7";
        } else if (testMessage.startsWith("§e")) {
            prefix = "§e[§6CoolStuffLib§e] §7";
        } else if (testMessage.startsWith("§a")) {
            prefix = "§a[§6CoolStuffLib§a] §7";
        }
        return prefix + testMessage;
    }
}
