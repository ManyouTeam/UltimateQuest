package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public class ObjectTask extends QuestDefinition {

    public ObjectTask(String id, YamlConfiguration config, Map<String, YamlConfiguration> library) {
        super(id, config, library);
    }
}
