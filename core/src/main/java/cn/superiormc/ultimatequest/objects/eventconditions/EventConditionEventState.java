package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.event.player.PlayerFishEvent;
import java.util.Set;

public final class EventConditionEventState extends AbstractEventCondition {

    public EventConditionEventState() {
        super("event_state");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Set<String> states = CommonUtil.values(condition.getSection(), "states", "state");
        return !states.isEmpty() && condition.getContext() != null && condition.getContext().event() instanceof PlayerFishEvent event && states.contains(event.getState().name());
    }
}
