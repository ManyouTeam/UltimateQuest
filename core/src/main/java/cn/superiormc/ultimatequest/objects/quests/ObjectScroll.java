package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.QuestPoolManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.rewards.ObjectReward;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;

public final class ObjectScroll {

    private final ObjectQuestSource source;

    private final String id;

    private final YamlConfiguration config;

    private final Map<String, YamlConfiguration> library;

    public ObjectScroll(String id, YamlConfiguration config, Map<String, YamlConfiguration> pools, Map<String, YamlConfiguration> objectives, Map<String, YamlConfiguration> rewardPools) {
        this.id = id;
        this.config = config;
        this.source = new ObjectQuestSource(config.getConfigurationSection("source"));
        RewardManager.rewardManager.resolveReferences(config, rewardPools);
        ConfigurationSection source = config.getConfigurationSection("source");
        YamlConfiguration pool = source == null || !source.contains("pool") ? null : pools.get(source.getString("pool", ""));
        long count = pool == null ? 0L : QuestDefinition.positiveLong(source, "count", 1L);
        if ((source != null && source.contains("pool") && (pool == null || count > 45L)) || !config.isConfigurationSection("item")) {
            throw new IllegalArgumentException("Invalid scroll configuration: " + id);
        }
        readDuration(config);
        if (pool != null && pool.getConfigurationSection("quests").getKeys(false).size() < count) {
            throw new IllegalArgumentException("Scroll " + id + " has insufficient objectives");
        }
        ConfigurationSection tasks = config.getConfigurationSection("tasks");
        if (source == null && (tasks == null || tasks.getKeys(false).isEmpty())) {
            throw new IllegalArgumentException("Scroll " + id + " requires tasks or source.pool");
        }
        if (source != null && !source.contains("pool")) {
            YamlConfiguration resolved = QuestObjective.resolve("main", source, objectives);
            new QuestObjective("main", resolved);
        }
        if (tasks != null) {
            for (String taskId : tasks.getKeys(false)) {
                YamlConfiguration resolved = QuestObjective.resolve(taskId, tasks.getConfigurationSection(taskId), objectives);
                new QuestObjective(taskId, resolved);
            }
        }
        RewardManager.rewardManager.validate(config.getConfigurationSection("rewards"));
        this.library = Map.copyOf(objectives);
    }

    public void initReferences(Map<String, YamlConfiguration> categories, Map<String, QuestDefinition> definitions) {
        ObjectCategory.validateCategories(config, categories, "scrolls/" + id + ".yml");
        new ObjectReward(config.getConfigurationSection("rewards")).validateTargets(definitions);
    }

    public YamlConfiguration createRecord(String rarity, String difficulty, RandomGenerator random) {
        YamlConfiguration config = this.config;
        if (config == null) {
            throw new IllegalArgumentException("Unknown scroll id: " + id);
        }
        ConfigurationSection rarities = config.getConfigurationSection("rarities");
        ConfigurationSection difficulties = config.getConfigurationSection("difficulties");
        rarity = rarities == null ? config.getString("rarity", "common") : rarity == null ? QuestPoolManager.choose(rarities, random)
                                                                                          : rarity;
        difficulty = difficulties == null ? config.getString("difficulty", "normal") : difficulty == null ? QuestPoolManager.choose(difficulties, random)
                                                                                                          : difficulty;
        if (rarities != null && !rarities.contains(rarity) || difficulties != null && !difficulties.contains(difficulty)) {
            throw new IllegalArgumentException("Unknown scroll rarity or difficulty");
        }
        Set<String> objectiveIds = new LinkedHashSet<>();
        ConfigurationSection fixed = config.getConfigurationSection("tasks");
        if (fixed != null) {
            objectiveIds.addAll(fixed.getKeys(false));
        }
        objectiveIds.addAll(this.source.draw(random));
        createDefinition(List.copyOf(objectiveIds));
        YamlConfiguration record = new YamlConfiguration();
        record.set("schema-version", 3);
        record.set("kind", "scroll");
        record.set("state", "ISSUED");
        record.set("profile", id);
        record.set("rarity", rarity);
        record.set("difficulty", difficulty);
        record.set("duration-seconds", readDuration(config));
        record.set("objective-ids", List.copyOf(objectiveIds));
        return record;
    }

    public QuestDefinition createDefinition(List<String> objectiveIds) {
        YamlConfiguration config = this.config;
        if (config == null) {
            throw new IllegalArgumentException("Unknown scroll id: " + id);
        }
        if (objectiveIds.isEmpty() || objectiveIds.size() > 45 || new HashSet<>(objectiveIds).size() != objectiveIds.size()) {
            throw new IllegalArgumentException("Invalid scroll objective IDs: " + id);
        }
        YamlConfiguration task = new YamlConfiguration();
        task.set("type", "normal");
        task.set("listed", false);
        task.set("name", config.getString("name", id));
        task.set("description", config.getStringList("description"));
        task.set("categories", config.getStringList("categories"));
        ConfigurationFileUtil.copy(config.getConfigurationSection("icon"), task.createSection("icon"));
        ConfigurationFileUtil.copy(config.getConfigurationSection("rewards"), task.createSection("rewards"));
        ConfigurationSection fixed = config.getConfigurationSection("tasks");
        if (fixed != null) {
            for (String id : fixed.getKeys(false)) {
                ConfigurationFileUtil.copy(fixed.getConfigurationSection(id), task.createSection("objectives." + id));
            }
        }
        ConfigurationSection source = config.getConfigurationSection("source");
        if (source != null) {
            if (source.contains("pool")) {
                for (String objective : objectiveIds) {
                    if (fixed == null || !fixed.contains(objective)) {
                        task.set("objectives." + objective + ".source.quest", objective);
                    }
                }
            } else {
                ConfigurationFileUtil.copy(source, task.createSection("objectives.main.source"));
            }
        }
        QuestDefinition definition = new QuestDefinition(id, task, library);
        Set<String> resolvedIds = new HashSet<>();
        for (QuestObjective objective : definition.objectives()) {
            resolvedIds.add(objective.getId());
        }
        if (!resolvedIds.equals(new HashSet<>(objectiveIds))) {
            throw new IllegalArgumentException("Scroll objective IDs do not match id: " + id);
        }
        return definition;
    }

    public ItemStack buildItem(Player player, String rarity, String difficulty) {
        ItemStack item = BuildItem.buildItemStack(player, config.getConfigurationSection("item"));
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Invalid scroll item");
        }
        item.setAmount(1);
        ItemMeta meta = item.getItemMeta();
        UltimateQuest.methodUtil.setItemName(meta, CommonUtil.modifyString(player, config.getString("name", id), "rarity", rarity, "difficulty", difficulty), player);
        item.setItemMeta(meta);
        return item;
    }

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public ObjectQuestSource getSource() {
        return source;
    }

    public static long readDuration(YamlConfiguration config) {
        if (!config.contains("duration-seconds")) {
            return 0L;
        }
        Object value = config.get("duration-seconds");
        if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 0L) {
            throw new IllegalArgumentException("Scroll duration must be a nonnegative integer");
        }
        long seconds = ((Number) value).longValue();
        Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
        return seconds;
    }

}
