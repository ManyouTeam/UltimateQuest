package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.entity.EntityExhaustionEvent;
import java.util.Set;

public final class EventConditionExhaustionReason extends AbstractEventCondition {

    public EventConditionExhaustionReason() {
        super("exhaustion_reason");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> reasons = CommonUtil.values(condition.getSection(), "reasons", "reason");
        return !reasons.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof EntityExhaustionEvent event && reasons.contains(event.getExhaustionReason().name());
    }
}
