package de.happybavarian07.coolstufflib.commandmanagement;

import de.happybavarian07.coolstufflib.jpa.SQLExecutor;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * <p>Stores aliases in their own table ({@code <prefix>command_aliases}) through the JPA layer's
 * {@link SQLExecutor}; works with SQLite and MySQL/MariaDB.</p>
 */
public final class SqlAliasStore implements AliasStore {
    private final SQLExecutor executor;
    private final String table;

    public SqlAliasStore(SQLExecutor executor, String tablePrefix) {
        String prefix = tablePrefix == null ? "" : tablePrefix;
        if (!prefix.matches("[A-Za-z0-9_]*")) throw new IllegalArgumentException("Invalid table prefix: " + prefix);
        this.executor = executor;
        this.table = prefix + "command_aliases";
        run(() -> executor.executeUpdate("CREATE TABLE IF NOT EXISTS " + table + " ("
                + "name VARCHAR(64) NOT NULL PRIMARY KEY, "
                + "command TEXT NOT NULL, "
                + "permission VARCHAR(255), "
                + "console BOOLEAN NOT NULL DEFAULT FALSE, "
                + "description TEXT, "
                + "aliases TEXT, "
                + "player_only BOOLEAN NOT NULL DEFAULT FALSE, "
                + "cooldown_seconds BIGINT NOT NULL DEFAULT 0, "
                + "confirm_required BOOLEAN NOT NULL DEFAULT FALSE)"));
    }

    public String table() {
        return table;
    }

    @Override
    public List<CommandAlias> loadAll() {
        return run(() -> executor.query("SELECT name, command, permission, console, description, aliases, player_only, "
                + "cooldown_seconds, confirm_required FROM " + table, rs -> {
            List<CommandAlias> aliases = new ArrayList<>();
            while (rs.next()) {
                String extra = rs.getString("aliases");
                aliases.add(new CommandAlias(rs.getString("name"), rs.getString("command"), rs.getString("permission"),
                        rs.getBoolean("console"), rs.getString("description"),
                        extra == null || extra.isBlank() ? List.of() : Arrays.asList(extra.split(",")),
                        rs.getBoolean("player_only"), rs.getLong("cooldown_seconds"), rs.getBoolean("confirm_required")));
            }
            return aliases;
        }));
    }

    @Override
    public void save(CommandAlias alias) {
        run(() -> {
            executor.executeUpdate("DELETE FROM " + table + " WHERE name = ?", alias.name());
            return executor.executeUpdate("INSERT INTO " + table + " (name, command, permission, console, description, aliases, "
                            + "player_only, cooldown_seconds, confirm_required) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)",
                    alias.name(), alias.command(), alias.permission(), alias.console(), alias.description(),
                    String.join(",", alias.aliases()), alias.playerOnly(), alias.cooldownSeconds(), alias.confirm());
        });
    }

    @Override
    public void delete(String name) {
        run(() -> executor.executeUpdate("DELETE FROM " + table + " WHERE name = ?", name));
    }

    private static <T> T run(SqlCall<T> call) {
        try {
            return call.run();
        } catch (SQLException e) {
            throw new IllegalStateException("Command alias storage failed", e);
        }
    }

    @FunctionalInterface
    private interface SqlCall<T> {
        T run() throws SQLException;
    }
}
