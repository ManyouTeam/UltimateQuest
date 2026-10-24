package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.SeasonManager;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ObjectBattlePass extends QuestDefinition {

    private final Map<String, ObjectBattlePassTask> tasks = new LinkedHashMap<>();

    public ObjectBattlePass(String id, YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools) {
        this(id, config, library, rewardPools, null);
    }

    public ObjectBattlePass(String id, YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools, SeasonManager seasons) {
        super(id, prepareConfiguration(id, "battlepass", config, rewardPools, seasons), library);
        initTasks(config, library, rewardPools);
    }

    private void initTasks(YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools) {
        ConfigurationSection section = config.getConfigurationSection("tasks");
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            if (!key.matches("[a-z0-9_-]+") || !section.isConfigurationSection(key)) {
                throw new IllegalArgumentException("Invalid battle pass task: " + id() + "/" + key);
            }
            tasks.put(key, new ObjectBattlePassTask(id(), key, section.getConfigurationSection(key), library, rewardPools));
        }
    }

    @Override
    public void initReferences(Map<String, YamlConfiguration> categories, Map<String, QuestDefinition> definitions) {
        super.initReferences(categories, definitions);
        for (ObjectBattlePassTask task : tasks.values()) {
            task.initReferences(categories, definitions);
        }
    }

    public Map<String, ObjectBattlePassTask> getTasks() {
        return Collections.unmodifiableMap(tasks);
    }

}
