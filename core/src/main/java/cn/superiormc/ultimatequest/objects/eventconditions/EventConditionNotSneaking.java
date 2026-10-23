package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Player;

public final class EventConditionNotSneaking extends AbstractEventCondition {

    public EventConditionNotSneaking() {
        super("not_sneaking");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Player player = context == null ? null : context.player(condition.getSection().getString("target"), EntitySelector.TARGET);
        return player != null && !player.isSneaking();
    }
}
