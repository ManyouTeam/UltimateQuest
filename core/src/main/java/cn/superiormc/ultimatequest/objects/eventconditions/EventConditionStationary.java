package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Location;
import org.bukkit.entity.Entity;

public final class EventConditionStationary extends AbstractEventCondition {

    private final Map<UUID, Map.Entry<Location, long[]>> states = new ConcurrentHashMap<>();

    private volatile long lastCleanupAt;

    public EventConditionStationary() {
        super("stationary");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Entity entity = context == null ? null : context.entity(condition.getString("target"), EntitySelector.TARGET);
        if (entity == null) {
            return false;
        }
        Location now = entity.getLocation();
        long time = System.currentTimeMillis();
        if (time - lastCleanupAt >= 30_000L) {
            lastCleanupAt = time;
            states.entrySet().removeIf(entry -> time - entry.getValue().getValue()[1] >= 1_800_000L);
        }
        var old = states.get(entity.getUniqueId());
        if (old == null || !Objects.equals(old.getKey().getWorld(), now.getWorld()) || old.getKey().distanceSquared(now) > 0.01D) {
            states.put(entity.getUniqueId(), Map.entry(now.clone(), new long[] { time, time }));
            return false;
        }
        old.getValue()[1] = time;
        return time - old.getValue()[0] >= condition.getDouble("seconds", 3D, context) * 1000D;
    }

    @Override
    public void onEntityUnload(UUID entityId) {
        states.remove(entityId);
    }

    @Override
    public void onUnload() {
        states.clear();
    }
}
