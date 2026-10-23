package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.event.entity.EntityCombustEvent;

public final class EventConditionCombustDuration extends AbstractNumericEventCondition {

    public EventConditionCombustDuration() {
        super("combust_duration");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.event() instanceof EntityCombustEvent event ? (double) event.getDuration() : null;
    }
}
