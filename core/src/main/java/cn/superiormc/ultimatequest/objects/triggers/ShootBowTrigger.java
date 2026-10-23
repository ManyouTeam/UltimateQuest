package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.entity.EntityShootBowEvent;

public final class ShootBowTrigger extends EventTrigger<EntityShootBowEvent> {

    public ShootBowTrigger() {
        super("shoot_bow", "on-shoot-bow", EntityShootBowEvent.class);
    }

    @Override
    protected void handle(EntityShootBowEvent event, TriggerRuntime runtime) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        TriggerData.Builder data = runtime.base(player, event)
                .target(event.getProjectile()).location(event.getProjectile().getLocation())
                .triggerItem(event.getBow(), event.getHand())
                .extra(BuiltinContextKeys.BOW_FORCE, event.getForce())
                .extra(BuiltinContextKeys.BOW, event.getBow())
                .extra(BuiltinContextKeys.CONSUMABLE, event.getConsumable())
                .extra(BuiltinContextKeys.HAND, event.getHand());
        if (event.getProjectile() instanceof Projectile projectile) {
            data.extra(BuiltinContextKeys.PROJECTILE, projectile);
            runtime.trackProjectile(player, projectile, event.getBow(), event.getHand());
        }
        runtime.fire(this, data);
    }
}
