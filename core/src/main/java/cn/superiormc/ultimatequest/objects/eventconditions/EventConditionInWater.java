package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Entity;

public final class EventConditionInWater extends AbstractEventCondition {

    public EventConditionInWater() {
        super("in_water");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        return entity != null && entity.isInWater() == condition.getSection().getBoolean("value", true);
    }
}
