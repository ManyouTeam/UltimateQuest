package cn.superiormc.ultimatequest.gui.inv.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorScope;
import cn.superiormc.ultimatequest.editor.EditorUtil;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

public final class EditorRootGUI extends InvGUI {

    public EditorRootGUI(Player owner) {
        super(owner);
    }

    @Override
    public void constructGUI() {
        title = EditorLang.text(player, "editor.root.title", "UltimateQuest Editor");
        inv = UltimateQuest.methodUtil.createNewInv(player, 27, title, this);
        EditorScope[] scopes = EditorScope.values();
        for (int index = 0; index < scopes.length; index++) {
            EditorScope scope = scopes[index];
            inv.setItem(scopeSlot(index), EditorUtil.createItem(Material.BOOK, "&e" + EditorLang.scope(player, scope),
                    List.of("&7/" + scope.getFolderName(), EditorLang.text(player, "editor.action.open", "&aLeft click to open"))));
        }
        inv.setItem(22, EditorUtil.createItem(Material.BARRIER,
                EditorLang.text(player, "editor.root.close.name", "&cClose"), List.of()));
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        if (!MenuStatusManager.menuStatusManager.allowed(player)) {
            return true;
        }
        for (int index = 0; index < EditorScope.values().length; index++) {
            if (slot == scopeSlot(index)) {
                MenuStatusManager.menuStatusManager.openScope(player, EditorScope.values()[index], 0);
                return true;
            }
        }
        if (slot == 22) {
            player.closeInventory();
        }
        return true;
    }

    private int scopeSlot(int index) {
        return 10 + index / 7 * 9 + index % 7;
    }
}
