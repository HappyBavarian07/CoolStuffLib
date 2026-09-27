package de.happybavarian07.coolstufflib.languagemanager;

import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.UUID;

/** Where each player's chosen language is kept. */
public interface PlayerLanguageStore {
    @Nullable String get(UUID player);

    void set(UUID player, String language);

    void remove(UUID player);

    Map<UUID, String> all();
}
