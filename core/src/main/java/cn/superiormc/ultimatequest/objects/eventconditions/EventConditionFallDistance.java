package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.entity.Entity;

public final class EventConditionFallDistance extends AbstractNumericEventCondition {

    public EventConditionFallDistance() {
        super("fall_distance");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        Entity entity = context.entity(EntitySelector.parse(condition.getSection().getString("target"), EntitySelector.TARGET));
        return entity == null ? null : (double) entity.getFallDistance();
    }
}
