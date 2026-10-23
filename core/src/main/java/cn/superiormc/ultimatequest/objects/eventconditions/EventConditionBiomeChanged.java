package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;
import org.bukkit.event.player.PlayerMoveEvent;

public final class EventConditionBiomeChanged extends AbstractEventCondition {

    public EventConditionBiomeChanged() {
        super("biome_changed");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || !(condition.getContext().event() instanceof PlayerMoveEvent event)) {
            return false;
        }
        Location from = event.getFrom();
        Location to = event.getTo();
        return to != null && from.getWorld().equals(to.getWorld()) && !from.getBlock().getBiome().equals(to.getBlock().getBiome());
    }
}
