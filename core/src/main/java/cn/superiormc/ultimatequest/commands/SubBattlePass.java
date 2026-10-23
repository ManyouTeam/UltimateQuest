package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.editor.EditorScope;
import cn.superiormc.ultimatequest.managers.MenuStatusManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class SubBattlePass extends AbstractCommand {

    public SubBattlePass() {
        id = "battlepass";
        requiredPermission = "";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1, 2, 3, 4, 5, 6, 7 };
        requiredConsoleArgLength = new Integer[] { 2, 3, 4, 5, 6, 7 };
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        execute(player, args);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        execute(Bukkit.getConsoleSender(), args);
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        return suggestions(args, player, "battlepass");
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        return suggestions(args, Bukkit.getConsoleSender(), "battlepass");
    }

    public static List<String> suggestions(String[] args, CommandSender sender, String type) {
        List<String> result = new ArrayList<>();
        if (args.length == 2) {
            if (sender.hasPermission("ultimatequest.use") || sender.hasPermission("ultimatequest.admin")) {
                result.add("claim");
                result.addAll(List.of("claimall", "history", "challenges", "stats"));
                QuestManager.questManager.growthDefinitions(type).forEach(definition -> result.add(definition.id()));
            }
            if (sender.hasPermission("ultimatequest.admin")) {
                result.addAll(List.of("addxp", "setxp", "removexp", "setlevel", "premium", "boost", "resolve", "season", "editor"));
                if (!(sender instanceof Player)) {
                    Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                }
            }
        } else if (args[1].equalsIgnoreCase("addxp") && sender.hasPermission("ultimatequest.admin")) {
            if (args.length == 3) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            } else if (args.length == 4) {
                QuestManager.questManager.growthDefinitions(type).forEach(definition -> result.add(definition.id()));
            } else if (args.length == 5) {
                result.add("100");
            }
        } else if (args[1].equalsIgnoreCase("claim") && (sender.hasPermission("ultimatequest.use") || sender.hasPermission("ultimatequest.admin"))) {
            if (args.length == 3) {
                QuestManager.questManager.growthDefinitions(type).forEach(definition -> result.add(definition.id()));
            } else if (args.length == 4) {
                QuestDefinition definition = QuestManager.questManager.growthDefinition(type, args[2]);
                if (definition != null) {
                    definition.thresholds().keySet().forEach(level -> result.add(String.valueOf(level)));
                }
            } else if (args.length == 5) {
                result.addAll(List.of("free", "premium"));
                if (!(sender instanceof Player)) {
                    Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                }
            } else if (args.length == 6 && !(sender instanceof Player)) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            }
        } else if (Set.of("claimall", "history", "challenges", "stats").contains(args[1].toLowerCase(Locale.ROOT))) {
            if (args.length == 3) {
                QuestManager.questManager.growthDefinitions(type).forEach(definition -> result.add(definition.id()));
            } else if (args.length == 4 && !(sender instanceof Player) && sender.hasPermission("ultimatequest.admin")) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            } else if (args.length == 5 && args[1].equalsIgnoreCase("challenges") && !(sender instanceof Player)) {
                result.add("-b");
            }
        } else if (!(sender instanceof Player) && sender.hasPermission("ultimatequest.admin")) {
            if (args.length == 3) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                result.add("-b");
            } else if (args.length == 4) {
                result.add("-b");
            }
        }
        return result;
    }

    private void execute(CommandSender sender, String[] args) {
        executeProgression(sender, args, "battlepass");
    }

    public static void executeProgression(CommandSender sender, String[] args, String type) {
        String operation = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        QuestManager manager = QuestManager.questManager;
        if (operation.equals("editor")) {
            if (!(sender instanceof Player player) || !sender.hasPermission("ultimatequest.admin")) {
                LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
                return;
            }
            if (args.length != 3 || manager.growthDefinition(type, args[2]) == null) {
                LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-usage");
                return;
            }
            MenuStatusManager.menuStatusManager.openFile(player, EditorScope.ofName(type), args[2]);
            return;
        }
        if (operation.equals("season")) {
            if (!sender.hasPermission("ultimatequest.admin")) {
                LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
                return;
            }
            if (args.length < 4) {
                LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-usage");
                return;
            }
            String[] target = args[3].toLowerCase(Locale.ROOT).split(":", 2);
            String targetType = target.length == 2 ? target[0] : type;
            String targetId = target.length == 2 ? target[1] : target[0];
            manager.editor().editSeason(sender, targetType, targetId, args[2].toLowerCase(Locale.ROOT),
                    Arrays.asList(args).subList(4, args.length));
            return;
        }
        if (Set.of("addxp", "setxp", "removexp", "setlevel", "premium", "boost", "resolve").contains(operation)) {
            if (!sender.hasPermission("ultimatequest.admin")) {
                LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
                return;
            }
            int length = Set.of("boost", "resolve").contains(operation) ? 6 : 5;
            if (args.length != length) {
                LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-usage");
                return;
            }
            UUID target = resolvePlayer(args[2]);
            if (target == null) {
                LanguageManager.languageManager.sendStringText(sender, "error.player-not-found", "player", args[2]);
                return;
            }
            Player online = Bukkit.getPlayer(target);
            Runnable edit = () -> manager.manageProgression(sender, target, type, args[3].toLowerCase(Locale.ROOT), operation,
                    Arrays.asList(args).subList(4, args.length));
            if (online == null) {
                edit.run();
            } else {
                SchedulerUtil.runSync(online, edit);
            }
            return;
        }
        if (sender instanceof Player player) {
            if (!player.hasPermission("ultimatequest.use")) {
                LanguageManager.languageManager.sendStringText(player, "error.miss-permission");
                return;
            }
            executePlayerProgression(player, args, type, false);
            return;
        }
        boolean bypass = args[args.length - 1].equalsIgnoreCase("-b");
        int targetIndex = args.length - (bypass ? 2 : 1);
        boolean valid = targetIndex == 1 || targetIndex == 2
                || targetIndex == 3 && Set.of("claimall", "history", "challenges", "stats").contains(operation)
                || (targetIndex == 4 || targetIndex == 5) && operation.equals("claim");
        if (!valid || bypass && !(targetIndex <= 2 || operation.equals("challenges"))) {
            LanguageManager.languageManager.sendStringText(sender, "command.console-usage");
            return;
        }
        String[] forwarded = Arrays.copyOf(args, targetIndex);
        if (!sender.hasPermission("ultimatequest.admin")) {
            LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[targetIndex]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText(sender, "error.player-not-found", "player", args[targetIndex]);
            return;
        }
        SchedulerUtil.runSync(target, () -> executePlayerProgression(target, forwarded, type, bypass));
    }

    private static void executePlayerProgression(Player player, String[] args, String type, boolean bypass) {
        String operation = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        QuestManager manager = QuestManager.questManager;
        if (!QuestManager.questManager.ready(player.getUniqueId())) {
            QuestManager.questManager.join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return;
        }
        if (Set.of("claimall", "history", "challenges", "stats").contains(operation)) {
            if (args.length != 3) {
                LanguageManager.languageManager.sendStringText(player, "battlepass.player-usage");
                return;
            }
            String pass = args[2].toLowerCase(Locale.ROOT);
            switch (operation) {
                case "claimall" -> {
                    manager.claimAll(player, type, pass);
                }
                case "history" -> {
                    LanguageManager.languageManager.sendStringText(player, "battlepass.history", "seasons",
                            String.join(", ", manager.seasons(player.getUniqueId(), type, pass)));
                }
                case "challenges" -> {
                    if (type.equals("battlepass")) {
                        new ConfiguredMenuGUI(player, "battlepass/" + pass + "/challenges").openGUI(bypass);
                    }
                }
                case "stats" -> {
                    var quest = manager.growthInstance(player.getUniqueId(), type, pass);
                    if (quest != null) {
                        LanguageManager.languageManager.sendStringText(player, "battlepass.stats", "name", quest.definition().name(player),
                                "season", quest.definition().season(), "level", String.valueOf(quest.level()),
                                "total", String.valueOf(quest.total()), "premium", String.valueOf(quest.premium(player)));
                    }
                }
                default -> {
                }
            }
            return;
        }
        if (args.length <= 2) {
            if (args.length == 2 && QuestManager.questManager.growthDefinition(type, args[1]) == null
                    && manager.growthInstance(player.getUniqueId(), type, args[1]) == null) {
                LanguageManager.languageManager.sendStringText(player, "quests.not-found");
                return;
            }
            new ConfiguredMenuGUI(player, args.length == 1 ? type : type + "/" + args[1].toLowerCase(Locale.ROOT)).openGUI(bypass);
            return;
        }
        if (args[1].equalsIgnoreCase("claim") && (args.length == 4 || args.length == 5)) {
            try {
                int level = Integer.parseInt(args[3]);
                String track = args.length == 5 ? args[4].toLowerCase(Locale.ROOT)
                        : type.equals("milestone") ? "default" : "free";
                QuestManager.questManager.claimStage(player, type, args[2], level, track);
            } catch (NumberFormatException exception) {
                LanguageManager.languageManager.sendStringText(player, "progression.usage");
            }
            return;
        }
        LanguageManager.languageManager.sendStringText(player, "progression.usage");
    }

    private static UUID resolvePlayer(String name) {
        try {
            return UUID.fromString(name);
        } catch (IllegalArgumentException exception) {
            for (OfflinePlayer player : Bukkit.getOfflinePlayers()) {
                if (name.equalsIgnoreCase(player.getName())) {
                    return player.getUniqueId();
                }
            }
            return null;
        }
    }


}
