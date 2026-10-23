package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import java.util.Set;

public final class EventConditionPotionEffectAction extends AbstractEventCondition {

    public EventConditionPotionEffectAction() {
        super("potion_effect_action");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> actions = CommonUtil.values(condition.getSection(), "actions", "action");
        return !actions.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof EntityPotionEffectEvent event && actions.contains(event.getAction().name());
    }
}
