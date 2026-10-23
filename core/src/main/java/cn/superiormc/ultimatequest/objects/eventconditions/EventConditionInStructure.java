package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.generator.structure.GeneratedStructure;
import java.util.Locale;
import java.util.Set;

public final class EventConditionInStructure extends AbstractEventCondition {

    public EventConditionInStructure() {
        super("in_structure");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        Entity entity = context.entity(EntitySelector.parse(condition.getSection().getString("target"), EntitySelector.TARGET));
        Location location = entity == null ? context.location() : entity.getLocation();
        if (location == null || location.getWorld() == null) {
            return false;
        }
        Set<String> expected = CommonUtil.values(condition.getSection(), "structures", "structure");
        if (expected.isEmpty()) {
            return false;
        }
        for (GeneratedStructure generated : location.getChunk().getStructures()) {
            String key = generated.getStructure().getKey().toString().toLowerCase(Locale.ROOT);
            if (contains(expected, key) && generated.getBoundingBox().contains(location.getX(), location.getY(), location.getZ())) {
                return true;
            }
        }
        return false;
    }

    private boolean contains(Set<String> expected, String key) {
        for (String value : expected) {
            if (key.equals(value.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }
}
