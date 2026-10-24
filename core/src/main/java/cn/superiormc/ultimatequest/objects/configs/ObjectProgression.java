package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.TreeMap;

public final class ObjectProgression {

    private final QuestDefinition definition;

    private final YamlConfiguration config;

    private final Map<Integer, Long> thresholds;

    public ObjectProgression(QuestDefinition definition, YamlConfiguration config) {
        this.definition = definition;
        this.config = config;
        generateLevels();
        thresholds = loadThresholds();
    }

    public Map<Integer, Long> getThresholds() {
        return thresholds;
    }

    private void generateLevels() {
        if (!definition.type().equals("battlepass") || !config.isConfigurationSection("leveling")) {
            return;
        }
        int count = config.getInt("leveling.count", 0);
        long base = config.getLong("leveling.base", 100L);
        long increment = config.getLong("leveling.increment", 0L);
        double multiplier = config.getDouble("leveling.multiplier", 1D);
        if (count <= 0 || count > 10000 || base <= 0 || increment < 0 || !Double.isFinite(multiplier) || multiplier < 1D) {
            throw new IllegalArgumentException("Invalid level curve: " + definition.id());
        }
        long total = 0L;
        for (int level = 1; level <= count; level++) {
            double value = (base + (double) increment * (level - 1)) * Math.pow(multiplier, level - 1);
            if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
                throw new IllegalArgumentException("Level curve overflows: " + definition.id());
            }
            total = Math.addExact(total, (long) Math.ceil(value));
            String path = "levels." + level;
            if (!config.contains(path + ".required")) {
                config.set(path + ".required", total);
            } else {
                total = config.getLong(path + ".required");
            }
        }
    }

    private Map<Integer, Long> loadThresholds() {
        if (!definition.growth()) {
            return Map.of();
        }
        ConfigurationSection section = config.getConfigurationSection(definition.type().equals("battlepass") ? "levels" : "stages");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Missing progression stages: " + definition.id());
        }
        Map<Integer, Long> sorted = new TreeMap<>();
        for (String key : section.getKeys(false)) {
            int level = Integer.parseInt(key);
            ConfigurationSection stage = section.getConfigurationSection(key);
            if (level <= 0 || !String.valueOf(level).equals(key) || stage == null) {
                throw new IllegalArgumentException("Invalid progression stage: " + key);
            }
            long required = QuestDefinition.positiveLong(stage, "required", 0L);
            sorted.put(level, required);
            for (String track : definition.tracks()) {
                String rewardKey = definition.type().equals("battlepass") ? track : "rewards";
                if (stage.contains(rewardKey) && !stage.isConfigurationSection(rewardKey)) {
                    throw new IllegalArgumentException("Stage rewards must be a section: " + key + "/" + rewardKey);
                }
                RewardManager.rewardManager.validate(definition.stageRewards(level, track));
            }
        }
        int expected = 1;
        long previous = 0L;
        for (Map.Entry<Integer, Long> entry : sorted.entrySet()) {
            if (entry.getKey() != expected || entry.getValue() <= previous) {
                throw new IllegalArgumentException("Stages must start at 1 and have increasing thresholds: " + definition.id());
            }
            expected++;
            previous = entry.getValue();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
    }
}
