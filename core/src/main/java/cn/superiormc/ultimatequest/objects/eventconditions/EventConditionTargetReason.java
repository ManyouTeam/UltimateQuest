package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Set;

public final class EventConditionTargetReason extends AbstractEventCondition {

    public EventConditionTargetReason() {
        super("target_reason");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || condition.getContext().triggerData() == null) {
            return false;
        }
        Set<String> reasons = CommonUtil.values(condition.getSection(), "reasons", "reason");
        return !reasons.isEmpty() && condition.getContext().triggerData().extra(BuiltinContextKeys.TARGET_REASON).map(reason -> reasons.contains(reason.name())).orElse(false);
    }
}
