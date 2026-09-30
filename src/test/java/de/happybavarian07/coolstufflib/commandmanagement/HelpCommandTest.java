package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.languagemanager.LanguageManager;
import de.happybavarian07.coolstufflib.languagemanager.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.permissions.Permission;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class HelpCommandTest {
    private final List<String> sent = new ArrayList<>();
    private HelpCommand help;
    private CommandSender console;
    private MockedStatic<CoolStuffLib> statics;
    private MockedStatic<Bukkit> bukkit;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        statics = mockStatic(CoolStuffLib.class);
        statics.when(CoolStuffLib::getLib).thenThrow(new RuntimeException("not initialized"));
        bukkit = mockStatic(Bukkit.class);
        bukkit.when(Bukkit::getOnlinePlayers).thenAnswer(invocation -> List.of());

        LanguageManager lgm = mock(LanguageManager.class);
        when(lgm.getMessage(anyString(), any(), anyBoolean())).thenAnswer(invocation -> invocation.getArgument(0));
        when(lgm.replacePlaceholders(anyString(), anyMap())).thenAnswer(invocation -> {
            Map<String, Placeholder> placeholders = invocation.getArgument(1);
            return invocation.getArgument(0) + "|" + placeholders.get("%usage%").value();
        });
        when(lgm.getPrefix()).thenReturn("");

        SubCommand info = new SubCommandBuilder("admin", "info").info("Shows info").aliases("i")
                .arguments(Argument.player("target").optional()).executes((s, a) -> true).build();
        SubCommand secret = new SubCommandBuilder("admin", "secret").executes((s, a) -> true).build();
        SubCommand fly = new SubCommandBuilder("admin", "fly").player((p, a) -> {}).build();
        SubCommand kick = new SubCommandBuilder("admin", "kick").confirm().executes((s, a) -> true).build();
        SubCommand player = new SubCommandBuilder("admin", "player").sub(kick).build();
        help = new HelpCommand("admin");

        ArgumentTest.SingleSubManager manager = new ArgumentTest.SingleSubManager(info);
        manager.registerSubCommand(secret);
        manager.registerSubCommand(fly);
        manager.registerSubCommand(player);
        manager.registerSubCommand(help);

        CommandManagerRegistry registry = mock(CommandManagerRegistry.class);
        when(registry.getCommandManager("admin")).thenReturn(manager);
        help.setDependencies(null, lgm, registry);

        console = mock(CommandSender.class);
        when(console.hasPermission(any(Permission.class)))
                .thenAnswer(invocation -> !invocation.<Permission>getArgument(0).getName().equals("admin.secret"));
        doAnswer(invocation -> sent.add(invocation.getArgument(0))).when(console).sendMessage(anyString());
    }

    @AfterEach
    void tearDown() {
        statics.close();
        bukkit.close();
    }

    private void run(String... args) {
        help.execute(console, new CommandArgs(console, args, help.arguments()));
    }

    @Test
    void listsVisibleCommandsSortedByPath() {
        run();
        assertEquals(List.of(
                "Player.Commands.HelpMessages.Header",
                "Player.Commands.HelpMessages.Format|/admin help [page|command]",
                "Player.Commands.HelpMessages.Format|/admin info [target]",
                "Player.Commands.HelpMessages.Format|/admin player <kick>",
                "Player.Commands.HelpMessages.Format|/admin player kick",
                "Player.Commands.HelpMessages.Footer"), sent);
    }

    @Test
    void unknownPageIsReported() {
        run("5");
        assertEquals(List.of("Player.Commands.HelpPageDoesNotExist"), sent);
    }

    @Test
    void detailsForNestedCommand() {
        run("player", "kick");
        String joined = String.join("\n", sent);
        assertTrue(joined.contains("/admin player kick"), joined);
        assertTrue(joined.contains("admin.player.kick"), joined);
        assertTrue(joined.contains("needs confirmation"), joined);
    }

    @Test
    void detailsIncludeAliasesAndArguments() {
        run("i");
        String joined = String.join("\n", sent);
        assertTrue(joined.contains("Shows info"), joined);
        assertTrue(joined.contains("Aliases: " + ChatColor.WHITE + "i"), joined);
        assertTrue(joined.contains("[target]"), joined);
    }

    @Test
    void hiddenOrUnknownCommandsGetNoDetails() {
        run("secret");
        run("fly");
        run("nope");
        assertEquals(3, sent.size());
        assertTrue(sent.get(2).contains("nope"));
        assertTrue(sent.stream().allMatch(message -> message.contains("No help found")));
    }

    @Test
    void completionOffersPagesVisibleCommandsAndChildren() {
        Map<Integer, String[]> first = help.subArgs(console, 0, new String[]{""});
        List<String> options = Arrays.asList(first.get(1));
        assertTrue(options.containsAll(List.of("1", "info", "player", "help")));
        assertFalse(options.contains("secret"));
        assertFalse(options.contains("fly"));

        Map<Integer, String[]> second = help.subArgs(console, 0, new String[]{"player", ""});
        assertArrayEquals(new String[]{"kick"}, second.get(2));
    }

    @Test
    void consumerThatRegistersNothingGetsHelpAutomatically() {
        SubCommand info = new SubCommandBuilder("admin", "info").executes((s, a) -> true).build();
        ArgumentTest.SingleSubManager manager = new ArgumentTest.SingleSubManager(info);
        assertNull(manager.getSubCommand("help"));

        List<SubCommand> all = manager.getAllSubCommands();

        SubCommand registered = manager.getSubCommand("help");
        assertInstanceOf(HelpCommand.class, registered);
        assertSame(registered, manager.getSubCommand("help"));
        assertTrue(all.contains(registered));
        assertEquals("/admin help [page|command]", registered.syntax());
    }

    @Test
    void explicitlyRegisteredHelpIsKeptAndNotDuplicated() {
        SubCommand info = new SubCommandBuilder("admin", "info").executes((s, a) -> true).build();
        ArgumentTest.SingleSubManager manager = new ArgumentTest.SingleSubManager(info);
        HelpCommand explicit = new HelpCommand("admin");
        manager.registerSubCommand(explicit);

        manager.getAllSubCommands();
        manager.getAllSubCommands();

        assertEquals(1, manager.getSubCommands().stream().filter(sub -> sub.name().equals("help")).count());
        assertSame(explicit, manager.getSubCommand("help"));
    }
}
