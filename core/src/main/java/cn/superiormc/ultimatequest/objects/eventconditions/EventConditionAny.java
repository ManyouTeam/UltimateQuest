package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;

public final class EventConditionAny extends AbstractEventCondition {

    public EventConditionAny() {
        super("any");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        return EventConditionsManager.eventConditions.matchesAny(condition.getSection().getConfigurationSection("conditions"), condition.getContext(), condition.player());
    }
}
