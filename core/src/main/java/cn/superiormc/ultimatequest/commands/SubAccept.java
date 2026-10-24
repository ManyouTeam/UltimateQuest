package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubAccept extends AbstractCommand {

    public SubAccept() {
        id = "accept";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 2, 3, 4 };
        requiredConsoleArgLength = new Integer[] { 3, 4 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        if (args.length == 2) {
            QuestManager.questManager.accept(player, args[1]);
            return;
        }
        if (!player.hasPermission("ultimatequest.admin")) {
            LanguageManager.languageManager.sendStringText(player, "error.miss-permission");
            return;
        }
        if (args.length != 3 && (args.length != 4 || !args[3].equalsIgnoreCase("-b"))) {
            LanguageManager.languageManager.sendStringText(player, "command.console-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText(player, "error.player-not-found", "player", args[2]);
            return;
        }
        SchedulerUtil.runSync(target, () -> QuestManager.questManager.accept(target, args[1], true, args.length == 4));
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length != 3 && (args.length != 4 || !args[3].equalsIgnoreCase("-b"))) {
            LanguageManager.languageManager.sendStringText("command.console-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText("error.player-not-found", "player", args[2]);
            return;
        }
        SchedulerUtil.runSync(target, () -> QuestManager.questManager.accept(target, args[1], true, args.length == 4));
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        if (args.length == 2) {
            return ConfigManager.configManager.getListedDefinitions().stream().map(definition -> definition.id()).toList();
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return args.length == 4 ? List.of("-b") : List.of();
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        if (args.length > 2) {
            return player.hasPermission("ultimatequest.admin") ? getConsoleTabResult(args) : List.of();
        }
        List<String> ids = new ArrayList<>();
        ConfigManager.configManager.getListedDefinitions().forEach(definition -> ids.add(definition.id()));
        return ids;
    }

}
