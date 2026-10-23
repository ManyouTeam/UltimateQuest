package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.entity.LivingEntity;
import java.util.Locale;

public final class EventConditionHealth extends AbstractNumericEventCondition {

    public EventConditionHealth() {
        super("health");
    }

    @Override
    protected Double currentValue(QuestContext context, ObjectSingleCondition condition) {
        LivingEntity entity = context.livingEntity(condition.getSection().getString("target"), EntitySelector.PLAYER);
        if (entity == null) {
            return null;
        }
        String mode = condition.getSection().getString("mode", "CURRENT").toUpperCase(Locale.ROOT);
        return mode.equals("MAX") || mode.equals("MAXIMUM") ? CommonUtil.getMaxHealth(entity) : entity.getHealth();
    }
}
