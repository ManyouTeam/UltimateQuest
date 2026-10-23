package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;

public final class EventConditionTargetCount extends AbstractNumericEventCondition {

    public EventConditionTargetCount() {
        super("target_count");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        return context.triggerData().extra(BuiltinContextKeys.TARGET_COUNT).map(Integer::doubleValue).orElse(null);
    }
}
