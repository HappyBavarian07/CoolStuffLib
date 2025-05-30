package de.happybavarian07.coolstufflib.utils;/*
 * @Author HappyBavarian07
 * @Date 24.04.2023 | 17:24
 */

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.configstuff.advanced.filetypes.ConfigTypeConverterRegistry;
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
import java.util.logging.Level;
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
                return null; // Or handle it as needed
            }
        } catch (ClassNotFoundException e) {
            System.err.println("Class not found: " + fullClassName);
            if (CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Class not found: " + fullClassName, LogPrefix.ERROR, true);
            return null; // You can modify the return value based on your needs
        } catch (IllegalAccessException | InstantiationException e) {
            System.err.println("Error creating an instance: " + fullClassName);
            if (CoolStuffLib.getLib().getPluginFileLogger() != null)
                CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "Error creating an instance: " + fullClassName, LogPrefix.ERROR, true);
            return null; // You can modify the return value based on your needs
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
     * Creates a skull with a head texture and a name from a String.
     * <br>
     * Use {@link #createSkull(Head, String)}, if there's a need to use predefined Head Values.
     *
     * @param headTexture The head texture as a String
     * @param name
     * @return
     */
    public static ItemStack createSkull(String headTexture, String name) {
        ItemStack head = new ItemStack(legacyServer() ? Objects.requireNonNull(Material.matchMaterial("SKULL_ITEM")) : Material.PLAYER_HEAD, 1);
        if (headTexture.isEmpty()) return head;

        SkullMeta meta = (SkullMeta) head.getItemMeta();
        assert meta != null;
        meta.setDisplayName(Utils.chat(name));
        PlayerProfile profile = Bukkit.createPlayerProfile(UUID.randomUUID(), "CustomHead");

        try {
            profile.getTextures().setSkin(new URL("https://textures.minecraft.net/texture/" + headTexture));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
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
        if (value instanceof Map) {
            for (Map.Entry<String, Object> entry : ((Map<String, Object>) value).entrySet()) {
                String newPrefix = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
                map.putAll(flatten(registry, newPrefix, entry.getValue()));
            }
        } else if (value instanceof Collection) {
            int index = 0;
            for (Object item : (Collection<?>) value) {
                String newPrefix = prefix + "[" + index++ + "]";
                map.putAll(flatten(registry, newPrefix, item));
            }
        } else {
            map.put(prefix, registry.tryToSerialized(value));
        }
        return map;
    }

    public static Map<String, Object> unflatten(ConfigTypeConverterRegistry registry, Map<String, String> map) {
        Map<String, Object> result = new HashMap<>();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String key = entry.getKey();
            String[] parts = key.split("\\.");
            insertUnflattened(result, parts, 0, entry.getValue(), registry);
        }
        return result;
    }

    private static void insertUnflattened(Map<String, Object> current, String[] parts, int index, String value, ConfigTypeConverterRegistry registry) {
        String part = parts[index];
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([a-zA-Z0-9_]+)(\\[(\\d+)])?");
        java.util.regex.Matcher matcher = pattern.matcher(part);
        
        // If the part doesn't match the pattern, add it as-is and return
        if (!matcher.matches()) {
            if (index == parts.length - 1) {
                current.put(part, convertValue(registry, value));
            } else {
                // If there are more parts, create a nested map
                Map<String, Object> nested = new HashMap<>();
                current.put(part, nested);
                insertUnflattened(nested, parts, index + 1, value, registry);
            }
            return;
        }
        
        String key = matcher.group(1);
        String idxStr = matcher.group(3);
        boolean isLast = index == parts.length - 1;
        
        try {
            if (idxStr != null) {
                int listIndex = Integer.parseInt(idxStr);
                @SuppressWarnings("unchecked")
                List<Object> list = (List<Object>) current.computeIfAbsent(key, k -> new ArrayList<>());
                // Ensure the list is large enough
                while (list.size() <= listIndex) {
                    list.add(null);
                }
                if (isLast) {
                    list.set(listIndex, convertValue(registry, value));
                } else {
                    Object next = list.get(listIndex);
                    if (!(next instanceof Map)) {
                        next = new HashMap<String, Object>();
                        list.set(listIndex, next);
                    }
                    insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
                }
            } else {
                if (isLast) {
                    current.put(key, convertValue(registry, value));
                } else {
                    Object next = current.computeIfAbsent(key, k -> new HashMap<String, Object>());
                    if (!(next instanceof Map)) {
                        // If the value is not a map, replace it with a new map
                        next = new HashMap<String, Object>();
                        current.put(key, next);
                    }
                    insertUnflattened((Map<String, Object>) next, parts, index + 1, value, registry);
                }
            }
        } catch (NumberFormatException e) {
            // If we can't parse the index, treat it as a regular key
            if (isLast) {
                current.put(part, convertValue(registry, value));
            } else {
                Map<String, Object> nested = new HashMap<>();
                current.put(part, nested);
                insertUnflattened(nested, parts, index + 1, value, registry);
            }
        }
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
}
