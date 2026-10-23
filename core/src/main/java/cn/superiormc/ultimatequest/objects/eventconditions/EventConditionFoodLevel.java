package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.entity.Player;

public final class EventConditionFoodLevel extends AbstractNumericEventCondition {

    public EventConditionFoodLevel() {
        super("food_level");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        Player player = context.player(condition.getSection().getString("target"), EntitySelector.PLAYER);
        return player == null ? null : (double) player.getFoodLevel();
    }
}
