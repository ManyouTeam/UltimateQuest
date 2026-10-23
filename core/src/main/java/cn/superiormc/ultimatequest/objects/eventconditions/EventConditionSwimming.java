package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Player;

public final class EventConditionSwimming extends AbstractEventCondition {

    public EventConditionSwimming() {
        super("swimming");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Player player = context == null ? null : context.player(condition.getSection().getString("target"), EntitySelector.TARGET);
        return player != null && player.isSwimming() == condition.getSection().getBoolean("value", true);
    }
}
