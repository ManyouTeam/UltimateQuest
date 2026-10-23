package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Locale;
import java.util.Set;
import org.bukkit.Location;

public final class EventConditionWorld extends AbstractEventCondition {

    public EventConditionWorld() {
        super("world");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Location location = condition.getContext() == null ? null : condition.getContext().location();
        Set<String> worlds = CommonUtil.values(condition.getSection(), "worlds", "world");
        return !worlds.isEmpty() && location != null && location.getWorld() != null && worlds.contains(location.getWorld().getName().toUpperCase(Locale.ROOT));
    }
}
