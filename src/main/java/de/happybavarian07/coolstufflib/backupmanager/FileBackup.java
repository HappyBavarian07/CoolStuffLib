package de.happybavarian07.coolstufflib.backupmanager;/*
 * @Author HappyBavarian07
 * @Date 28.01.2023 | 16:08
 */


import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.utils.LogPrefix;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.logging.Level;

public class FileBackup implements Comparable<FileBackup> {
    private String identifier;
    private File[] filesToBackup;
    // Relative to the Admin-Panel Plugin Folder
    private File destinationPathToBackupToo;
    private File rootDirectory;
    private List<File> backupsDone;

    /**
     * Creates a FileBackup instance with the specified parameters.
     *
     * @param indentifier                Unique identifier for the backup.
     * @param filesToBackup              Array of files to be backed up.
     * @param destinationPathToBackupToo The directory where backups will be stored.
     * @param rootDirectory              The root directory where files will be searched for regex and unzipped after backup.
     */
    public FileBackup(String indentifier, File[] filesToBackup, File destinationPathToBackupToo, File rootDirectory) {
        this.identifier = indentifier;
        this.filesToBackup = filesToBackup;
        this.rootDirectory = rootDirectory;
        this.destinationPathToBackupToo = destinationPathToBackupToo;
        if (destinationPathToBackupToo == null || !destinationPathToBackupToo.isDirectory()) {
            throw new IllegalArgumentException("Destination path to backup too must be a valid directory.");
        }
        if (rootDirectory == null || !rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root directory must be a valid directory.");
        }
        this.backupsDone = new ArrayList<>();
        fillBackupsDoneIfEmpty();
    }

    /**
     * Creates a FileBackup instance with the specified parameters.
     *
     * @param identifier                 Unique identifier for the backup.
     * @param fileFilters                List of RegexFileFilter to include files in the backup.
     * @param excludeFilters             List of RegexFileFilter to exclude files from the backup.
     * @param destinationPathToBackupToo The directory where backups will be stored.
     * @param rootDirectory              The root directory where files will be searched for regex and unzipped after backup.
     */
    public FileBackup(String identifier, List<RegexFileFilter> fileFilters, List<RegexFileFilter> excludeFilters, File destinationPathToBackupToo, File rootDirectory) {
        this.identifier = identifier;
        this.rootDirectory = rootDirectory;
        this.destinationPathToBackupToo = destinationPathToBackupToo;
        if (destinationPathToBackupToo == null || !destinationPathToBackupToo.isDirectory()) {
            throw new IllegalArgumentException("Destination path to backup too must be a valid directory.");
        }
        if (rootDirectory == null || !rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root directory must be a valid directory.");
        }
        this.filesToBackup = getFilesFromFolderWithRegex(rootDirectory, fileFilters, excludeFilters).toArray(new File[0]);
        this.backupsDone = new ArrayList<>();
        fillBackupsDoneIfEmpty();
    }

    private List<File> getFilesFromFolderWithRegex(File folder, List<RegexFileFilter> fileFilters, List<RegexFileFilter> excludeFilters) {
        List<File> files = new ArrayList<>();
        for (File file : Objects.requireNonNull(folder.listFiles())) {
            if (file.isDirectory()) {
                files.addAll(getFilesFromFolderWithRegex(file, fileFilters, excludeFilters));
            } else {
                for (RegexFileFilter fileFilter : fileFilters) {
                    if (fileFilter.accept(file)) {
                        boolean exclude = false;
                        for (RegexFileFilter excludeFilter : excludeFilters) {
                            if (excludeFilter.accept(file)) {
                                exclude = true;
                                break;
                            }
                        }
                        if (!exclude) files.add(file);
                    }
                }
            }
        }
        return files;
    }

    public void fillBackupsDoneIfEmpty() {
        if (!backupsDone.isEmpty()) {
            return;
        }
        File[] filesInBackupFolder = destinationPathToBackupToo.listFiles();
        if (filesInBackupFolder == null) {
            return;
        }
        Arrays.stream(filesInBackupFolder).filter(f -> (f.getParentFile().getName() + "/" + f.getName()).contains(identifier + "_")).forEach(this::addBackupDone);
    }

