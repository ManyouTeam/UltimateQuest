package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;

public final class PotionEffectTrigger extends EventTrigger<EntityPotionEffectEvent> {

    public PotionEffectTrigger() {
        super("potion_effect", "on-potion-effect", EntityPotionEffectEvent.class);
    }

    @Override
    protected void handle(EntityPotionEffectEvent event, TriggerRuntime runtime) {
        if (!(event.getEntity() instanceof Player player)) {
            return;
        }
        PotionEffect effect = event.getNewEffect();
        float duration = effect == null ? 0.0F
                : effect.getDuration() == PotionEffect.INFINITE_DURATION ? 999999.0F : effect.getDuration();
        runtime.fire(this, runtime.base(player, event).target(player)
                .extra(BuiltinContextKeys.ORIGINAL_DURATION, duration));
    }
}
