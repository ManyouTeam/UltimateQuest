package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.api.registry.KeyedRegistry;
import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.conditions.AbstractCheckCondition;
import cn.superiormc.ultimatequest.objects.conditions.ConditionAny;
import cn.superiormc.ultimatequest.objects.conditions.ConditionBiome;
import cn.superiormc.ultimatequest.objects.conditions.ConditionNot;
import cn.superiormc.ultimatequest.objects.conditions.ConditionPermission;
import cn.superiormc.ultimatequest.objects.conditions.ConditionPlaceholder;
import cn.superiormc.ultimatequest.objects.conditions.ConditionWorld;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class ConditionManager<S extends AbstractConfiguredSection<C>, C> {

    private final KeyedRegistry<String, AbstractCheckCondition<S, C>> conditions = KeyedRegistry.stringTypes();

    public ConditionManager() {
        registerBuiltInConditions();
    }

    public void registerBuiltInConditions() {
        registerNewCondition("biome", new ConditionBiome<>());
        registerNewCondition("permission", new ConditionPermission<>());
        registerNewCondition("placeholder", new ConditionPlaceholder<>());
        registerNewCondition("world", new ConditionWorld<>());
        registerNewCondition("any", new ConditionAny<>((section, parent, player, context) -> matchesAny(section, player, context)));
        registerNewCondition("not", new ConditionNot<>((section, parent, player, context) -> matches(section, player, context)));
    }

    public void registerNewCondition(String conditionID, AbstractCheckCondition<S, C> condition) {
        conditions.register(normalize(conditionID), condition);
    }

    public boolean checkBoolean(S condition, Player player, C context) {
        if (condition == null || player == null) {
            return false;
        }
        AbstractCheckCondition<S, C> checkCondition = conditions.get(normalize(condition.getString("type")));
        return checkCondition != null && checkCondition.checkCondition(condition, player, context);
    }

    public boolean checkBoolean(S condition, Player player) {
        return checkBoolean(condition, player, null);
    }

    /**
     * Evaluates all named normal conditions in a section.
     */
    public boolean matches(ConfigurationSection section, Player player, C context) {
        if (section == null) {
            return true;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection single = section.getConfigurationSection(key);
            if (single == null || !single.contains("type")) {
                return false;
            }
            @SuppressWarnings("unchecked")
            S condition = (S) new ObjectSingleCondition(single, powerContext(context, player), player);
            if (!checkBoolean(condition, player, context)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Evaluates whether any named normal condition in a section matches.
     */
    public boolean matchesAny(ConfigurationSection section, Player player, C context) {
        if (section == null) {
            return true;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection single = section.getConfigurationSection(key);
            if (single == null || !single.contains("type")) {
                continue;
            }
            @SuppressWarnings("unchecked")
            S condition = (S) new ObjectSingleCondition(single, powerContext(context, player), player);
            if (checkBoolean(condition, player, context)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getConditionTypes() {
        return conditions.keys();
    }

    public boolean hasConditionType(String type) {
        return conditions.get(normalize(type)) != null;
    }

    private String normalize(String type) {
        return type == null ? "" : type.toLowerCase().replace('-', '_');
    }

    private QuestContext powerContext(C context, Player player) {
        return context instanceof QuestContext powerContext ? powerContext.withPlayer(player) : null;
    }
}
