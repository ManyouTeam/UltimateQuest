package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class EventConditionBestTool extends AbstractEventCondition {

    public EventConditionBestTool() {
        this("best_tool");
    }

    public EventConditionBestTool(String type) {
        super(type);
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        Block block = context == null ? null : context.block();
        if (block == null) {
            return false;
        }
        Entity entity = context.entity(condition.getSection().getString("target"), EntitySelector.SOURCE);
        if (!(entity instanceof Player player)) {
            return false;
        }
        String hand = condition.getSection().getString("hand", "MAIN_HAND");
        ItemStack tool = hand != null && hand.equalsIgnoreCase("OFF_HAND") ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        boolean expected = condition.getSection().getBoolean("value", true);
        return block.isPreferredTool(tool) == expected;
    }
}
