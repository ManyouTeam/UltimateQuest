package cn.superiormc.ultimatequest.objects.triggers;

import java.util.List;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import io.papermc.paper.event.player.PlayerPurchaseEvent;
import org.bukkit.entity.Entity;
import org.bukkit.inventory.EquipmentSlot;

public final class PurchaseTrigger extends EventTrigger<PlayerPurchaseEvent> {

    public PurchaseTrigger() {
        super("purchase", "on-purchase", PlayerPurchaseEvent.class);
    }

    @Override
    protected void handle(PlayerPurchaseEvent event, TriggerRuntime runtime) {
        TriggerData.Builder data = runtime.base(event.getPlayer(), event)
                .outputItems(List.of(event.getTrade().getResult()))
                .triggerItem(event.getTrade().getResult(), EquipmentSlot.HAND);
        if (event.getMerchant() instanceof Entity merchant) {
            data.target(merchant).location(merchant.getLocation());
        }
        runtime.fire(this, data);
    }
}
