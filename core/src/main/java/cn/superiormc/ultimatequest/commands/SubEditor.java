package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.editor.EditorLang;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import cn.superiormc.ultimatequest.editor.EditorScope;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SubEditor extends AbstractCommand {

    public SubEditor() {
        this.id = "editor";
        this.requiredPermission = "ultimatequest.admin";
        this.onlyInGame = true;
        this.requiredArgLength = new Integer[]{1, 2, 3};
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        if (args.length == 1) {
            MenuStatusManager.menuStatusManager.openRoot(player);
            return;
        }

        EditorScope scope = EditorScope.ofName(args[1]);
        if (scope == null) {
            EditorLang.send(player, "editor.message.command-usage",
                    "&cUse /uq editor [quest|task|category|battlepass|milestone|period|scroll|pool|reward-pool|menu] [id]");
            return;
        }

        if (args.length == 2) {
            MenuStatusManager.menuStatusManager.openScope(player, scope, 0);
            return;
        }
        MenuStatusManager.menuStatusManager.openFile(player, scope, args[2]);
    }

    @Override
    public List<String> getTabResult(String[] args, Player player) {
        List<String> result = new ArrayList<>();
        if (args.length == 2) {
            for (EditorScope scope : EditorScope.values()) {
                result.add(scope.name().toLowerCase(Locale.ROOT).replace('_', '-'));
            }
            return result;
        }
        if (args.length == 3) {
            EditorScope scope = EditorScope.ofName(args[1]);
            if (scope != null) {
                result.addAll(scope.listIds());
            }
        }
        return result;
    }
}
