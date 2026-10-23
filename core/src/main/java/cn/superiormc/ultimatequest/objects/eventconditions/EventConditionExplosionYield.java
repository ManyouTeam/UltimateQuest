package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.event.entity.EntityExplodeEvent;

public final class EventConditionExplosionYield extends AbstractNumericEventCondition {

    public EventConditionExplosionYield() {
        super("explosion_yield");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.event() instanceof EntityExplodeEvent event ? (double) event.getYield() : null;
    }
}
