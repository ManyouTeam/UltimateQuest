package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Entity;

public final class EventConditionRiding extends AbstractEventCondition {

    public EventConditionRiding() {
        super("riding");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        boolean expected = condition.getSection().getBoolean("value", true);
        return entity != null && (entity.getVehicle() != null) == expected;
    }
}
