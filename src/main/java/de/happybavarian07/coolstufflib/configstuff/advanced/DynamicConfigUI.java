package de.happybavarian07.coolstufflib.configstuff.advanced;

import org.bukkit.entity.Player;
import de.happybavarian07.coolstufflib.configstuff.advanced.interfaces.ConfigSection;
import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.menusystem.PlayerMenuUtility;

/**
 * <p>Next-Gen feature: Automatically generates in-game GUI menus for editing AdvancedConfig structures.</p>
 */
public final class DynamicConfigUI {

    private DynamicConfigUI() {}

    /**
     * <p>Opens an auto-generated configuration menu for the specified player.</p>
     * <pre><code>DynamicConfigUI.openFor(player, myConfigSection);</code></pre>
     *
     * @param player  the admin player editing the config
     * @param section the configuration section to display and edit
     */
    public static void openFor(Player player, ConfigSection section) {
        if (player == null || section == null) return;
        CoolStuffLib lib = CoolStuffLib.getLib();
        PlayerMenuUtility utility = lib.getPlayerMenuUtility(player.getUniqueId());
        
        new DynamicConfigMenu(utility, null, section, "Root").open();
    }
}
