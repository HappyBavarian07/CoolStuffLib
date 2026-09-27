package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public final class LanguageMigration {
    private static final DateTimeFormatter BACKUP_NAME = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

    private LanguageMigration() {
    }

    /** Copies every entry of {@code language} from one backend to another and compares both sides afterwards. */
    public static MigrationReport copy(LanguageBackend from, LanguageBackend to, String language) {
        ReadResult source = from.read(language);
        if (!source.problems().isEmpty()) {
            return new MigrationReport(language, 0, List.of(), source.problems(), null);
        }
        List<LanguageProblem> problems = new ArrayList<>(to.write(language, source.entries().values(), source.sectionComments()));
        ReadResult target = to.read(language);
        problems.addAll(target.problems());
        List<String> differences = new ArrayList<>();
        for (LanguageEntry entry : source.entries().values()) {
            LanguageEntry copied = target.entries().get(entry.key());
            if (copied == null || !Objects.equals(entry.value(), copied.value())) differences.add(entry.key());
        }
        return new MigrationReport(language, source.entries().size(), differences, problems, null);
    }

    /**
     * <p>Moves {@code languages/<lang>.yml} into the split layout when there is no {@code languages/<lang>/} yet.
     * The old file goes to {@code languages/_backup/<date>/}. On any difference the split folder is removed again
     * and the old file stays.</p>
     */
    public static @Nullable MigrationReport migrateLegacyFile(File folder, String language) {
        File legacy = new File(folder, language + ".yml");
        File split = new File(folder, language);
        if (!legacy.isFile() || split.exists()) return null;
        MigrationReport report = copy(new LegacyYamlBackend(folder), new SplitYamlBackend(folder), language);
        if (!report.ok()) {
            deleteTree(split.toPath());
            return report;
        }
        try {
            Path backupDir = folder.toPath().resolve("_backup").resolve(LocalDateTime.now().format(BACKUP_NAME));
            Files.createDirectories(backupDir);
            Path backup = backupDir.resolve(legacy.getName());
            Files.move(legacy.toPath(), backup, StandardCopyOption.REPLACE_EXISTING);
            return new MigrationReport(language, report.entries(), List.of(), List.of(), backup.toFile());
        } catch (IOException e) {
            deleteTree(split.toPath());
            return new MigrationReport(language, report.entries(), List.of(),
                    List.of(new LanguageProblem(legacy.getName(), 0, "Could not move the old file: " + e.getMessage())), null);
        }
    }

    /**
     * <p>The problem to report when a language has both the split folder and an old single file: the folder is used
     * and the file is dead weight, but it is the owner's file and therefore never deleted here.</p>
     */
    public static @Nullable LanguageProblem strayLegacyFile(File folder, String language) {
        File legacy = new File(folder, language + ".yml");
        if (!new File(folder, language).isDirectory() || !legacy.isFile()) return null;
        return new LanguageProblem(legacy.getName(), 0, "Ignored because the folder " + language
                + "/ is used instead. Move the keys you still need into it, then delete this file.");
    }

    private static void deleteTree(Path root) {
        if (!Files.exists(root)) return;
        try (Stream<Path> walk = Files.walk(root)) {
            walk.sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        } catch (IOException ignored) {
        }
    }
}
