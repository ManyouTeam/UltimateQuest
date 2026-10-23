package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Monster;
import java.util.Set;
import java.util.Locale;

public final class EventConditionTargetCategory extends AbstractEventCondition {

    private static final Set<String> UNDEAD = Set.of("ZOMBIE", "HUSK", "DROWNED", "SKELETON", "STRAY", "WITHER_SKELETON", "WITHER", "ZOMBIE_VILLAGER", "ZOMBIFIED_PIGLIN", "PHANTOM", "BOGGED");

    public EventConditionTargetCategory() {
        super("target_category");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        Entity target = condition.getContext() == null ? null : condition.getContext().target();
        return switch(condition.getSection().getString("category", "ANIMAL").toUpperCase(Locale.ROOT)) {
            case "MONSTER" ->
                target instanceof Monster;
            case "ANIMAL" ->
                target instanceof Animals;
            case "UNDEAD" ->
                target != null && UNDEAD.contains(target.getType().name());
            default ->
                false;
        };
    }
}
