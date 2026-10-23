package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.PotionEffectSelector;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityPotionEffectEvent;
import org.bukkit.potion.PotionEffectType;
import java.util.ArrayList;
import java.util.List;

public final class EventConditionPotionEffectType extends AbstractEventCondition {

    public EventConditionPotionEffectType() {
        this("potion_effect_type");
    }

    public EventConditionPotionEffectType(String type) {
        super(type);
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null) {
            return false;
        }
        PotionEffectType currentType = effectType(condition.getContext().event());
        if (currentType == null) {
            return false;
        }
        return PotionEffectSelector.parse(configuredSelectors(condition.getSection())).matches(currentType);
    }

    private PotionEffectType effectType(Event event) {
        Object value = eventValue(event, "io.papermc.paper.event.entity.EntityEffectTickEvent", "getType");
        if (value instanceof PotionEffectType effect) {
            return effect;
        }
        if (event instanceof EntityPotionEffectEvent potionEffect) {
            return potionEffect.getModifiedType();
        }
        return null;
    }

    private List<String> configuredSelectors(ConfigurationSection section) {
        List<String> selectors = new ArrayList<>();
        selectors.addAll(section.getStringList("selectors"));
        add(selectors, section.getString("selector"));
        selectors.addAll(section.getStringList("effects"));
        add(selectors, section.getString("effect"));
        selectors.addAll(section.getStringList("types"));
        selectors.addAll(section.getStringList("categories"));
        add(selectors, section.getString("category"));
        return selectors;
    }

    private void add(List<String> selectors, String value) {
        if (value != null && !value.isBlank()) {
            selectors.add(value);
        }
    }
}
