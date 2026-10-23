package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class BlockBreakTrigger extends EventTrigger<BlockBreakEvent> {

    public BlockBreakTrigger() {
        super("block_break", "on-block-break", BlockBreakEvent.class);
    }

    @Override
    protected void handle(BlockBreakEvent event, TriggerRuntime runtime) {
        TriggerData.Builder data = runtime.base(event.getPlayer(), event).block(event.getBlock()).location(event.getBlock().getLocation().add(0.5D, 0.5D, 0.5D)).triggerItem(event.getPlayer().getInventory().getItemInMainHand(), EquipmentSlot.HAND).extra(BuiltinContextKeys.ORIGINAL_EXPERIENCE, event.getExpToDrop());
        runtime.fire(this, data);
    }
}
