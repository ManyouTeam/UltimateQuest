package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubPeriod extends AbstractCommand {

    public SubPeriod() {
        id = "period";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1 };
        requiredConsoleArgLength = new Integer[] { 2, 3 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        SubMenu.open(player, "period");
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length != 2 && (args.length != 3 || !args[2].equalsIgnoreCase("-b"))) {
            LanguageManager.languageManager.sendStringText("command.console-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText("error.player-not-found", "player", args[1]);
            return;
        }
        SchedulerUtil.runSync(target, () -> SubMenu.open(target, "period", args.length == 3));
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        if (args.length == 2) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return args.length == 3 ? List.of("-b") : List.of();
    }

}
