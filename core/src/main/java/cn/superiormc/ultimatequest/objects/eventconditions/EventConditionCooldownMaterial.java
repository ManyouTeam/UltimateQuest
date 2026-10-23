package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.Material;
import java.util.Locale;
import java.util.Set;

public final class EventConditionCooldownMaterial extends AbstractEventCondition {

    public EventConditionCooldownMaterial() {
        super("cooldown_material");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        Set<String> materials = CommonUtil.values(condition.getSection(), "materials", "material");
        if (materials.isEmpty()) {
            return false;
        }
        Object value = eventValue(context.event(), "io.papermc.paper.event.player.PlayerItemCooldownEvent", "getType");
        if (!(value instanceof Material material)) {
            return false;
        }
        String materialName = material.name();
        String materialKey = material.getKey().toString().toUpperCase(Locale.ROOT);
        return materials.contains(materialName) || materials.contains(materialKey);
    }
}
