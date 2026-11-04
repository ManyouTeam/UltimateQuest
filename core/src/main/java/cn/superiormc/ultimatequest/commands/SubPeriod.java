package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubPeriod extends AbstractCommand {

    public SubPeriod() {
        id = "period";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1, 4 };
        requiredConsoleArgLength = new Integer[] { 2, 3, 4 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        if (args.length == 4) {
            reset(args, player);
            return;
        }
        SubMenu.open(player, "period");
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length == 4) {
            reset(args, Bukkit.getConsoleSender());
            return;
        }
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

    private void reset(String[] args, CommandSender sender) {
        if (!sender.hasPermission("ultimatequest.admin")) {
            LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
            return;
        }
        if (!args[1].equalsIgnoreCase("reset")) {
            LanguageManager.languageManager.sendStringText(sender, "period.reset-usage");
            return;
        }
        String group = args[3].toLowerCase(Locale.ROOT);
        if (!group.equals("all") && ConfigManager.configManager.getPeriod(group) == null) {
            LanguageManager.languageManager.sendStringText(sender, "period.not-found", "period", args[3]);
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText(sender, "error.player-not-found", "player", args[2]);
            return;
        }
        SchedulerUtil.runSync(target, () -> QuestManager.questManager.resetPeriods(sender, target, group));
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        return player.hasPermission("ultimatequest.admin") ? resetSuggestions(args) : List.of();
    }

    private List<String> resetSuggestions(String[] args) {
        if (args.length == 2) {
            return List.of("reset");
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("reset")) {
            if (args.length == 3) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            }
            if (args.length == 4) {
                List<String> result = new ArrayList<>(ConfigManager.configManager.getPeriodConfigurations().keySet());
                result.add("all");
                return result;
            }
        }
        return List.of();
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        if (args.length == 2) {
            List<String> result = new ArrayList<>(Bukkit.getOnlinePlayers().stream().map(Player::getName).toList());
            result.add("reset");
            return result;
        }
        if (args.length >= 3 && args[1].equalsIgnoreCase("reset")) {
            return resetSuggestions(args);
        }
        return args.length == 3 ? List.of("-b") : List.of();
    }
}
