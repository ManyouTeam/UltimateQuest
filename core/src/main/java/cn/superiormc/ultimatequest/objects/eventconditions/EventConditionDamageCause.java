package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.entity.EntityDamageEvent;
import java.util.Set;

public final class EventConditionDamageCause extends AbstractEventCondition {

    public EventConditionDamageCause() {
        super("damage_cause");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> causes = CommonUtil.values(condition.getSection(), "causes", "cause");
        return !causes.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof EntityDamageEvent event && causes.contains(event.getCause().name());
    }
}
