package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.LanguageManager;
import org.bukkit.entity.Player;

public final class SubHelp extends AbstractCommand {

    public SubHelp() {
        id = "help";
        requiredPermission = "";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1 };
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        LanguageManager.languageManager.sendStringText(player, "quests.help");
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        LanguageManager.languageManager.sendStringText("quests.help");
    }

}
