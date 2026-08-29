package dev.minhhaudev.checktimedeploy.listener;

import dev.minhhaudev.checktimedeploy.CheckTimeDeploy;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {

    private final CheckTimeDeploy plugin;

    public PlayerListener(CheckTimeDeploy plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onJoin(PlayerJoinEvent e) {
        // Ghi nhận ngay lập tức nhưng async để không lag main thread
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().handleJoin(e.getPlayer());
        });
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onQuit(PlayerQuitEvent e) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().handleQuit(e.getPlayer());
        });
    }
}
