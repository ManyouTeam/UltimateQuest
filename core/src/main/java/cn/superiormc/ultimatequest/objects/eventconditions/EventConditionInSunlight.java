package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public final class EventConditionInSunlight extends AbstractEventCondition {

    public EventConditionInSunlight() {
        super("in_sunlight");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        if (entity == null) {
            return false;
        }
        Location location = entity.getLocation();
        boolean sunlight = location.getWorld() != null && location.getWorld().getTime() < 12300 && location.getBlock().getLightFromSky() >= 15;
        return sunlight == condition.getSection().getBoolean("value", true);
    }
}
