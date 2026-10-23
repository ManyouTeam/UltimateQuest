package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import org.bukkit.entity.Player;
import org.bukkit.entity.Trident;
import org.bukkit.event.entity.ProjectileLaunchEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class ProjectileLaunchTrigger extends EventTrigger<ProjectileLaunchEvent> {

    public ProjectileLaunchTrigger() {
        super("projectile_launch", "on-projectile-launch", ProjectileLaunchEvent.class);
    }

    @Override
    protected void handle(ProjectileLaunchEvent event, TriggerRuntime runtime) {
        if (!(event.getEntity().getShooter() instanceof Player player)) {
            return;
        }
        TriggerRuntime.TrackedProjectile tracked = runtime.trackedProjectile(event.getEntity());
        ItemStack item;
        EquipmentSlot slot;
        if (tracked != null) {
            item = tracked.item();
            slot = tracked.slot();
        } else {
            item = event.getEntity() instanceof Trident trident ? trident.getWeapon()
                    : player.getInventory().getItemInMainHand();
            slot = runtime.findSlot(player, item);
            runtime.trackProjectile(player, event.getEntity(), item, slot);
        }
        runtime.fire(this, runtime.base(player, event)
                .source(player).target(event.getEntity())
                .location(event.getEntity().getLocation())
                .triggerItem(item, slot)
                .extra(BuiltinContextKeys.PROJECTILE, event.getEntity()));
    }
}
