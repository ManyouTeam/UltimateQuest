package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import java.util.List;
import java.util.Locale;
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
        List<Object> key = getCooldownKey(condition);
        if (key == null) {
            return false;
        }
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
    protected String getResultMessage(ObjectSingleCondition condition, String path) {
        List<Object> key = getCooldownKey(condition);
        Long deadline = key == null ? null : deadlines.get(key);
        double seconds = deadline == null ? 0.0D
                : Math.max(0.0D, (deadline - System.currentTimeMillis()) / 1000.0D);
        double rounded = Math.ceil(seconds * 10.0D) / 10.0D;
        String remaining = rounded == Math.rint(rounded)
                ? Long.toString((long) rounded)
                : String.format(Locale.ROOT, "%.1f", rounded);
        return condition.getString(path, "", condition.getContext(), "remaining", remaining);
    }

    private List<Object> getCooldownKey(ObjectSingleCondition condition) {
        var context = condition.getContext();
        if (context == null || context.source() == null) {
            return null;
        }
        Entity owner = context.entity(condition.getString("owner"), EntitySelector.SOURCE);
        if (owner == null) {
            return null;
        }
        Entity target = condition.getBoolean("per-target", false) ? context.target() : null;
        var section = condition.getSection();
        return List.of(section.getRoot(), section.getCurrentPath(), owner.getUniqueId(),
                target == null ? owner.getUniqueId() : target.getUniqueId());
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
