package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.QuestPoolManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.random.RandomGenerator;

public final class ObjectQuestPool {

    private final String id;

    private final YamlConfiguration config;

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public ObjectQuestPool(String id, YamlConfiguration config, Map<String, YamlConfiguration> pools, Map<String, YamlConfiguration> objectives, Map<String, YamlConfiguration> rewardPools) {
        this.id = id;
        this.config = config;
        ConfigurationSection quests = config.getConfigurationSection("quests");
        QuestPoolManager.validateWeights(quests);
        for (String questId : quests.getKeys(false)) {
            if (!objectives.containsKey(questId)) {
                throw new IllegalArgumentException("Unknown objective in pool " + id + ": " + questId);
            }
        }
        this.objectives = Map.copyOf(objectives);
    }

    private final Map<String, YamlConfiguration> objectives;

    public List<String> draw(int count, RandomGenerator random) {
        String pool = id;
        YamlConfiguration config = this.config;
        if (config == null || count < 1 || count > 45) {
            throw new IllegalArgumentException("Unknown pool or invalid draw count: " + pool);
        }
        ConfigurationSection weights = config.getConfigurationSection("quests");
        List<String> candidates = new ArrayList<>();
        for (String questId : weights.getKeys(false)) {
            if (objectives.containsKey(questId)) {
                candidates.add(questId);
            }
        }
        return QuestPoolManager.drawCandidates(pool, weights, candidates, count, random);
    }
}
