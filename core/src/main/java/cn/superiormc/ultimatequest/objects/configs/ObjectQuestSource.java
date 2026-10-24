package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.*;
import cn.superiormc.ultimatequest.objects.quests.*;

import org.bukkit.configuration.ConfigurationSection;

import java.util.*;
import java.util.random.RandomGenerator;

public final class ObjectQuestSource {

    private final ConfigurationSection config;

    public ObjectQuestSource(ConfigurationSection config) {
        this.config = config;
    }

    public boolean usesPool() {
        return config != null && config.contains("pool");
    }

    public List<String> draw(RandomGenerator random) {
        if (config == null) {
            return List.of();
        }
        if (!usesPool()) {
            return List.of("main");
        }
        return QuestPoolManager.questPoolManager.draw(config.getString("pool"), config.getInt("count", 1), random);
    }

    public ConfigurationSection getConfig() {
        return config;
    }
}
