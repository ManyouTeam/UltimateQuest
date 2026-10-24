package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ObjectBattlePass extends QuestDefinition {

    private final Map<String, ObjectBattlePassTask> tasks = new LinkedHashMap<>();

    public ObjectBattlePass(String id, YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools) {
        super(id, config, library);
        ConfigurationSection section = config.getConfigurationSection("tasks");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (!key.matches("[a-z0-9_-]+") || !section.isConfigurationSection(key)) {
                throw new IllegalArgumentException("Invalid battle pass task: " + id + "/" + key);
            }
            tasks.put(key, new ObjectBattlePassTask(id, key, section.getConfigurationSection(key), library, rewardPools));
        }
    }

    public Map<String, ObjectBattlePassTask> getTasks() {
        return Collections.unmodifiableMap(tasks);
    }
}
