package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffect;

public final class PotionEffectTrigger extends EventTrigger<EntityPotionEffectEvent> {

    public PotionEffectTrigger() {
        super("potion_effect", "on-potion-effect", EntityPotionEffectEvent.class);
    }

    @Override
    protected void handle(EntityPotionEffectEvent event, TriggerRuntime runtime) {
        // 通过 EntityEvent 父类引用调用，避免绑定 EntityPotionEffectEvent 的协变
        // getEntity() 签名（部分服务端实现缺少该协变覆写，会抛 NoSuchMethodError）
        Entity entity = ((EntityEvent) event).getEntity();
        if (!(entity instanceof Player player)) {
            return;
        }
        PotionEffect effect = event.getNewEffect();
        float duration = effect == null ? 0.0F
                : effect.getDuration() == PotionEffect.INFINITE_DURATION ? 999999.0F : effect.getDuration();
        runtime.fire(this, runtime.base(player, event).target(player)
                .extra(BuiltinContextKeys.ORIGINAL_DURATION, duration));
    }
}
