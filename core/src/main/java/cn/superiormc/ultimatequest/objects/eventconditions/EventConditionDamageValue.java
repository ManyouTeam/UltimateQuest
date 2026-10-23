package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.event.entity.EntityDamageEvent;

public final class EventConditionDamageValue extends AbstractNumericEventCondition {

    public EventConditionDamageValue() {
        super("damage_value");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.event() instanceof EntityDamageEvent event ? event.getDamage() : null;
    }
}
