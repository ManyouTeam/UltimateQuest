package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.event.entity.ExplosionPrimeEvent;

public final class EventConditionExplosionRadius extends AbstractNumericEventCondition {

    public EventConditionExplosionRadius() {
        super("explosion_radius");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.event() instanceof ExplosionPrimeEvent event ? (double) event.getRadius() : null;
    }
}
