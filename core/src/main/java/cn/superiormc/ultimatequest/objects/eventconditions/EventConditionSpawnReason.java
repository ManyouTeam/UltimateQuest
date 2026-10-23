package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.entity.Entity;
import org.bukkit.event.entity.CreatureSpawnEvent;
import java.util.Set;

public final class EventConditionSpawnReason extends AbstractEventCondition {

    public EventConditionSpawnReason() {
        super("spawn_reason");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        Set<String> reasons = CommonUtil.values(condition.getSection(), "reasons", "reason");
        if (reasons.isEmpty()) {
            return false;
        }
        Entity entity = context.entity(EntitySelector.parse(condition.getSection().getString("target"), EntitySelector.TARGET));
        if (entity == null) {
            return false;
        }
        CreatureSpawnEvent.SpawnReason spawnReason = null;
        if (context.event() instanceof CreatureSpawnEvent event && event.getEntity().getUniqueId().equals(entity.getUniqueId())) {
            spawnReason = event.getSpawnReason();
        }
        if (spawnReason == null) {
            try {
                Object value = Entity.class.getMethod("getEntitySpawnReason").invoke(entity);
                if (value instanceof CreatureSpawnEvent.SpawnReason reason) {
                    spawnReason = reason;
                }
            } catch (ReflectiveOperationException exception) {
                return false;
            }
        }
        return spawnReason != null && reasons.contains(spawnReason.name());
    }
}
