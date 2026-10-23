package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Set;
import org.bukkit.entity.Player;

public final class EventConditionGameMode extends AbstractEventCondition {

    public EventConditionGameMode() {
        super("game_mode");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Player player = context == null ? null : context.player(condition.getSection().getString("target"), EntitySelector.TARGET);
        Set<String> modes = CommonUtil.values(condition.getSection(), "modes", "mode");
        return player != null && modes.contains(player.getGameMode().name());
    }
}
