package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Waterlogged;
import org.bukkit.event.player.PlayerMoveEvent;
import java.util.Locale;

public final class EventConditionLiquidTransition extends AbstractEventCondition {

    public EventConditionLiquidTransition() {
        super("liquid_transition");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || !(condition.getContext().event() instanceof PlayerMoveEvent event)) {
            return false;
        }
        Location to = event.getTo();
        if (to == null || !event.getFrom().getWorld().equals(to.getWorld())) {
            return false;
        }
        String configured = configuredLiquid(condition);
        if ("INVALID".equals(configured)) {
            return false;
        }
        boolean fromMatches = matches(configured, liquidAt(event.getFrom().getBlock()));
        boolean toMatches = matches(configured, liquidAt(to.getBlock()));
        String transition = condition.getString("transition", "ENTER").strip().toUpperCase(Locale.ROOT).replace('-', '_');
        return switch(transition) {
            case "EXIT", "LEAVE" ->
                fromMatches && !toMatches;
            case "ENTER" ->
                !fromMatches && toMatches;
            default ->
                false;
        };
    }

    private boolean matches(String configured, String actual) {
        return actual != null && ("ANY".equals(configured) || configured.equals(actual));
    }

    private String configuredLiquid(ObjectSingleCondition condition) {
        String configured = condition.getString("liquid", "ANY").strip().toUpperCase(Locale.ROOT);
        return switch(configured) {
            case "ANY" ->
                "ANY";
            case "WATER" ->
                "WATER";
            case "LAVA" ->
                "LAVA";
            default ->
                "INVALID";
        };
    }

    private String liquidAt(Block block) {
        Material material = block.getType();
        if (material == Material.LAVA) {
            return "LAVA";
        }
        if (material == Material.WATER || material == Material.BUBBLE_COLUMN || material == Material.KELP || material == Material.KELP_PLANT || material == Material.SEAGRASS || material == Material.TALL_SEAGRASS || block.getBlockData() instanceof Waterlogged waterlogged && waterlogged.isWaterlogged()) {
            return "WATER";
        }
        return null;
    }
}
