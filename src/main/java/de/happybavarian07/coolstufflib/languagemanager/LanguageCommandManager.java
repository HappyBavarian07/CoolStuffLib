package de.happybavarian07.coolstufflib.languagemanager;

import de.happybavarian07.coolstufflib.commandmanagement.Argument;
import de.happybavarian07.coolstufflib.commandmanagement.CommandData;
import de.happybavarian07.coolstufflib.commandmanagement.CommandManager;
import de.happybavarian07.coolstufflib.commandmanagement.HelpCommand;
import de.happybavarian07.coolstufflib.languagemanager.storage.LanguageStorage;
import de.happybavarian07.coolstufflib.languagemanager.storage.MigrationReport;
import de.happybavarian07.coolstufflib.utils.Utils;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;
import java.util.Locale;

/** {@code /<name> reload|debug|find|missing|migrate} for server owners; enabled with {@code enableCommands(name)}. */
@CommandData
public class LanguageCommandManager extends CommandManager {
    private final LanguageManager languages;
    private final JavaPlugin plugin;
    private final String name;

    public LanguageCommandManager(LanguageManager languages, JavaPlugin plugin, String name) {
        this.languages = languages;
        this.plugin = plugin;
        this.name = name;
    }

    @Override
    public String getCommandName() {
        return name;
    }

    @Override
    public String getCommandUsage() {
        return "/" + name + " <reload|debug|find|missing|migrate>";
    }

    @Override
    public String getCommandInfo() {
        return "Tools for the language files";
    }

    @Override
    public JavaPlugin getJavaPlugin() {
        return plugin;
    }

    @Override
    public List<String> getCommandAliases() {
        return List.of();
    }

    @Override
    public String getCommandPermissionAsString() {
        return plugin.getName().toLowerCase(Locale.ROOT) + ".lang";
    }

    @Override
    public boolean autoRegisterPermission() {
        return true;
    }

    @Override
    public void setup() {
        String permission = getCommandPermissionAsString() + ".";
        registerSubCommand(new HelpCommand(getCommandName()));
        registerSubCommand(subCommand("reload").permission(permission + "reload").info("Reloads the language files")
                .executes((sender, args) -> {
                    languages.reloadLanguages(null, false);
                    List<String> report = languages.getLastReport();
                    send(sender, report.isEmpty() ? "&aReloaded, nothing to report." : "&aReloaded:");
                    report.forEach(line -> send(sender, "&7- " + line));
                    return true;
                }).build());
        registerSubCommand(subCommand("debug").permission(permission + "debug").info("Shows the key next to every text")
                .executes((sender, args) -> {
                    send(sender, languages.toggleDebug(sender) ? "&aKeys are shown now." : "&cKeys are hidden again.");
                    return true;
                }).build());
        registerSubCommand(subCommand("find").permission(permission + "find").info("Finds the key of a text you see in game")
                .arguments(Argument.text("text"))
                .executes((sender, args) -> {
                    String language = sender instanceof Player player
                            ? languages.getLangOrPlayerLang(true, languages.getCurrentLangName(), player).getLangName()
                            : languages.getCurrentLangName();
                    List<String> hits = languages.findKeys(language, args.joined(0), 10);
                    send(sender, hits.isEmpty() ? "&cNo text contains that." : "&aFound in " + language + ":");
                    hits.forEach(hit -> send(sender, "&7- " + hit));
                    return true;
                }).build());
        registerSubCommand(subCommand("missing").permission(permission + "missing").info("Lists keys missing in or unknown to the files")
                .arguments(Argument.custom("language", sender -> languages.getRegisteredLanguages().keySet()).optional())
                .executes((sender, args) -> {
                    String language = args.string(0, languages.getCurrentLangName());
                    LanguageManager.MissingKeys missing = languages.missingKeys(language);
                    send(sender, "&aMissing in the files of " + language + ": &f" + missing.missingInFiles().size());
                    missing.missingInFiles().stream().limit(20).forEach(key -> send(sender, "&7- " + key));
                    send(sender, "&aNot used by the plugin: &f" + missing.unknownToPlugin().size());
                    missing.unknownToPlugin().stream().limit(20).forEach(key -> send(sender, "&7- " + key));
                    return true;
                }).build());
        registerSubCommand(subCommand("migrate").permission(permission + "migrate").info("Moves all languages to another storage")
                .arguments(Argument.custom("backend", sender -> LanguageStorage.backendIds()))
                .confirm()
                .executes((sender, args) -> {
                    List<MigrationReport> reports = languages.migrateAll(args.string(0));
                    reports.forEach(report -> send(sender, (report.ok() ? "&a" : "&c") + report.summary()));
                    send(sender, reports.stream().allMatch(MigrationReport::ok) ? "&aNow using " + args.string(0) + "." : "&cNothing changed.");
                    return true;
                }).build());
    }

    private void send(CommandSender sender, String text) {
        sender.sendMessage(Utils.chat(text));
    }
}
