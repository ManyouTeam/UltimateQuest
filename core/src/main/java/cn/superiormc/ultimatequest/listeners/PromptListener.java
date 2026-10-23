package cn.superiormc.ultimatequest.listeners;

import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public final class PromptListener implements Listener {

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        MenuStatusManager manager = MenuStatusManager.menuStatusManager;
        if (!manager.hasPrompt(player)) {
            return;
        }
        event.setCancelled(true);
        String message = event.getMessage();
        SchedulerUtil.runSync(player, () -> {
            if (player.isOnline()) {
                manager.submitPrompt(player, message);
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        MenuStatusManager.menuStatusManager.clear(event.getPlayer());
    }
}
