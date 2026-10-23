package cn.superiormc.ultimatequest.objects.triggers;

import org.bukkit.entity.Item;
import java.util.List;
import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class FishTrigger extends EventTrigger<PlayerFishEvent> {

    public FishTrigger() {
        super("fish", "on-fish", PlayerFishEvent.class);
    }

    @Override
    protected void handle(PlayerFishEvent event, TriggerRuntime runtime) {
        EquipmentSlot hand = event.getHand() == EquipmentSlot.OFF_HAND ? EquipmentSlot.OFF_HAND : EquipmentSlot.HAND;
        ItemStack rod = hand == EquipmentSlot.OFF_HAND ? event.getPlayer().getInventory().getItemInOffHand() : event.getPlayer().getInventory().getItemInMainHand();
        runtime.fire(this, runtime.base(event.getPlayer(), event).outputItems(event.getState() == PlayerFishEvent.State.CAUGHT_FISH && event.getCaught() instanceof Item item
                ? List.of(item.getItemStack()) : List.of()).target(event.getCaught()).location(event.getHook().getLocation()).triggerItem(rod, hand).extra(BuiltinContextKeys.ORIGINAL_EXPERIENCE, event.getExpToDrop()));
    }
}
