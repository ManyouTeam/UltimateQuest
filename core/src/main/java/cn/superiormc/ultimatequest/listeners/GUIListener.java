package cn.superiormc.ultimatequest.listeners;

import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;

public final class GUIListener implements Listener {

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof InvGUI gui)) {
            return;
        }
        event.setCancelled(true);
        if (event.getClickedInventory() != null && event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            event.setCancelled(!gui.allowsPlayerInventoryClick(event.getClick()));
            return;
        }
        if (event.getClickedInventory() == null || event.getRawSlot() < 0 || event.getRawSlot() >= event.getView().getTopInventory().getSize()) {
            return;
        }
        try {
            gui.clickEventHandle(event.getClickedInventory(), event.getClick(), event.getSlot());
        } catch (RuntimeException exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not handle quest menu: " + exception.getMessage());
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getView().getTopInventory().getHolder() instanceof InvGUI) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onClose(InventoryCloseEvent event) {
        if (event.getInventory().getHolder() instanceof InvGUI gui) {
            gui.closeEventHandle(event.getInventory());
        }
    }

    @EventHandler
    public void onSwap(PlayerSwapHandItemsEvent event) {
        Player player = event.getPlayer();
        if (player.getOpenInventory().getTopInventory().getHolder() instanceof InvGUI) {
            event.setCancelled(true);
        }
    }
}
