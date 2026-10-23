package cn.superiormc.ultimatequest.objects.matchentity;

import cn.superiormc.ultimatequest.objects.ObjectSingleMatchEntity;
import org.bukkit.configuration.ConfigurationSection;

public class EntityNone extends AbstractMatchEntityRule {

    public EntityNone() {
        super();
    }

    @Override
    public boolean getMatch(ObjectSingleMatchEntity match) {
        ConfigurationSection section = match.getSection();
        return !section.getBoolean("none");
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("none");
    }
}
