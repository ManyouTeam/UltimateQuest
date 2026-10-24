package cn.superiormc.ultimatequest.objects.rewards;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public final class ObjectRewardPool {

    private final String id;

    private final YamlConfiguration config;

    public ObjectRewardPool(String id, YamlConfiguration config) {
        this.id = id;
        this.config = config;
        ConfigurationSection rewards = config.getConfigurationSection("rewards");
        if (rewards == null || rewards.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Missing rewards in reward pool: " + id);
        }
    }

    public void initRewards(Map<String, YamlConfiguration> pools) {
        for (String key : getRewards().getKeys(false)) {
            if (!key.matches("[a-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid reward choice ID in pool " + id + ": " + key);
            }
        }
        YamlConfiguration check = new YamlConfiguration();
        check.set("rewards.pool.type", "random");
        check.set("rewards.pool.pool", id);
        ObjectReward reward = new ObjectReward(check);
        reward.resolveReferences(check, pools);
        reward.validate(check.getConfigurationSection("rewards"));
    }

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public ConfigurationSection getRewards() {
        return config.getConfigurationSection("rewards");
    }

}
