package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.FoodLevelChangeEvent;

public final class EventConditionFoodChange extends AbstractNumericEventCondition {

    public EventConditionFoodChange() {
        super("food_change");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        if (!(context.event() instanceof FoodLevelChangeEvent event) || !(event.getEntity() instanceof Player player)) {
            return null;
        }
        return (double) (event.getFoodLevel() - player.getFoodLevel());
    }
}
