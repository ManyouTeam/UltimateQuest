package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.AbstractManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ItemManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import org.bukkit.entity.Player;

public final class SubReload extends AbstractCommand {

    public SubReload() {
        id = "reload";
        requiredPermission = "ultimatequest.reload";
        onlyInGame = false;
        requiredArgLength = new Integer[] { 1 };
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        reload();
        LanguageManager.languageManager.sendStringText(player, "plugin.reloaded");
    }

    @Override
    public void executeCommandInConsole(String[] args) {
        reload();
        LanguageManager.languageManager.sendStringText("plugin.reloaded");
    }

    private void reload() {
        UltimateQuest.instance.reloadConfig();
        new ConfigManager();
        new LanguageManager();
        new ItemManager();
        AbstractManager.reloadManagers();
    }
}
