package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;

public final class EventConditionRandom extends AbstractEventCondition {

    public EventConditionRandom() {
        super("random");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        double chance = condition.getDouble("value", condition.player(), condition.getContext());
        return Math.random() < Math.clamp(chance, 0.0D, 1.0D);
    }
}
