package de.happybavarian07.coolstufflib.service.impl;

import de.happybavarian07.coolstufflib.CoolStuffLib;
import de.happybavarian07.coolstufflib.service.api.Service;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * <p>Service that manages capturing chat input from players safely without memory leaks.</p>
 */
public class ChatInputService implements Service, Listener {
    private final UUID serviceId = UUID.randomUUID();
    private final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();

    @Override
    public UUID id() {
        return serviceId;
    }

    @Override
    public String serviceName() {
        return "chat-input-service";
    }

    @Override
    public CompletableFuture<Void> init() {
        return CompletableFuture.runAsync(() -> {
            Bukkit.getPluginManager().registerEvents(this, CoolStuffLib.getLib().getJavaPluginUsingLib());
        });
    }

    @Override
    public CompletableFuture<Void> shutdown() {
        return CompletableFuture.runAsync(pendingInputs::clear);
    }

    /**
     * <p>Requests the next chat message from a player and passes it to the callback.</p>
     *
     * @param player   the player to request input from
     * @param callback the consumer to process the input
     */
    public void requestInput(Player player, Consumer<String> callback) {
        pendingInputs.put(player.getUniqueId(), callback);
    }

    /**
     * <p>Cancels any pending chat input request for a player.</p>
     *
     * @param player the player
     */
    public void cancelInput(Player player) {
        pendingInputs.remove(player.getUniqueId());
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        Consumer<String> callback = pendingInputs.remove(player.getUniqueId());

        if (callback != null) {
            event.setCancelled(true);
            Bukkit.getScheduler().runTask(CoolStuffLib.getLib().getJavaPluginUsingLib(), () -> callback.accept(event.getMessage()));
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        pendingInputs.remove(event.getPlayer().getUniqueId());
    }
}
