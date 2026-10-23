package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.player.PlayerInteractEvent;
import java.util.Set;

public final class EventConditionInteractionAction extends AbstractEventCondition {

    public EventConditionInteractionAction() {
        super("interaction_action");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> actions = CommonUtil.values(condition.getSection(), "actions", "action");
        return !actions.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof PlayerInteractEvent event && actions.contains(event.getAction().name());
    }
}
