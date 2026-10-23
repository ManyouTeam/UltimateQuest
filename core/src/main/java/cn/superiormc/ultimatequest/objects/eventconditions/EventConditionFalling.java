package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Entity;

public final class EventConditionFalling extends AbstractEventCondition {

    public EventConditionFalling() {
        super("falling");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        double maximumY = condition.getDouble("maximum-y", 0.0D, context);
        boolean expected = condition.getSection().getBoolean("value", true);
        boolean actual = entity != null && !entity.isOnGround() && entity.getVelocity().getY() < maximumY;
        return entity != null && actual == expected;
    }
}