    /**
     * Error Codes:
     * 0 = Success,
     * -1 = Files to Backup is null or there are none,
     * -2 = IO Exception,
     * -3 = There is no Destination to backup too or its not a Directory,
     * -4 = The Backups exceed the maxBackups Number
     *
     * @param maxBackups Number of Backups before deleting existing ones
     * @return Error Code
     */
    public int backup(int maxBackups, boolean removeOldBackupsIfHitsCap) {
        if (maxBackups <= 0) maxBackups = 1;
        if (filesToBackup == null || filesToBackup.length == 0) return -1;
        if (destinationPathToBackupToo == null || !destinationPathToBackupToo.isDirectory()) return -3;
        try {
            File zipFile = new File(destinationPathToBackupToo, identifier + "_" + (backupsDone.size() + 1) + ".zip");
            if (backupsDone.size() >= maxBackups && removeOldBackupsIfHitsCap) {
                while (backupsDone.size() >= maxBackups) {
                    removeOldestBackup();
                }
            } else if (backupsDone.size() >= maxBackups) {
                return -4;
            }
            if (backupsDone.contains(zipFile)) {
                for (int i = 1; i <= maxBackups; i++) {
                    if (!backupsDone.contains(new File(destinationPathToBackupToo, identifier + "_" + (backupsDone.size() + 1) + ".zip"))) {
                        zipFile = new File(destinationPathToBackupToo, identifier + "_" + (backupsDone.size() + 1) + ".zip");
                        break;
                    }
                }
            }
            Utils.zipFiles(filesToBackup, zipFile.getAbsolutePath(), rootDirectory);
            backupsDone.add(zipFile);
            return 0;
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }
    }

    public void removeLastBackup() {
        File fileToRemove = new File(destinationPathToBackupToo, identifier + "_" + (backupsDone.size()) + ".zip");
        if (fileToRemove.delete()) backupsDone.remove(fileToRemove);
    }

    public void removeOldestBackup() {
        Map<Long, File> backupFileDates = new HashMap<>();
        for (File backupFile : backupsDone) {
            backupFileDates.put(backupFile.lastModified(), backupFile);
        }
        if (backupFileDates.isEmpty()) return;
        long minBackupFileDate = Collections.min(backupFileDates.keySet());
        File oldest = backupFileDates.get(minBackupFileDate);
        if (minBackupFileDate == 0 || oldest == null) return;
        backupsDone.remove(oldest);
    }

    public File getNewestBackupFile() {
        Map<Long, File> backupFileDates = new HashMap<>();
        for (File backupFile : backupsDone) {
            backupFileDates.put(backupFile.lastModified(), backupFile);
        }
        long maxBackupFileDate = Collections.max(backupFileDates.keySet());
        if (maxBackupFileDate == 0) return null;
        if (backupFileDates.get(maxBackupFileDate).exists())
            return backupFileDates.get(maxBackupFileDate);
        return null;
    }

    /**
     * Error Codes:
     * 0 = Success,
     * -1 = Files to Backup is null or there are none,
     * -2 = IO Exception,
     * -3 = Zip File is null or doesn't exist
     *
     * @param zipFile The Zip File
     * @return Error Code
     */
    public int loadBackup(File zipFile) {
        if (filesToBackup == null || filesToBackup.length == 0) return -1;
        if (zipFile == null || !zipFile.exists()) return -3;
        try {
            File zipFileTemp = getCurrentSettingsBackupZipName();
            Utils.zipFiles(filesToBackup, zipFileTemp.getAbsolutePath(), rootDirectory);
        } catch (IOException e) {
            e.printStackTrace();
            return -2;
        }

        for (File f : filesToBackup) {
            new File(f.getAbsolutePath()).delete();
        }

        Utils.unzipFiles(zipFile.getAbsolutePath(), rootDirectory.getAbsolutePath(), true);
        return 0;
    }

    private @NotNull File getCurrentSettingsBackupZipName() {
        /*if (zipFileTemp.exists()) {
            int i = 1;
            while (zipFileTemp.exists() && i <= 5) {
                zipFileTemp = new File(rootDirectory + File.separator + "old_or_corrupted_configs_" + i + ".zip");
                i++;
            }
        }*/
        return new File(rootDirectory + File.separator + "old_or_corrupted_configs.zip");
    }

