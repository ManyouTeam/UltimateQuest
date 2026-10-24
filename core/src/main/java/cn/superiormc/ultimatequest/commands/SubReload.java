package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
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
        synchronized (ConfigManager.configManager) {
            try {
                ConfigManager.configManager.reloadMainConfig();
                new LanguageManager();
                ItemManager.itemManager.initSavedItems();
                ConfigManager.configManager.onInit();
                EventConditionsManager.eventConditions.onPluginReload();
            } catch (Exception exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not reload plugin configurations: " + exception.getMessage());
                throw new IllegalStateException("Configuration reload failed", exception);
            }
        }
        CacheManager.cacheManager.configureAutoSave();
    }
}
