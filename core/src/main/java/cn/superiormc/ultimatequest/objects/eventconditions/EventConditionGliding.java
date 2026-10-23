package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.LivingEntity;

public final class EventConditionGliding extends AbstractEventCondition {

    public EventConditionGliding() {
        super("gliding");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        LivingEntity living = context == null ? null : context.livingEntity(condition.getSection().getString("target"), EntitySelector.TARGET);
        return living != null && living.isGliding() == condition.getSection().getBoolean("value", true);
    }
}
