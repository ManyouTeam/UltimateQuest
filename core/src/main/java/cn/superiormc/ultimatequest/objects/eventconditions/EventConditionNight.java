package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.LivingEntity;

public final class EventConditionNight extends AbstractEventCondition {

    public EventConditionNight() {
        super("night");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || !(condition.getContext().source() instanceof LivingEntity entity)) {
            return false;
        }
        long time = entity.getWorld().getTime();
        boolean night = time >= 12300 && time <= 23850;
        return night == condition.getSection().getBoolean("value", true);
    }
}
