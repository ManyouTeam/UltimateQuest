package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Material;
import org.bukkit.entity.Entity;

public final class EventConditionInLava extends AbstractEventCondition {

    public EventConditionInLava() {
        super("in_lava");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getSection().getString("target"), EntitySelector.TARGET);
        return entity != null && (entity.getLocation().getBlock().getType() == Material.LAVA) == condition.getSection().getBoolean("value", true);
    }
}
