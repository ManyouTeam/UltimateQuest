package cn.superiormc.ultimatequest.objects.quests;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class ObjectTask extends QuestDefinition {

    public ObjectTask(String id, YamlConfiguration config, Map<String, YamlConfiguration> library) {
        this(id, config, library, Map.of());
    }

    public ObjectTask(String id, YamlConfiguration config, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewards) {
        super(id, prepareConfiguration(id, "normal", config, rewards, null), library);
    }

}
