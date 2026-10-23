package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.managers.ChangesManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.MatchItemManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public final class SubChange extends AbstractChangesRule {

    @Override
    public ItemStack setChange(ObjectSingleChange change) {
        ConfigurationSection choices = change.getConfigurationSection("sub-change");
        if (choices == null) {
            return change.getItem();
        }
        ItemStack result = change.getItem();
        for (String key : choices.getKeys(false)) {
            ConfigurationSection choice = choices.getConfigurationSection(key);
            if (choice == null || !MatchItemManager.matchItemManager.getMatch(choice.getConfigurationSection("match-item"), change.getPlayer(), result, change.getContext()) || !EventConditionsManager.eventConditions.matches(choice.getConfigurationSection("conditions"), change.getContext())) {
                continue;
            }
            ConfigurationSection changes = choice.getConfigurationSection("changes");
            if (changes != null) {
                result = ChangesManager.changesManager.setChange(new ObjectSingleChange(changes, change));
            }
            if (change.getBoolean("sub-change-match-one", false)) {
                break;
            }
        }
        return result;
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("sub-change");
    }
}
