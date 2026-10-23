package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Entity;

public final class EventConditionInRain extends AbstractEventCondition {

    public EventConditionInRain() {
        super("in_rain");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        if (entity == null) {
            return false;
        }
        try {
            boolean actual = (boolean) Entity.class.getMethod("isInRain").invoke(entity);
            return actual == condition.getSection().getBoolean("value", true);
        } catch (ReflectiveOperationException exception) {
            return false;
        }
    }
}
