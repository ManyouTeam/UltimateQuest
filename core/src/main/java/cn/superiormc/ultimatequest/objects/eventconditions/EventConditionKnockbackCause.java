package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.HashSet;
import java.util.Set;

public final class EventConditionKnockbackCause extends AbstractEventCondition {

    public EventConditionKnockbackCause() {
        this("knockback_cause");
    }

    public EventConditionKnockbackCause(String type) {
        super(type);
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null) {
            return false;
        }
        Object cause = eventValue(condition.getContext().event(), "io.papermc.paper.event.entity.EntityKnockbackEvent", "getCause");
        Set<String> causes = new HashSet<>(CommonUtil.values(condition.getSection(), "causes", "cause"));
        causes.addAll(CommonUtil.values(condition.getSection(), "reasons", "reason"));
        return cause instanceof Enum<?> value && causes.contains(value.name());
    }
}
