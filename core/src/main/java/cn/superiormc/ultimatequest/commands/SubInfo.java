package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubInfo extends AbstractCommand {

    public SubInfo() {
        id = "info";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 2 };
        requiredConsoleArgLength = new Integer[] { 3 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        showInfo(player, args[1]);
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        if (args.length != 3) {
            LanguageManager.languageManager.sendStringText("command.console-usage");
            return;
        }
        Player target = Bukkit.getPlayerExact(args[2]);
        if (target == null) {
            LanguageManager.languageManager.sendStringText("error.player-not-found", "player", args[2]);
            return;
        }
        SchedulerUtil.runSync(target, () -> {
            if (!CacheManager.cacheManager.isReady(target.getUniqueId())) {
                CacheManager.cacheManager.join(target);
                LanguageManager.languageManager.sendStringText("quests.loading");
                return;
            }
            showInfo(target, args[1]);
        });
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        if (args.length == 2) {
            return ConfigManager.configManager.getListedDefinitions().stream().map(definition -> definition.id()).toList();
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        if (args.length != 2) {
            return List.of();
        }
        List<String> ids = new ArrayList<>();
        ConfigManager.configManager.getListedDefinitions().forEach(definition -> ids.add(definition.id()));
        CacheManager.cacheManager.instances(player.getUniqueId()).forEach(quest -> ids.add(quest.key()));
        for (String source : List.of("period", "scroll")) {
            QuestManager.questManager.assignedInstances(player.getUniqueId(), source).forEach(quest -> ids.add(quest.key()));
        }
        return ids.stream().distinct().toList();
    }

    public static void showInfo(Player player, String id) {
        QuestInstance instance = CacheManager.cacheManager.instance(player.getUniqueId(), id);
        QuestDefinition definition = instance == null ? ConfigManager.configManager.getDefinition(id) : instance.definition();
        if (definition == null) {
            LanguageManager.languageManager.sendStringText(player, "quests.not-found");
            return;
        }
        TextUtil.sendMessage(player, definition.name(player));
        for (String line : definition.objectiveLines(player, instance)) {
            TextUtil.sendMessage(player, line);
        }
    }
}
