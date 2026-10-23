package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;

public final class EventConditionStorm extends AbstractEventCondition {

    public EventConditionStorm() {
        super("storm");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Location location = condition.getContext() == null ? null : condition.getContext().location();
        return location != null && location.getWorld() != null && location.getWorld().hasStorm() == condition.getSection().getBoolean("value", true);
    }
}
