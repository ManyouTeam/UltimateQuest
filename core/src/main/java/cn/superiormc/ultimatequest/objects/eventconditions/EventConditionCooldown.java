package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Entity;

public final class EventConditionCooldown extends AbstractEventCondition {

    private final Map<List<Object>, Long> deadlines = new ConcurrentHashMap<>();

    private volatile long lastCleanupAt;

    public EventConditionCooldown() {
        super("cooldown");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        if (context == null) {
            return false;
        }
        double seconds = condition.getDouble("seconds", 0.0D, context);
        if (seconds <= 0.0D || context.source() == null) {
            return true;
        }
        if (!Double.isFinite(seconds)) {
            return false;
        }
        Entity owner = context.entity(condition.getString("owner"), EntitySelector.SOURCE);
        if (owner == null) {
            return false;
        }
        Entity target = condition.getBoolean("per-target", false) ? context.target() : null;
        var section = condition.getSection();
        List<Object> key = List.of(section.getRoot(), section.getCurrentPath(), owner.getUniqueId(), target == null ? owner.getUniqueId() : target.getUniqueId());
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt >= 30_000L) {
            lastCleanupAt = now;
            deadlines.entrySet().removeIf(entry -> entry.getValue() <= now);
        }
        long duration = Math.min(Long.MAX_VALUE - now, Math.max(1L, (long) Math.ceil(seconds * 1000D)));
        Long deadline = deadlines.putIfAbsent(key, now + duration);
        return deadline == null || deadline <= now && deadlines.replace(key, deadline, now + duration);
    }

    @Override
    public void onEntityUnload(UUID entityId) {
        deadlines.keySet().removeIf(key -> key.contains(entityId));
    }

    @Override
    public void onUnload() {
        deadlines.clear();
    }
}
