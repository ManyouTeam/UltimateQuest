package cn.superiormc.ultimatequest.commands;

import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubMilestone extends AbstractCommand {

    public SubMilestone() {
        id = "milestone";
        requiredPermission = "";
        onlyInGame = false;
        requiredArgLength = new Integer[] {1, 2, 3, 4, 5, 6, 7};
        requiredConsoleArgLength = new Integer[] {2, 3, 4, 5, 6, 7};
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        SubBattlePass.executeProgression(player, args, "milestone");
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        SubBattlePass.executeProgression(Bukkit.getConsoleSender(), args, "milestone");
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        return SubBattlePass.suggestions(args, player, "milestone");
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        return SubBattlePass.suggestions(args, Bukkit.getConsoleSender(), "milestone");
    }
}
