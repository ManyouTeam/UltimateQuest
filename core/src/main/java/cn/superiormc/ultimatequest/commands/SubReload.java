package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.ItemManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
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
        new ConfigManager();
        new LanguageManager();
        ItemManager.itemManager.initSavedItems();
        try {
            QuestManager.questManager.reloadDefinitions();
            EventConditionsManager.eventConditions.onPluginReload();
            QuestManager.questManager.configureAutoSave();
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not reload quests; keeping previous definitions: " + exception.getMessage());
            throw new IllegalStateException("Quest reload failed", exception);
        }
    }
}
