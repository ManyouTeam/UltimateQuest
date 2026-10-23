package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.QuestManager;
import java.util.ArrayList;
import java.util.List;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class SubAbandon extends AbstractCommand {

    public SubAbandon() {
        id = "abandon";
        requiredPermission = "ultimatequest.use";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 2 };
        requiredConsoleArgLength = new Integer[] { 3 };
        requiredConsolePermission = "ultimatequest.admin";
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        QuestManager.questManager.abandon(player, args[1]);
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
            if (!QuestManager.questManager.ready(target.getUniqueId())) {
                QuestManager.questManager.join(target);
                LanguageManager.languageManager.sendStringText("quests.loading");
                return;
            }
            QuestManager.questManager.abandon(target, args[1]);
        });
    }

    @Override
    protected List<String> getConsoleTabResult(String[] args) {
        if (args.length == 2) {
            return QuestManager.questManager.definitions().stream().map(definition -> definition.id()).toList();
        }
        if (args.length == 3) {
            return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
        }
        return List.of();
    }

    @Override
    protected List<String> getTabResult(String[] args, Player player) {
        List<String> ids = new ArrayList<>();
        QuestManager.questManager.instances(player.getUniqueId()).forEach(quest -> ids.add(quest.key()));
        return ids;
    }

}
