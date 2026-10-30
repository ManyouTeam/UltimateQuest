package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.inventory.EquipmentSlot;

public final class MeleeAttackTrigger extends EventTrigger<EntityDamageByEntityEvent> {

    public MeleeAttackTrigger() {
        super("melee_attack", "on-melee-attack", EntityDamageByEntityEvent.class);
    }

    @Override
    protected void handle(EntityDamageByEntityEvent event, TriggerRuntime runtime) {
        if (!(event.getDamager() instanceof Player player)) {
            return;
        }
        runtime.fire(this, runtime.base(player, event)
                .source(player).target(event.getEntity())
                .location(event.getEntity().getLocation())
                .triggerItem(player.getInventory().getItemInMainHand(), EquipmentSlot.HAND)
                .extra(BuiltinContextKeys.ORIGINAL_DAMAGE, event.getDamage())
                .extra(BuiltinContextKeys.FINAL_DAMAGE, event.getFinalDamage())
                .extra(BuiltinContextKeys.DAMAGE_CAUSE, event.getCause()));
    }
}
