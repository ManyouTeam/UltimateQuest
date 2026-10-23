package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.managers.MatchItemManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Item;
import org.bukkit.event.player.PlayerFishEvent;

public final class EventConditionCatchMatchItem extends AbstractEventCondition {

    public EventConditionCatchMatchItem() {
        super("catch_match_item");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null || !(context.event() instanceof PlayerFishEvent event) || !(event.getCaught() instanceof Item item)) {
            return false;
        }
        ConfigurationSection section = condition.getSection();
        ConfigurationSection rules = section.getConfigurationSection("match");
        ConfigurationSection actualRules = rules == null ? section : rules;
        return MatchItemManager.matchItemManager.getMatch(actualRules, event.getPlayer(), item.getItemStack(), context);
    }
}
