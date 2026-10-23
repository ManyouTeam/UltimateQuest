package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;

public final class EventConditionLightLevel extends AbstractEventCondition {

    public EventConditionLightLevel() {
        super("light_level");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || condition.getContext().source() == null) {
            return false;
        }
        int light = condition.getContext().source().getLocation().getBlock().getLightLevel();
        return light >= condition.getDouble("min", 0, condition.getContext()) && light <= condition.getDouble("max", 15, condition.getContext());
    }
}
