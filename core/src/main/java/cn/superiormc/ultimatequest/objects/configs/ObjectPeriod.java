package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.QuestPeriodManager;
import cn.superiormc.ultimatequest.managers.QuestPoolManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.time.Instant;
import java.util.Map;
import java.util.Random;

public final class ObjectPeriod {

    private final ObjectQuestSource source;

    private final String id;

    private final YamlConfiguration config;

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public ObjectPeriod(String id, YamlConfiguration config, Map<String, YamlConfiguration> pools, Map<String, YamlConfiguration> objectives, Map<String, YamlConfiguration> rewardPools) {
        this.id = id;
        this.config = config;
        this.source = new ObjectQuestSource(config.getConfigurationSection("source"));
        RewardManager.rewardManager.resolveReferences(config, rewardPools);
        RewardManager.rewardManager.validate(config.getConfigurationSection("task.rewards"));
        ConfigurationSection source = config.getConfigurationSection("source");
        if (source == null) {
            throw new IllegalArgumentException("Periodic quest " + id + " requires a source section");
        }
        if (source.contains("pool")) {
            try {
                QuestPoolManager.draw(source, pools, objectives, new Random(0L));
            } catch (IllegalArgumentException exception) {
                throw new IllegalArgumentException("Invalid periodic quest pool in " + id + ": " + exception.getMessage(), exception);
            }
        } else {
            YamlConfiguration resolved = QuestObjective.resolve(id, source, objectives);
            new QuestObjective(id, resolved);
        }
        QuestPeriodManager.start(config, Instant.now());
        QuestPeriodManager.end(config, Instant.now());
        TimeWindow.read(config);
    }

    public QuestDefinition createDefinition(String objective, Map<String, YamlConfiguration> objectives) {
        String group = id;
        ConfigurationSection period = this.config;
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationSection task = period.getConfigurationSection("task");
        if (task != null) {
            copySection(task, config);
        }
        config.set("source-name", period.getString("name", group));
        YamlConfiguration template = objective == null ? null : objectives.get(objective);
        if (!config.contains("categories") && template != null && template.isList("categories")) {
            config.set("categories", template.getStringList("categories"));
        }
        if (!config.contains("name")) {
            config.set("name", template == null ? group : template.getString("name", objective));
        }
        if (template != null && !config.isConfigurationSection("icon") && template.isConfigurationSection("icon")) {
            ConfigurationSection icon = config.createSection("icon");
            copySection(template.getConfigurationSection("icon"), icon);
        }
        config.set("type", "normal");
        config.set("listed", false);
        if (objective == null) {
            copySection(period.getConfigurationSection("source"), config.createSection("source"));
        } else {
            config.set("source.quest", objective);
        }
        return new QuestDefinition(group + "_" + (objective == null ? "main" : objective), config, objectives);
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public ObjectQuestSource getSource() {
        return source;
    }
}
