package de.happybavarian07.coolstufflib.commandmanagement;

import java.util.List;

/**
 * <p>Persistence for command aliases created at runtime. Implementations keep aliases in the data
 * system: {@link YamlAliasStore} (its own section in {@code data.yml}) or {@link SqlAliasStore}
 * (its own table). Aliases are never written into {@code config.yml}.</p>
 */
public interface AliasStore {
    List<CommandAlias> loadAll();

    void save(CommandAlias alias);

    void delete(String name);
}
