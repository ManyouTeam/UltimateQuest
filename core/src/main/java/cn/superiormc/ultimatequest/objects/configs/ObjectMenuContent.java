package cn.superiormc.ultimatequest.objects.configs;

import org.bukkit.configuration.ConfigurationSection;

public final class ObjectMenuContent {

    private final ObjectButton button;

    private final String id;

    private final ConfigurationSection config;

    public ObjectMenuContent(String id, ConfigurationSection config) {
        this.button = new ObjectQuestButton(id, config);
        this.id = id;
        this.config = config;
    }

    public String getId() {
        return id;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public ObjectButton getButton() {
        return button;
    }
}
