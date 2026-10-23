package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.List;
import java.util.ArrayList;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubMenu extends AbstractCommand {

    public SubMenu() {
        id = "menu";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1, 2 };
        requiredConsoleArgLength = new Integer[] { 2, 3, 4 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        open(player, args.length == 1 ? "main" : args[1]);
    }

    public static void open(Player player, String menu) {
        open(player, menu, false);
    }

    public static void open(Player player, String menu, boolean bypass) {
        if (!QuestManager.questManager.ready(player.getUniqueId())) {
            QuestManager.questManager.join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return;
        }
        if (!ConfigManager.configManager.hasMenu(menu.split("/", 2)[0])) {
            LanguageManager.languageManager.sendStringText(player, "menu-not-found");
            return;
        }
        new ConfiguredMenuGUI(player, menu).openGUI(bypass);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        boolean bypass = args[args.length - 1].equalsIgnoreCase("-b");
        int length = args.length - (bypass ? 1 : 0);
        if (length != 2 && length != 3) {
            LanguageManager.languageManager.sendStringText("command.console-usage");
            return;
        }
        String menu = length == 2 ? "main" : args[1];
        String name = length == 2 ? args[1] : args[2];
        Player target = Bukkit.getPlayerExact(name);
        if (target == null) {
            LanguageManager.languageManager.sendStringText("error.player-not-found", "player", name);
            return;
        }
        SchedulerUtil.runSync(target, () -> open(target, menu, bypass));
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        return args.length == 2 ? List.copyOf(ConfigManager.configManager.getMenuIds()) : List.of();
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        List<String> suggestions = new ArrayList<>();
        if (args.length == 2) {
            suggestions.addAll(ConfigManager.configManager.getMenuIds());
            Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
        } else if (args.length == 3) {
            if (ConfigManager.configManager.hasMenu(args[1])) {
                Bukkit.getOnlinePlayers().forEach(player -> suggestions.add(player.getName()));
            }
            if (Bukkit.getPlayerExact(args[1]) != null) {
                suggestions.add("-b");
            }
        } else if (args.length == 4 && ConfigManager.configManager.hasMenu(args[1])
                && Bukkit.getPlayerExact(args[2]) != null) {
            suggestions.add("-b");
        }
        return suggestions;
    }

}
