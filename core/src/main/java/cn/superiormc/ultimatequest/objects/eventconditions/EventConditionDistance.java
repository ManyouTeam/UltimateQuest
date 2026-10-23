package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.entity.Entity;

public final class EventConditionDistance extends AbstractNumericEventCondition {

    public EventConditionDistance() {
        super("distance");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        Entity source = context.entity(EntitySelector.parse(condition.getSection().getString("source"), EntitySelector.SOURCE));
        Entity target = context.entity(EntitySelector.parse(condition.getSection().getString("target"), EntitySelector.TARGET));
        if (source == null || target == null || !source.getWorld().equals(target.getWorld())) {
            return null;
        }
        return source.getLocation().distance(target.getLocation());
    }
}
