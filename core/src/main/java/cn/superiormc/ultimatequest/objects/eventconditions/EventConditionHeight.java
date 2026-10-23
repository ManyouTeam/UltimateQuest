package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;

public final class EventConditionHeight extends AbstractEventCondition {

    public EventConditionHeight() {
        super("height");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Location location = condition.getContext() == null ? null : condition.getContext().location();
        if (location == null) {
            return false;
        }
        return location.getBlockY() >= condition.getDouble("min", Integer.MIN_VALUE, condition.getContext()) && location.getBlockY() <= condition.getDouble("max", Integer.MAX_VALUE, condition.getContext());
    }
}
