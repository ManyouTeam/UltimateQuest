package cn.superiormc.ultimatequest.objects.triggers;

import io.papermc.paper.event.entity.EntityAttemptSmashAttackEvent;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;

public final class AttemptSmashAttackTrigger extends EventTrigger<EntityAttemptSmashAttackEvent> {

    public AttemptSmashAttackTrigger() {
        super("attempt_smash_attack", "on-attempt-smash-attack",
                EntityAttemptSmashAttackEvent.class);
    }

    @Override
    protected void handle(EntityAttemptSmashAttackEvent event, TriggerRuntime runtime) {
        if (event.getResult() == Event.Result.DENY) {
            return;
        }
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        runtime.fire(this, runtime.base(player, event)
                .source(player)

                .target(event.getTarget())
                .location(event.getTarget().getLocation())
                .triggerItem(event.getWeapon(), EquipmentSlot.HAND));
    }
}
