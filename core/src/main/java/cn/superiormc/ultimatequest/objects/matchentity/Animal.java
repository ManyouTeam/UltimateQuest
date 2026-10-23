package cn.superiormc.ultimatequest.objects.matchentity;

import cn.superiormc.ultimatequest.objects.ObjectSingleMatchEntity;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Animals;

public final class Animal extends AbstractMatchEntityRule {

    @Override
    public boolean getMatch(ObjectSingleMatchEntity match) {
        boolean result = match.getEntity() instanceof Animals;
        return match.getSection().getBoolean("animal") ? result : !result;
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("animal");
    }
}
