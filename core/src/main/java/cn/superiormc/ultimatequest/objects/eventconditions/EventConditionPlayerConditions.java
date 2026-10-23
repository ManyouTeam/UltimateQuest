package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Player;

public final class EventConditionPlayerConditions extends AbstractEventCondition {

    public EventConditionPlayerConditions() {
        super("player_conditions");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Player player = resolvePlayer(condition);
        return player != null && EventConditionsManager.eventConditions.getPlayerConditions().matches(condition.getSection().getConfigurationSection("conditions"), player, condition.getContext());
    }

    private Player resolvePlayer(ObjectSingleCondition condition) {
        if (condition.getContext() == null) {
            return null;
        }
        return condition.getContext().player(condition.getSection().getString("target"), EntitySelector.SOURCE);
    }
}
