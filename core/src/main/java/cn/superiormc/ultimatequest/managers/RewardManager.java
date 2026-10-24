package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.ObjectSingleAction;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import cn.superiormc.ultimatequest.objects.rewards.ObjectReward;

public final class RewardManager extends AbstractManager {

    public static RewardManager rewardManager;

    public RewardManager() {
        rewardManager = this;
    }

    public void resolveReferences(ConfigurationSection config) {
        new ObjectReward(config).resolveReferences(config);
    }

    public Map<String, YamlConfiguration> loadPools() {
        return ConfigManager.configManager.getRewardPoolConfigurations();
    }

    public void resolveReferences(ConfigurationSection config, Map<String, YamlConfiguration> pools) {
        new ObjectReward(config).resolveReferences(config, pools);
    }

    public void validate(ConfigurationSection section) {
        new ObjectReward(section).validate();
    }

    /**
     * Walks every reward entry, descending into random choices so callers can inspect nested rewards.
     */
    public void forEachReward(ConfigurationSection section, BiConsumer<String, ConfigurationSection> consumer) {
        new ObjectReward(section).forEachReward(section, consumer);
    }

    public boolean hasRandom(ConfigurationSection section) {
        return new ObjectReward(section).hasRandom(section);
    }

    public YamlConfiguration resolveSnapshot(ConfigurationSection section) {
        return new ObjectReward(section).resolveSnapshot(section);
    }

    public List<String> drawKeys(ConfigurationSection section) {
        return new ObjectReward(section).drawKeys(section);
    }

    public YamlConfiguration resolveKeys(ConfigurationSection section, List<String> keys) {
        return new ObjectReward(section).resolveKeys(section, keys);
    }

    public List<String> preview(Player player, ConfigurationSection section) {
        return new ObjectReward(section).preview(player);
    }

    public CompletableFuture<Void> grant(Player player, QuestDefinition definition) {
        return grant(player, definition.rewards(), definition.id());
    }

    public CompletableFuture<Void> grant(Player player, ConfigurationSection section, String questId) {
        return new ObjectReward(section).grant(player, questId);
    }
}
