package cn.superiormc.ultimatequest.objects.configs;

import org.bukkit.configuration.ConfigurationSection;

public final class ObjectQuestButton extends ObjectButton {

    public ObjectQuestButton(String id, ConfigurationSection config) {
        super(id, config);
    }

    public String getContentType() {
        return config.getString("type", "quests");
    }
}
