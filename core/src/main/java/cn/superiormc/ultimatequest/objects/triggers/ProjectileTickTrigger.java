package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;

public final class ProjectileTickTrigger extends EventTrigger<TriggerRuntime.ProjectileTick> {

    public ProjectileTickTrigger() {
        super("projectile_tick", "on-projectile-tick", TriggerRuntime.ProjectileTick.class);
    }

    @Override
    protected void handle(TriggerRuntime.ProjectileTick event, TriggerRuntime runtime) {
        runtime.fire(this, runtime.base(event.player(), null)
                .source(event.shooter()).target(event.projectile())
                .location(event.projectile().getLocation()).block(event.projectile().getLocation().getBlock())
                .triggerItem(event.item(), event.slot()).tick(event.tick())
                .extra(BuiltinContextKeys.PROJECTILE, event.projectile()).build());
    }
}
