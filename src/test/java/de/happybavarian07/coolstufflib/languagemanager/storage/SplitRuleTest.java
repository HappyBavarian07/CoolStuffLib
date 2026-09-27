package de.happybavarian07.coolstufflib.languagemanager.storage;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SplitRuleTest {
    @ParameterizedTest
    @CsvSource({
            "LanguageFullName, language.yml, LanguageFullName",
            "Messages.Player.General.NoPermissions, messages/Player.yml, General.NoPermissions",
            "Messages.ToManyArguments, messages/_root.yml, ToManyArguments",
            "Items.StartMenu.HintItem.displayName, items/StartMenu.yml, HintItem.displayName",
            "MenuTitles.PlayerManager.Selector, titles.yml, PlayerManager.Selector",
            "CustomVariables.x, other.yml, CustomVariables.x",
            "Messages.Bad Name.X, other.yml, Messages.Bad Name.X"
    })
    void mapsKeysToFilesAndBack(String key, String file, String relative) {
        SplitRule.Target target = SplitRule.of(key);
        assertEquals(file, target.file());
        assertEquals(relative, target.relativeKey());
        assertEquals(key, SplitRule.fullKey(target.file(), target.relativeKey()));
    }
}
