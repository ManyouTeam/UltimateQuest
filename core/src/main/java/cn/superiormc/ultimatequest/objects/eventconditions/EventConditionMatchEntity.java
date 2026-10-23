package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.managers.MatchEntityManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

public final class EventConditionMatchEntity extends AbstractEventCondition {

    public EventConditionMatchEntity() {
        super("match_entity");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        ConfigurationSection section = condition.getSection();
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        LivingEntity living = context.livingEntity(section.getString("target"), EntitySelector.TARGET);
        if (living == null) {
            return false;
        }
        Player player = context.source() instanceof Player source ? source : context.player();
        ConfigurationSection rules = section.getConfigurationSection("match");
        return MatchEntityManager.matchEntityManager.getMatch(rules == null ? section : rules, living, player, context);
    }
}
