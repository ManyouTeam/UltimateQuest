package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Locale;
import java.util.Set;

public final class EventConditionAdvancement extends AbstractEventCondition {

    public EventConditionAdvancement() {
        super("advancement");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || condition.getContext().triggerData() == null) {
            return false;
        }
        Set<String> advancements = CommonUtil.values(condition.getSection(), "advancements", "advancement");
        String advancement = condition.getContext().triggerData().extra(BuiltinContextKeys.ADVANCEMENT_KEY).orElse(null);
        return advancement != null && advancements.contains(advancement.toUpperCase(Locale.ROOT));
    }
}
