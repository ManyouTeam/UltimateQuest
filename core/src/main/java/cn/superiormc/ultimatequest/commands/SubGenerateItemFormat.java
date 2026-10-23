package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.methods.DebuildItem;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public class SubGenerateItemFormat extends AbstractCommand {

    public SubGenerateItemFormat() {
        this.id = "generateitemformat";
        this.requiredPermission = "ultimatequest." + id;
        this.onlyInGame = true;
        this.requiredArgLength = new Integer[] { 1 };
    }

    @Override
    public void executeCommandInGame(String[] args, Player player) {
        YamlConfiguration itemConfig = new YamlConfiguration();
        DebuildItem.debuildItem(player.getInventory().getItemInMainHand(), itemConfig);
        String yaml = itemConfig.saveToString();
        SchedulerUtil.runTaskAsynchronously(() -> {
            Path path = new File(UltimateQuest.instance.getDataFolder(), "generated-item-format.yml").toPath();
            try {
                Files.write(path, yaml.getBytes(StandardCharsets.UTF_8));
            } catch (IOException e) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not export item: " + e.getMessage());
            }
        });
        LanguageManager.languageManager.sendStringText(player, "plugin.generated");
        if (ConfigManager.configManager.getString("debuild-item-method", "LEGACY").equalsIgnoreCase("COMPONENT")) {
            TextUtil.sendMessage(player, TextUtil.pluginPrefix() + " §fYou are using the modern component item debuilder, this feature is still in early alpha test, " + "if you found any issue, please report it to author. You can still use LEGACY item debuilder by setting debuild-item-method option in config.yml file.");
        }
    }
}
