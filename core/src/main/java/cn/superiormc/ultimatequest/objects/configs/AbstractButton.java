package cn.superiormc.ultimatequest.objects.configs;

import cn.superiormc.ultimatequest.gui.action.GUIActionContext;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public abstract class AbstractButton {

    protected final ConfigurationSection config;

    protected AbstractButton(ConfigurationSection config) {
        this.config = config;
    }

    public ConfigurationSection getConfig() {
        return config;
    }

    public abstract void clickEvent(Player player, GUIActionContext context);
}
