package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;

public final class EventConditionThunder extends AbstractEventCondition {

    public EventConditionThunder() {
        super("thunder");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Location location = condition.getContext() == null ? null : condition.getContext().location();
        return location != null && location.getWorld() != null && location.getWorld().isThundering() == condition.getSection().getBoolean("value", true);
    }
}
