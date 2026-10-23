package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityAirChangeEvent;

public final class EventConditionAirChange extends AbstractNumericEventCondition {

    public EventConditionAirChange() {
        super("air_change");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        if (!(context.event() instanceof EntityAirChangeEvent event) || !(event.getEntity() instanceof Player player) || context.triggerData() == null) {
            return null;
        }
        int previous = context.triggerData().extra(BuiltinContextKeys.PREVIOUS_AIR).orElse(player.getRemainingAir());
        return (double) (event.getAmount() - previous);
    }
}
