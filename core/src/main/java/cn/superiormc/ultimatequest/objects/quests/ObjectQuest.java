package cn.superiormc.ultimatequest.objects.quests;

import org.bukkit.configuration.file.YamlConfiguration;

import java.util.Map;

public final class ObjectQuest {

    private final String id;

    private final YamlConfiguration config;

    private QuestObjective objective;

    public ObjectQuest(String id, YamlConfiguration config) {
        this.id = id;
        this.config = config;
    }

    public void initObjective(Map<String, YamlConfiguration> library) {
        objective = new QuestObjective(id, QuestObjective.resolve(id, config, library));
    }

    public void initObjective(Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> categories) {
        initObjective(library);
        ObjectCategory.validateCategories(config, categories, "quests/" + id + ".yml");
    }

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public QuestObjective getObjective() {
        return objective;
    }

}
