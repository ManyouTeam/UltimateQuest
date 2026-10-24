package cn.superiormc.ultimatequest.objects.quests;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class ObjectMilestone extends QuestDefinition {

    public ObjectMilestone(String id, YamlConfiguration config, Map<String, YamlConfiguration> library) {
        this(id, config, library, Map.of());
    }

    public ObjectMilestone(String id, YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewards) {
        super(id, prepareConfiguration(id, "milestone", config, rewards), library);
    }

}
