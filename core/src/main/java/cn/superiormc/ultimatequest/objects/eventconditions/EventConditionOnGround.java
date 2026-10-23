package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.LivingEntity;

public final class EventConditionOnGround extends AbstractEventCondition {

    public EventConditionOnGround() {
        super("on_ground");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        LivingEntity entity = context == null ? null : context.livingEntity(condition.getSection().getString("target"), EntitySelector.TARGET);
        return entity != null && entity.isOnGround() == condition.getSection().getBoolean("value", true);
    }
}
