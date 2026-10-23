package cn.superiormc.ultimatequest.gui.inv.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorTarget;
import cn.superiormc.ultimatequest.editor.EditorUtil;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.methods.Dupe;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class EditorItemValueGUI extends InvGUI {

    private static final int EDIT_SLOT = 13;

    private final EditorTarget target;

    private final String path;

    private ItemStack selected;

    public EditorItemValueGUI(Player owner, EditorTarget target, String path) {
        super(owner);
        this.target = target;
        this.path = path;
    }

    @Override
    public void constructGUI() {
        title = EditorLang.text(player, "editor.item.title", "Item Editor: {path}",
                "path", EditorUtil.displayPath(player, path));
        if (inv == null) {
            inv = UltimateQuest.methodUtil.createNewInv(player, 27, title, this);
        }
        inv.clear();

        ConfigurationSection section = MenuStatusManager.menuStatusManager.getSection(target, path);
        if (section != null && (inv.getItem(EDIT_SLOT) == null || inv.getItem(EDIT_SLOT).getType().isAir())) {
            try {
                ItemStack itemStack = BuildItem.buildItemStack(player, section);
                selected = itemStack.clone();
                inv.setItem(EDIT_SLOT, Dupe.markGuiDisplayItem(itemStack.clone()));
            } catch (RuntimeException exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not preview editor item: " + exception.getMessage());
                inv.setItem(EDIT_SLOT, new ItemStack(Material.AIR));
            }
        }

        inv.setItem(10, EditorUtil.createItem(Material.HOPPER,
                EditorLang.text(player, "editor.item.slot.name", "&eItemFormat Slot"),
                List.of(
                        EditorLang.text(player, "editor.item.slot.line-1", "&7Click slot 13 while holding an item on your cursor."),
                        EditorLang.text(player, "editor.item.slot.line-2", "&7The item is copied, never taken. Apply to update the draft.")
                )));
        inv.setItem(22, EditorUtil.createItem(Material.CHEST,
                EditorLang.text(player, "editor.item.raw.name", "&eRaw Section"),
                List.of(EditorLang.text(player, "editor.item.raw.desc", "&7Open all keys inside this item section"))));
        inv.setItem(23, EditorUtil.createItem(Material.EMERALD,
                EditorLang.text(player, "editor.item.apply.name", "&aApply Item"),
                List.of(EditorLang.text(player, "editor.item.apply.desc", "&7Serialize slot 13 into the current section"))));
        inv.setItem(24, EditorUtil.createItem(Material.BARRIER,
                EditorLang.text(player, "editor.item.clear.name", "&cClear Section"),
                List.of(EditorLang.text(player, "editor.item.clear.desc", "&7Delete this entire item section"))));
        inv.setItem(26, EditorUtil.createItem(Material.ARROW,
                EditorLang.text(player, "editor.common.back.name", "&eBack"),
                List.of(EditorLang.text(player, "editor.item.back.desc", "&7Return to the parent section"))));
    }

    @Override
    public boolean allowsPlayerInventoryClick(ClickType type) {
        return type == ClickType.LEFT || type == ClickType.RIGHT;
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        if (!MenuStatusManager.menuStatusManager.allowed(player)) {
            return true;
        }
        if (slot == EDIT_SLOT) {
            ItemStack cursor = player.getItemOnCursor();
            if (cursor != null && !cursor.getType().isAir()) {
                selected = cursor.clone();
                inv.setItem(EDIT_SLOT, Dupe.markGuiDisplayItem(cursor.clone()));
            }
            return true;
        }
        if (slot == 22) {
            MenuStatusManager.menuStatusManager.openTarget(player, target, path, 0);
            return true;
        }
        if (slot == 23) {
            ItemStack itemStack = selected;
            if (itemStack == null || itemStack.getType().isAir()) {
                return true;
            }
            MenuStatusManager.menuStatusManager.replaceItemSection(player, target, path, itemStack);
            MenuStatusManager.menuStatusManager.openTarget(player, target, EditorUtil.parentPath(path), 0);
            return true;
        }
        if (slot == 24) {
            MenuStatusManager.menuStatusManager.removeValue(player, target, path);
            MenuStatusManager.menuStatusManager.openTarget(player, target, EditorUtil.parentPath(path), 0);
            return true;
        }
        if (slot == 26) {
            MenuStatusManager.menuStatusManager.openTarget(player, target, EditorUtil.parentPath(path), 0);
            return true;
        }
        return true;
    }

}
