package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Entity;

public final class EventConditionInAir extends AbstractEventCondition {

    public EventConditionInAir() {
        super("in_air");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        boolean expected = condition.getSection().getBoolean("value", true);
        boolean actual = entity != null && !entity.isOnGround();
        return entity != null && actual == expected;
    }
}
