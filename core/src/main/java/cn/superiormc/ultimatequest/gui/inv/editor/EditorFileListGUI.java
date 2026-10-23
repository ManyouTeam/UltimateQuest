package cn.superiormc.ultimatequest.gui.inv.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.editor.EditorScope;
import cn.superiormc.ultimatequest.editor.EditorUtil;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;

import java.util.List;

public class EditorFileListGUI extends InvGUI {

    private final EditorScope scope;

    private final int page;

    private final List<String> ids;

    public EditorFileListGUI(Player owner, EditorScope scope, int page) {
        super(owner);
        this.scope = scope;
        this.page = page;
        this.ids = scope.listIds();
    }

    @Override
    public void constructGUI() {
        title = EditorLang.text(player, "editor.file-list.title", "Editor {scope}s",
                "scope", EditorLang.scope(player, scope));
        if (inv == null) {
            inv = UltimateQuest.methodUtil.createNewInv(player, 54, title, this);
        }
        inv.clear();

        int start = page * 45;
        for (int i = 0; i < 45; i++) {
            int index = start + i;
            if (index >= ids.size()) {
                break;
            }
            String id = ids.get(index);
            inv.setItem(i, EditorUtil.createItem(
                    Material.BOOK,
                    "&e" + id,
                    List.of(
                            EditorLang.text(player, "editor.file-list.type", "&7Type: &f{value}",
                                    "value", EditorLang.scope(player, scope)),
                            EditorLang.text(player, "editor.file-list.file", "&7File: &f{value}",
                                    "value", scope.getFolderName() + "/" + id + ".yml"),
                            EditorLang.text(player, "editor.action.open", "&aLeft click to open")
                    )
            ));
        }

        inv.setItem(45, EditorUtil.createItem(Material.ARROW,
                EditorLang.text(player, "editor.common.back.name", "&eBack"),
                List.of(EditorLang.text(player, "editor.file-list.back", "&7Return to editor home"))));
        inv.setItem(49, EditorUtil.createItem(Material.COMPASS,
                EditorLang.text(player, "editor.common.refresh.name", "&eRefresh"),
                List.of(EditorLang.text(player, "editor.file-list.refresh", "&7Reload the file list"))));
        inv.setItem(52, EditorUtil.createItem(Material.SPECTRAL_ARROW,
                EditorLang.text(player, "editor.common.previous-page.name", "&ePrevious Page"),
                List.of(EditorLang.text(player, "editor.common.previous-page.desc", "&7Go to the previous page"))));
        inv.setItem(53, EditorUtil.createItem(Material.TIPPED_ARROW,
                EditorLang.text(player, "editor.common.next-page.name", "&eNext Page"),
                List.of(EditorLang.text(player, "editor.common.next-page.desc", "&7Go to the next page"))));
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        if (!MenuStatusManager.menuStatusManager.allowed(player)) {
            return true;
        }
        if (slot >= 0 && slot < 45) {
            int index = page * 45 + slot;
            if (index < ids.size()) {
                MenuStatusManager.menuStatusManager.openFile(player, scope, ids.get(index));
            }
            return true;
        }
        if (slot == 45) {
            MenuStatusManager.menuStatusManager.openRoot(player);
            return true;
        }
        if (slot == 49) {
            MenuStatusManager.menuStatusManager.openScope(player, scope, page);
            return true;
        }
        if (slot == 52 && page > 0) {
            MenuStatusManager.menuStatusManager.openScope(player, scope, page - 1);
            return true;
        }
        if (slot == 53 && (page + 1) * 45 < ids.size()) {
            MenuStatusManager.menuStatusManager.openScope(player, scope, page + 1);
            return true;
        }
        return true;
    }
}
