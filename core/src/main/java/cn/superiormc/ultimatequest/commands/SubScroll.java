package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestScrollManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubScroll extends AbstractCommand {

    public SubScroll() {
        id = "scroll";
        requiredPermission = "";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1, 4, 5, 6 };
        requiredConsoleArgLength = new Integer[] { 2, 3, 4, 5, 6 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        if (args.length == 1) {
            if (player.hasPermission("ultimatequest.use")) {
                SubMenu.open(player, "scroll");
            } else {
                LanguageManager.languageManager.sendStringText(player, "error.miss-permission");
            }
        } else if (args[1].equalsIgnoreCase("give")) {
            giveScroll(player, args);
        } else {
            LanguageManager.languageManager.sendStringText(player, "scroll.usage");
        }
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length >= 4 && args[1].equalsIgnoreCase("give")) {
            giveScroll(Bukkit.getConsoleSender(), args);
            return;
        }
        if ((args.length != 2 && (args.length != 3 || !args[2].equalsIgnoreCase("-b"))) || args[1].equalsIgnoreCase("give")) {
            LanguageManager.languageManager.sendStringText("scroll.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[1]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText("error.player-not-found", "player", args[1]);
            return;
        }
        SchedulerUtil.runSync(target, () -> SubMenu.open(target, "scroll", args.length == 3));
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        return player.hasPermission("ultimatequest.admin") ? suggestions(args) : List.of();
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        List<String> result = suggestions(args);
        if (args.length == 2) {
            Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
        } else if (args.length == 3 && !args[1].equalsIgnoreCase("give")) {
            result.add("-b");
        }
        return result;
    }

    private List<String> suggestions(String[] args) {
        List<String> result = new ArrayList<>();
        if (args.length == 2) {
            result.add("give");
        } else if (args[1].equalsIgnoreCase("give")) {
            if (args.length == 3) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            } else if (args.length == 4) {
                result.addAll(QuestScrollManager.questScrollManager.definitions().keySet());
            } else if (args.length == 5 || args.length == 6) {
                var profile = QuestScrollManager.questScrollManager.definitions().get(args[3].toLowerCase(Locale.ROOT));
                if (profile != null) {
                    var choices = profile.getConfigurationSection(args.length == 5 ? "rarities" : "difficulties");
                    if (choices != null) {
                        result.addAll(choices.getKeys(false));
                    } else {
                        result.add(profile.getString(args.length == 5 ? "rarity" : "difficulty",
                                args.length == 5 ? "common" : "normal"));
                    }
                }
            }
        }
        return result;
    }

    private void giveScroll(CommandSender sender, String[] args) {
        if (!sender.hasPermission("ultimatequest.admin")) {
            LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
            return;
        }
        if (args.length < 4 || args.length > 6) {
            LanguageManager.languageManager.sendStringText(sender, "scroll.usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText(sender, "error.player-not-found", "player", args[2]);
            return;
        }
        String profile = args[3].toLowerCase(Locale.ROOT);
        String rarity = args.length >= 5 ? args[4].toLowerCase(Locale.ROOT) : null;
        String difficulty = args.length == 6 ? args[5].toLowerCase(Locale.ROOT) : null;
        SchedulerUtil.runSync(target, () -> {
            try {
                QuestScrollManager.questScrollManager.give(target, profile, rarity, difficulty);
            } catch (RuntimeException exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not issue scroll: " + exception.getMessage());
                LanguageManager.languageManager.sendStringText(sender, "scroll.usage");
            }
        });
    }


}
