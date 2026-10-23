package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.event.entity.EntityRegainHealthEvent;

public final class EventConditionRegainAmount extends AbstractNumericEventCondition {

    public EventConditionRegainAmount() {
        super("regain_amount");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.event() instanceof EntityRegainHealthEvent event ? event.getAmount() : null;
    }
}
