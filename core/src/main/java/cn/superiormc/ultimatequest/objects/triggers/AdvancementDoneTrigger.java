package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;

import org.bukkit.event.player.PlayerAdvancementDoneEvent;

public final class AdvancementDoneTrigger extends EventTrigger<PlayerAdvancementDoneEvent> {

    public AdvancementDoneTrigger() {
        super("advancement_done", "on-advancement-done", PlayerAdvancementDoneEvent.class);
    }

    @Override
    protected void handle(PlayerAdvancementDoneEvent event, TriggerRuntime runtime) {
        runtime.fire(this, runtime.base(event.getPlayer(), event)
                .extra(BuiltinContextKeys.ADVANCEMENT_KEY, event.getAdvancement().getKey().toString()));
    }
}