    public int loadSpecificFilesFromBackup(File zipFile, File[] filesToLoad) {
        List<File> internalFilesToBackupList = new ArrayList<>(Arrays.stream(filesToBackup).toList());
        internalFilesToBackupList.removeIf(f -> !Arrays.asList(filesToLoad).contains(f));

        if (filesToBackup.length == 0) return -1;
        if (zipFile == null || !zipFile.exists()) return -3;
        try {
            File zipFileTemp = getCurrentSettingsBackupZipName();
            Utils.zipFiles(internalFilesToBackupList.toArray(new File[0]), zipFileTemp.getAbsolutePath(), rootDirectory);
        } catch (IOException e) {
            CoolStuffLib.getLib().getPluginFileLogger().writeToLog(Level.SEVERE, "An Error occurred while creating a backup of the old files: " + e.getMessage(), LogPrefix.FILE, true);
            return -2;
        }

        for (File f : filesToLoad) {
            new File(f.getAbsolutePath()).delete();
        }

        Utils.unzipFiles(zipFile.getAbsolutePath(), rootDirectory.getAbsolutePath(), false);
        return 0;
    }

    /**
     * Error Codes:
     * 0 = Success,
     * -1 = Files to Backup is null or there are none,
     * -2 = IO Exception,
     * -3 = Zip File is null or doesn't exist
     *
     * @param number The Zip File Number from the BackupsDone List (-1 = newest)
     * @return Error Code
     */
    public int deleteZipBackup(int number) {
        File zipFile = number == -1 ? getNewestBackupFile() : getBackupFileFromNumber(number);

        if (filesToBackup == null || filesToBackup.length == 0) return -1;
        if (zipFile == null || !zipFile.exists()) return -3;

        if (zipFile.delete()) return 0;
        else return -2;
    }

    /**
     * Error Codes:
     * 0 = Success,
     * -1 = Files to Backup is null or there are none,
     * -2 = IO Exception,
     * -3 = Zip File is null or doesn't exist
     *
     * @param zipFile The Zip File
     * @return Error Code
     */
    public int deleteZipBackup(File zipFile) {
        if (filesToBackup == null || filesToBackup.length == 0) return -1;
        if (zipFile == null || !zipFile.exists()) return -3;

        if (zipFile.delete()) return 0;
        else return -2;
    }

    public File getBackupFileFromNumber(int number) {
        if(number >= backupsDone.size()) {
            throw new IndexOutOfBoundsException("Backup number is out of bounds: " + number);
        }
        if (number < 0) {
            return getNewestBackupFile();
        }
        return backupsDone.get(number);
    }

    public File getBackupFromFileName(String filename) {
        return backupsDone.stream().filter(f -> f.getName().equalsIgnoreCase(filename)).findFirst().get();
    }

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public File[] getFilesToBackup() {
        return filesToBackup;
    }

    public void setFilesToBackup(File[] filesToBackup) {
        this.filesToBackup = filesToBackup;
    }

    public File getDestinationPathToBackupToo() {
        return destinationPathToBackupToo;
    }

    public void setDestinationPathToBackupToo(File destinationPathToBackupToo) {
        if (destinationPathToBackupToo == null || !destinationPathToBackupToo.isDirectory()) {
            throw new IllegalArgumentException("Destination path to backup too must be a valid directory.");
        }
        this.destinationPathToBackupToo = destinationPathToBackupToo;
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public void setRootDirectory(File rootDirectory) {
        if (rootDirectory == null || !rootDirectory.isDirectory()) {
            throw new IllegalArgumentException("Root directory must be a valid directory.");
        }
        this.rootDirectory = rootDirectory;
    }

    public List<File> getBackupsDone() {
        return backupsDone;
    }

    public void setBackupsDone(List<File> backupsDone) {
        this.backupsDone = backupsDone;
    }

    public void addBackupDone(File backupDone) {
        if (backupDone == null || !backupDone.exists()) {
            throw new IllegalArgumentException("Backup file must not be null and must exist.");
        }
        if (backupsDone.contains(backupDone)) {
            return;
        }
        backupsDone.add(backupDone);
    }

    @Override
    public int compareTo(FileBackup o) {
        int identifierComparison = this.identifier.compareTo(o.identifier);
        if (identifierComparison != 0) {
            return identifierComparison;
        }

        int destinationPathComparison = this.destinationPathToBackupToo.compareTo(o.destinationPathToBackupToo);
        if (destinationPathComparison != 0) {
            return destinationPathComparison;
        }

        return this.filesToBackup.length - o.filesToBackup.length;
    }
}
