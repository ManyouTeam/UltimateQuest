package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import java.util.Set;

public final class EventConditionPotionEffectCause extends AbstractEventCondition {

    public EventConditionPotionEffectCause() {
        super("potion_effect_cause");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> causes = CommonUtil.values(condition.getSection(), "causes", "cause");
        return !causes.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof EntityPotionEffectEvent event && causes.contains(event.getCause().name());
    }
}
