package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.managers.SeasonManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
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
        if (args.length < 2) {
            return result;
        }
        boolean admin = sender.hasPermission("ultimatequest.admin");
        boolean console = !(sender instanceof Player);
        if (console ? !admin : !admin && !sender.hasPermission("ultimatequest.use")) {
            return result;
        }
        QuestManager manager = QuestManager.questManager;
        String operation = args[1].toLowerCase(Locale.ROOT);
        List<String> playerOperations = new ArrayList<>(List.of("claim", "claimall", "history", "stats"));
        if (type.equals("battlepass")) {
            playerOperations.add("challenges");
        }
        Set<String> adminOperations = Set.of("addxp", "setxp", "removexp", "setlevel", "premium", "boost", "resolve");
        if (args.length == 2) {
            result.addAll(playerOperations);
            for (QuestDefinition definition : ConfigManager.configManager.getGrowthDefinitions(type)) {
                result.add(definition.id());
                if (sender instanceof Player player && !SeasonManager.seasonManager.season(definition).isEmpty() && args[1].contains("@")) {
                    result.addAll(CacheManager.cacheManager.seasons(player.getUniqueId(), type, definition.id()));
                }
            }
            if (admin) {
                result.addAll(List.of("addxp", "setxp", "removexp", "setlevel", "premium", "boost", "resolve", "season"));
                if (console) {
                    Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                }
            }
        } else if (adminOperations.contains(operation)) {
            if (!admin) {
                return result;
            }
            Player target = Bukkit.getPlayerExact(args[2]);
            if (args.length == 3) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            } else if (args.length == 4) {
                for (QuestDefinition definition : ConfigManager.configManager.getGrowthDefinitions(type)) {
                    result.add(definition.id());
                    if (target != null && !SeasonManager.seasonManager.season(definition).isEmpty() && args[3].contains("@")) {
                        result.addAll(CacheManager.cacheManager.seasons(target.getUniqueId(), type, definition.id()));
                    }
                }
            } else {
                QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, args[3].split("@", 2)[0]);
                if (definition == null) {
                    return result;
                }
                if (args.length == 5) {
                    switch (operation) {
                        case "premium" -> {
                            result.addAll(List.of("true", "false"));
                        }
                        case "setlevel" -> {
                            result.add("0");
                            definition.thresholds().keySet().forEach(level -> result.add(String.valueOf(level)));
                        }
                        case "boost" -> {
                            result.addAll(List.of("1.5", "2.0"));
                        }
                        case "resolve" -> {
                            var quest = target == null ? null : manager.growthInstance(target.getUniqueId(), type, args[3]);
                            if (quest != null) {
                                quest.stageStates().forEach((stage, state) -> {
                                    if (Set.of("CLAIMING", "REVIEW").contains(state)) {
                                        result.add(stage);
                                    }
                                });
                            } else {
                                for (String track : definition.tracks()) {
                                    definition.thresholds().keySet().forEach(level -> result.add(track + "/" + level));
                                }
                            }
                        }
                        default -> {
                            result.addAll(operation.equals("setxp") ? List.of("0", "100", "1000") : List.of("100", "1000"));
                        }
                    }
                } else if (args.length == 6 && operation.equals("boost")) {
                    result.addAll(List.of("3600", "86400"));
                } else if (args.length == 6 && operation.equals("resolve")) {
                    result.addAll(List.of("AVAILABLE", "CLAIMED"));
                }
            }
        } else if (operation.equals("season")) {
            if (!admin) {
                return result;
            }
            Set<String> seasonOperations = Set.of("pause", "resume", "end", "start", "schedule", "globalboost");
            String action = args[2].toLowerCase(Locale.ROOT);
            if (args.length == 3) {
                result.addAll(List.of("pause", "resume", "end", "start", "schedule", "globalboost"));
            } else if (!seasonOperations.contains(action)) {
                return result;
            } else if (args.length == 4) {
                ConfigManager.configManager.getGrowthDefinitions(type).forEach(definition -> result.add(definition.id()));
                for (String targetType : List.of("battlepass", "milestone")) {
                    ConfigManager.configManager.getGrowthDefinitions(targetType).forEach(definition -> result.add(targetType + ":" + definition.id()));
                }
                ConfigManager.configManager.getDefinitions().values().stream().filter(definition -> !definition.growth()
                        && !definition.isBattlePassTask())
                        .forEach(definition -> result.add("normal:" + definition.id()));
                ConfigManager.configManager.getPeriodConfigurations().keySet().forEach(id -> result.add("period:" + id));
            } else if (args.length == 5) {
                if (Set.of("start", "schedule").contains(action)) {
                    result.add("season_" + Instant.now().getEpochSecond());
                } else if (action.equals("globalboost")) {
                    result.addAll(List.of("1.5", "2.0"));
                }
            } else if (args.length == 6) {
                if (action.equals("start")) {
                    result.addAll(List.of("2592000", "604800", "86400"));
                } else if (action.equals("globalboost")) {
                    result.addAll(List.of("3600", "86400"));
                } else if (action.equals("schedule")) {
                    result.add(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(1L, ChronoUnit.DAYS).toString());
                }
            } else if (args.length == 7 && action.equals("schedule")) {
                try {
                    result.add(Instant.parse(args[5]).plus(30L, ChronoUnit.DAYS).toString());
                } catch (DateTimeParseException exception) {
                    result.add(Instant.now().truncatedTo(ChronoUnit.SECONDS).plus(31L, ChronoUnit.DAYS).toString());
                }
            }
        } else if (playerOperations.contains(operation)) {
            if (args.length == 3) {
                for (QuestDefinition definition : ConfigManager.configManager.getGrowthDefinitions(type)) {
                    result.add(definition.id());
                    if (sender instanceof Player player && !SeasonManager.seasonManager.season(definition).isEmpty()
                            && !Set.of("history", "challenges").contains(operation) && args[2].contains("@")) {
                        result.addAll(CacheManager.cacheManager.seasons(player.getUniqueId(), type, definition.id()));
                    }
                }
            } else if (operation.equals("claim")) {
                QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, args[2].split("@", 2)[0]);
                if (definition == null) {
                    return result;
                }
                if (args.length == 4) {
                    definition.thresholds().keySet().forEach(level -> result.add(String.valueOf(level)));
                } else if (args.length == 5) {
                    result.addAll(definition.tracks());
                    if (console) {
                        Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                    }
                } else if (args.length == 6 && console && definition.tracks().contains(args[4].toLowerCase(Locale.ROOT))) {
                    Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                }
            } else if (args.length == 4 && console) {
                Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
            } else if (args.length == 5 && operation.equals("challenges") && console) {
                result.add("-b");
            }
        } else if (console && admin) {
            if (args.length == 3) {
                if (ConfigManager.configManager.getGrowthDefinition(type, operation.split("@", 2)[0]) != null) {
                    Bukkit.getOnlinePlayers().forEach(player -> result.add(player.getName()));
                }
                if (Bukkit.getPlayerExact(args[1]) != null) {
                    result.add("-b");
                }
            } else if (args.length == 4 && ConfigManager.configManager.getGrowthDefinition(type, operation.split("@", 2)[0]) != null
                    && Bukkit.getPlayerExact(args[2]) != null) {
                result.add("-b");
            }
        }
        return result.stream().distinct().toList();
    }

    private void execute(CommandSender sender, String[] args) {
        executeProgression(sender, args, "battlepass");
    }

    public static void executeProgression(CommandSender sender, String[] args, String type) {
        String operation = args.length < 2 ? "" : args[1].toLowerCase(Locale.ROOT);
        QuestManager manager = QuestManager.questManager;
        if (operation.equals("season")) {
            if (!sender.hasPermission("ultimatequest.admin")) {
                LanguageManager.languageManager.sendStringText(sender, "error.miss-permission");
                return;
            }
            String action = args.length > 2 ? args[2].toLowerCase(Locale.ROOT) : "";
            int length = switch (action) {
                case "pause", "resume", "end" -> 4;
                case "start", "globalboost" -> 6;
                case "schedule" -> 7;
                default -> 0;
            };
            if (length == 0 || args.length != length) {
                String usage = Set.of("pause", "resume", "end", "start", "schedule", "globalboost").contains(action)
                        ? "battlepass.season-usages." + action : "battlepass.admin-usages.season";
                LanguageManager.languageManager.sendStringText(sender, usage, "type", type);
                return;
            }
            String[] target = args[3].toLowerCase(Locale.ROOT).split(":", 2);
            String targetType = target.length == 2 ? target[0] : type;
            String targetId = target.length == 2 ? target[1] : target[0];
            SeasonManager.seasonManager.editSeason(sender, targetType, targetId, args[2].toLowerCase(Locale.ROOT),
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
                LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-usages." + operation, "type", type);
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
        if (!CacheManager.cacheManager.isReady(player.getUniqueId())) {
            CacheManager.cacheManager.join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return;
        }
        if (Set.of("claimall", "history", "challenges", "stats").contains(operation)) {
            if (args.length != 3) {
                LanguageManager.languageManager.sendStringText(player, "battlepass.usage", "type", type,
                        "actions", type.equals("battlepass") ? "claimall|history|challenges|stats" : "claimall|history|stats");
                return;
            }
            String pass = args[2].toLowerCase(Locale.ROOT);
            switch (operation) {
                case "claimall" -> {
                    manager.claimAll(player, type, pass);
                }
                case "history" -> {
                    QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, pass);
                    if (definition == null) {
                        LanguageManager.languageManager.sendStringText(player, "quests.not-found");
                        return;
                    }
                    List<String> seasons = CacheManager.cacheManager.seasons(player.getUniqueId(), type, pass);
                    if (seasons.isEmpty()) {
                        LanguageManager.languageManager.sendStringText(player, "battlepass.history-empty", "name", definition.name(player));
                    } else if (SeasonManager.seasonManager.season(definition).isEmpty() && seasons.stream().noneMatch(season -> season.contains("@"))) {
                        var quest = manager.growthInstance(player.getUniqueId(), type, pass);
                        LanguageManager.languageManager.sendStringText(player, "battlepass.history-permanent",
                                "name", definition.name(player), "level", String.valueOf(quest.level()),
                                "total", quest.totalAmount().stripTrailingZeros().toPlainString());
                    } else {
                        LanguageManager.languageManager.sendStringText(player, "battlepass.history-title",
                                "name", definition.name(player), "count", String.valueOf(seasons.size()));
                        for (String season : seasons) {
                            var quest = manager.growthInstance(player.getUniqueId(), type, season);
                            if (quest == null) {
                                continue;
                            }
                            QuestDefinition saved = quest.definition();
                            String period = LanguageManager.languageManager.getStringText(player, "menu-time.unlimited");
                            if (quest.startsAt() != null && quest.endsAt() != null) {
                                period = quest.startsAt().atZone(ZoneId.systemDefault()).toLocalDate() + " ~ "
                                        + quest.endsAt().atZone(ZoneId.systemDefault()).toLocalDate();
                            }
                            LanguageManager.languageManager.sendStringText(player, "battlepass.history-entry",
                                    "season", CommonUtil.parseLang(player, saved.settings().getString("season-name", quest.season())),
                                    "period", period, "level", String.valueOf(quest.level()),
                                    "total", quest.totalAmount().stripTrailingZeros().toPlainString());
                            LanguageManager.languageManager.sendStringText(player, "battlepass.history-details",
                                    "type", type, "id", season);
                        }
                    }
                }
                case "challenges" -> {
                    if (type.equals("battlepass")) {
                        new ConfiguredMenuGUI(player, "battlepass/" + pass + "/challenges").openGUI(bypass);
                    }
                }
                case "stats" -> {
                    var quest = manager.growthInstance(player.getUniqueId(), type, pass);
                    if (quest != null) {
                        String season = quest.season().isEmpty()
                                ? LanguageManager.languageManager.getStringText(player, "battlepass.permanent")
                                : CommonUtil.parseLang(player, quest.definition().settings().getString("season-name", quest.season()));
                        LanguageManager.languageManager.sendStringText(player,
                                type.equals("milestone") ? "battlepass.milestone-stats" : "battlepass.stats",
                                "name", quest.definition().name(player), "season", season, "level", String.valueOf(quest.level()),
                                "total", quest.totalAmount().stripTrailingZeros().toPlainString(), "premium",
                                LanguageManager.languageManager.getStringText(player, quest.premium(player)
                                        ? "battlepass.premium-unlocked" : "battlepass.premium-locked"));
                    } else {
                        LanguageManager.languageManager.sendStringText(player, "quests.not-found");
                    }
                }
                default -> {
                }
            }
            return;
        }
        if (args.length <= 2) {
            if (args.length == 2 && ConfigManager.configManager.getGrowthDefinition(type, args[1]) == null
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
                LanguageManager.languageManager.sendStringText(player, "battlepass.claim-usage", "type", type,
                        "tracks", type.equals("milestone") ? "default" : "free|premium");
            }
            return;
        }
        LanguageManager.languageManager.sendStringText(player, "battlepass.usage", "type", type,
                "actions", type.equals("battlepass") ? "claimall|history|challenges|stats" : "claimall|history|stats");
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
