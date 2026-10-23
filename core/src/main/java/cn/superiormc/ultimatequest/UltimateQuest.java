package cn.superiormc.ultimatequest;

import cn.superiormc.ultimatequest.managers.AbstractManager;
import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.managers.AntiAbuseManager;
import cn.superiormc.ultimatequest.managers.ChangesManager;
import cn.superiormc.ultimatequest.managers.CommandManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.HookManager;
import cn.superiormc.ultimatequest.managers.InitManager;
import cn.superiormc.ultimatequest.managers.ItemManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.ListenerManager;
import cn.superiormc.ultimatequest.managers.MatchEntityManager;
import cn.superiormc.ultimatequest.managers.MatchItemManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.managers.QuestPoolManager;
import cn.superiormc.ultimatequest.managers.QuestPeriodManager;
import cn.superiormc.ultimatequest.managers.QuestScrollManager;
import cn.superiormc.ultimatequest.managers.QuestStorage;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SpecialMethodUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class UltimateQuest extends JavaPlugin {

    public static UltimateQuest instance;

    public static SpecialMethodUtil methodUtil;

    public static boolean isFolia = false;

    public static int yearVersion;

    public static int majorVersion;

    public static int minorVersion;

    @Override
    public void onEnable() {
        instance = this;
        DatabaseExecutor.start();
        try {
            String[] versionParts = Bukkit.getBukkitVersion().split("-")[0].split("\\.");
            yearVersion = versionParts.length > 0 && versionParts[0].matches("\\d+") ? Integer.parseInt(versionParts[0]) : 1;
            majorVersion = versionParts.length > 1 && versionParts[1].matches("\\d+") ? Integer.parseInt(versionParts[1]) : 0;
            minorVersion = versionParts.length > 2 && versionParts[2].matches("\\d+") ? Integer.parseInt(versionParts[2]) : 0;
        } catch (Throwable throwable) {
            Bukkit.getConsoleSender().sendMessage(TextUtil.pluginPrefix() + " §cError: Can not get your Minecraft version! Default set to 1.0.0.");
        }
        if (CommonUtil.getClass("com.destroystokyo.paper.PaperConfig") && CommonUtil.getMinorVersion(18, 2)) {
            try {
                Class<?> paperClass = Class.forName("cn.superiormc.ultimatequest.paper.PaperMethodUtil");
                methodUtil = (SpecialMethodUtil) paperClass.getDeclaredConstructor().newInstance();
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fPaper is found, entering Paper plugin mode...");
            } catch (Throwable throwable) {
                Bukkit.getConsoleSender().sendMessage(TextUtil.pluginPrefix() + " §cError: The plugin seems break, please download it again from site.");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        } else {
            try {
                Class<?> spigotClass = Class.forName("cn.superiormc.ultimatequest.spigot.SpigotMethodUtil");
                methodUtil = (SpecialMethodUtil) spigotClass.getDeclaredConstructor().newInstance();
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fSpigot is found, entering Spigot plugin mode...");
            } catch (Throwable throwable) {
                Bukkit.getConsoleSender().sendMessage(TextUtil.pluginPrefix() + " §cError: The plugin seems break, please download it again from site.");
                Bukkit.getPluginManager().disablePlugin(this);
            }
        }
        if (CommonUtil.getClass("io.papermc.paper.threadedregions.RegionizedServer")) {
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fFolia is found, enabled Folia compatibility feature!");
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §6Warning: Folia support is not fully test, major bugs maybe found! " +
                    "Please do not use in production environment!");
            isFolia = true;
        }
        new ErrorManager();
        new InitManager();
        new ConfigManager();
        new HookManager();
        new ItemManager();
        new LanguageManager();
        new MatchEntityManager();
        new MatchItemManager();
        new ChangesManager();
        new EventConditionsManager();
        new AntiAbuseManager();
        new TriggerManager();
        new QuestStorage();
        new RewardManager();
        new QuestPoolManager();
        new QuestPeriodManager();
        new QuestScrollManager();
        new QuestManager();
        new CommandManager();
        new ListenerManager();
        AbstractManager.initializeManagers();
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fYour server version is: " + yearVersion + "." + majorVersion + "." + minorVersion + "!");
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fPlugin is loaded. Author: PQguanfang.");
    }

    @Override
    public void onDisable() {
        AbstractManager.disableManagers();
        if (methodUtil != null) {
            Bukkit.getOnlinePlayers().forEach(methodUtil::clearBossBars);
        }
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fUltimateQuest is disabled.");
    }

}
