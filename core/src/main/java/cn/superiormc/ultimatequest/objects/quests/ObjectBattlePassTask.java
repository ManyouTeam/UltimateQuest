package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.RewardManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;
import java.util.Set;

public final class ObjectBattlePassTask extends ObjectTask {

    public ObjectBattlePassTask(String pass, String taskId, ConfigurationSection task, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools) {
        super(pass + "_" + taskId, prepare(pass, taskId, task, rewardPools), library);
    }

    public String getBattlePass() {
        return settings().getString("challenge.pass");
    }

    public long getPoints() {
        return settings().getLong("challenge.points");
    }

    public int getUnlockWeek() {
        return settings().getInt("challenge.week");
    }

    public String getReset() {
        return settings().getString("challenge.reset");
    }

    private static YamlConfiguration prepare(String pass, String taskId, ConfigurationSection task, Map<String, YamlConfiguration> rewardPools) {
        YamlConfiguration config = new YamlConfiguration();
        copySection(task, config);
        config.set("type", "normal");
        config.set("listed", false);
        config.set("auto-claim", false);
        config.set("challenge.pass", pass);
        config.set("challenge.points", QuestDefinition.positiveLong(task, "points", 0L));
        config.set("challenge.week", Math.max(1, task.getInt("unlock-week", 1)));
        config.set("challenge.catch-up", task.getBoolean("catch-up", true));
        config.set("challenge.premium", task.getBoolean("premium", false));
        String reset = task.getString("reset", "season");
        if (!Set.of("daily", "weekly", "season").contains(reset)) {
            throw new IllegalArgumentException("Invalid battle pass task reset: " + pass + "/" + taskId);
        }
        config.set("challenge.reset", reset);

        RewardManager.rewardManager.resolveReferences(config, rewardPools);
        return config;
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

}
