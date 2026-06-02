package de.happybavarian07.coolstufflib.utils;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * <p>Utilities for file and directory manipulation, IO streams, and ZIP operations.</p>
 */
public final class FileUtils {

    private FileUtils() {}

    @Nullable
    public static InputStream getResource(@NotNull String filename) {
        try {
            URL url = FileUtils.class.getClassLoader().getResource(filename);
            if (url == null) return null;

            URLConnection connection = url.openConnection();
            connection.setUseCaches(false);
            return connection.getInputStream();
        } catch (IOException ex) {
            return null;
        }
    }

    public static void saveResource(File configFolder, @NotNull String resourcePath, boolean replace) {
        if (resourcePath == null || resourcePath.isEmpty()) {
            throw new IllegalArgumentException("ResourcePath cannot be null or empty");
        }

        resourcePath = resourcePath.replace('\\', '/');
        InputStream in = getResource(resourcePath);
        if (in == null) {
            throw new IllegalArgumentException("The embedded resource '" + resourcePath + "' cannot be found.");
        }

        File outFile = new File(configFolder, resourcePath);
        int lastIndex = resourcePath.lastIndexOf('/');
        File outDir = new File(configFolder, resourcePath.substring(0, Math.max(lastIndex, 0)));

        if (!outDir.exists()) outDir.mkdirs();

        try {
            if (outFile.exists() && !replace) {
                throw new IllegalStateException("Could not save " + outFile.getName() + " because it already exists.");
            }
            
            try (OutputStream out = new FileOutputStream(outFile)) {
                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
            }
        } catch (IOException ex) {
            throw new IllegalStateException("Could not save " + outFile.getName(), ex);
        } finally {
            try { in.close(); } catch (IOException ignored) {}
        }
    }

    public static void zipFiles(File[] files, String zipFile, File baseDir) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(zipFile);
             ZipOutputStream zos = new ZipOutputStream(fos)) {
            for (File file : files) {
                if (file == null || !file.exists()) continue;
                if (file.isDirectory()) {
                    zipDirIntoZipFile(zos, file, baseDir);
                    continue;
                }
                writeZipContentToStream(zos, file, baseDir);
            }
        }
    }

    private static void zipDirIntoZipFile(ZipOutputStream zos, File directory, File baseDir) throws IOException {
        if (!directory.isDirectory()) return;
        File[] filesInDir = directory.listFiles();
        if (filesInDir == null) return;
        
        for (File fileInDir : filesInDir) {
            if (fileInDir.isDirectory()) {
                zipDirIntoZipFile(zos, fileInDir, baseDir);
            } else {
                writeZipContentToStream(zos, fileInDir, baseDir);
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

    public static void unzipFiles(String zipFilePath, String destDir, boolean replace) {
        File dir = new File(destDir);
        if (!dir.exists()) dir.mkdirs();
        
        byte[] buffer = new byte[1024];
        try (FileInputStream fis = new FileInputStream(zipFilePath);
             ZipInputStream zis = new ZipInputStream(fis)) {
            
            ZipEntry ze;
            while ((ze = zis.getNextEntry()) != null) {
                String fileName = ze.getName();
                File newFile = new File(destDir + File.separator + fileName);
                
                if (ze.isDirectory()) {
                    newFile.mkdirs();
                    zis.closeEntry();
                    continue;
                }
                
                newFile.getParentFile().mkdirs();
                if (!replace && newFile.exists()) {
                    zis.closeEntry();
                    continue;
                }
                
                try (FileOutputStream fos = new FileOutputStream(newFile)) {
                    int len;
                    while ((len = zis.read(buffer)) > 0) {
                        fos.write(buffer, 0, len);
                    }
                }
                zis.closeEntry();
            }
        } catch (IOException e) {
            if (CoolStuffLib.getLib() != null && CoolStuffLib.getLib().getPluginFileLogger() != null) {
                CoolStuffLib.getLib().getPluginFileLogger()
                        .writeToLog(Level.SEVERE, "Failed to unzip files: " + e.getMessage(), LogPrefix.ERROR, true);
            }
        }
    }

    public static String getFileExtension(File file) {
        if (file == null) return "";
        String fileName = file.getName();
        int dotIndex = fileName.lastIndexOf('.');
        if (dotIndex > 0 && dotIndex < fileName.length() - 1) {
            return fileName.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
        }
        return "";
    }

    public static boolean isValidPath(String s) {
        if (s == null || s.isEmpty() || s.contains("..")) return false;
        boolean invalidChars = s.chars().anyMatch(c -> !Character.isLetterOrDigit(c) && c != '-' && c != '_' && c != '/' && c != '.');
        if (invalidChars) return false;
        
        boolean startsWithSlash = s.startsWith("/") || s.startsWith("\\");
        boolean endsWithSlash = s.endsWith("/") || s.endsWith("\\");
        if (startsWithSlash || endsWithSlash) return false;
        
        return !s.startsWith(".") && !s.endsWith(".");
    }

    public static void createDirectories(File file) {
        if (file == null) return;
        if (file.isDirectory()) {
            if (!file.exists()) file.mkdirs();
            return;
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }
    }

    public static void copyFile(File src, File dest) {
        if (src == null || dest == null || !src.exists()) return;
        createDirectories(dest);
        try (InputStream in = new FileInputStream(src); OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            if (CoolStuffLib.getLib() != null && CoolStuffLib.getLib().getPluginFileLogger() != null) {
                CoolStuffLib.getLib().getPluginFileLogger()
                        .writeToLog(Level.SEVERE, "Failed to copy file: " + e.getMessage(), LogPrefix.ERROR, true);
            }
        }
    }

    public static void deleteDirectory(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File f : files) deleteDirectory(f);
            }
        }
        file.delete();
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
            if (!part.isEmpty()) result.add(part);
        }
        return result.toArray(new String[0]);
    }
}
