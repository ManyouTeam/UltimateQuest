package cn.superiormc.ultimatequest.objects.triggers;

import com.destroystokyo.paper.event.entity.PhantomPreSpawnEvent;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

public final class PhantomPreSpawnTrigger extends EventTrigger<PhantomPreSpawnEvent> {

    public PhantomPreSpawnTrigger() {
        super("phantom_pre_spawn", "on-phantom-pre-spawn", PhantomPreSpawnEvent.class);
    }

    @Override
    protected void handle(PhantomPreSpawnEvent event, TriggerRuntime runtime) {
        if (event.shouldAbortSpawn()) {
            return;
        }
        Entity spawningEntity = event.getSpawningEntity();
        if (!(spawningEntity instanceof Player player)) {
            return;
        }

        runtime.fire(this, runtime.base(player, event)
                .location(event.getSpawnLocation())
                .block(event.getSpawnLocation().getBlock()));
    }
}
