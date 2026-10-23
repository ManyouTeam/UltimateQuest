package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

public final class EventConditionFirstAttackFromMonster extends AbstractEventCondition {

    private final Map<List<Object>, Event> firstEvents = new ConcurrentHashMap<>();

    public EventConditionFirstAttackFromMonster() {
        this("first_attack_from_monster");
    }

    public EventConditionFirstAttackFromMonster(String type) {
        super(type);
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        if (context == null || !(context.event() instanceof EntityDamageByEntityEvent damage) || damage.isCancelled() || !(context.source() instanceof Monster monster) || !(context.target() instanceof Player defender)) {
            return false;
        }
        var section = condition.getSection();
        List<Object> key = List.of(section.getRoot(), section.getCurrentPath(), defender.getUniqueId(), monster.getUniqueId());
        Event previous = firstEvents.putIfAbsent(key, context.event());
        return previous == null || previous == context.event();
    }

    @Override
    public void onEntityUnload(UUID entityId) {
        firstEvents.keySet().removeIf(key -> key.contains(entityId));
    }

    @Override
    public void onUnload() {
        firstEvents.clear();
    }
}
