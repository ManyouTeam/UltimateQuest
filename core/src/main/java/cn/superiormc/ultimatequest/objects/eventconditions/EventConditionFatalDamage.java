package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.LivingEntity;
import org.bukkit.event.entity.EntityDamageEvent;

public final class EventConditionFatalDamage extends AbstractEventCondition {

    public EventConditionFatalDamage() {
        super("fatal_damage");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        return condition.getContext() != null && condition.getContext().event() instanceof EntityDamageEvent event && event.getEntity() instanceof LivingEntity living && event.getFinalDamage() >= living.getHealth();
    }
}
