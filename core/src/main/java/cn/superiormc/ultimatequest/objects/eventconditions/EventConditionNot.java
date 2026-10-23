package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;

public final class EventConditionNot extends AbstractEventCondition {

    public EventConditionNot() {
        super("not");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        return !EventConditionsManager.eventConditions.matches(condition.getSection().getConfigurationSection("conditions"), condition.getContext(), condition.player());
    }
}
