package cn.superiormc.ultimatequest.objects.matchitem;

import cn.superiormc.ultimatequest.managers.MatchItemManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleMatchItem;
import org.bukkit.configuration.ConfigurationSection;

public class Not extends AbstractMatchItemRule {

    public Not() {
        super();
    }

    @Override
    public boolean getMatch(ObjectSingleMatchItem match) {
        ObjectSingleMatchItem notMatch = match.withSection(match.section.getConfigurationSection("not"));
        for (AbstractMatchItemRule rule : MatchItemManager.matchItemManager.getRules()) {
            if (rule.configNotContains(notMatch.section)) {
                continue;
            }
            if (rule.getMatch(notMatch)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getConfigurationSection("not") == null;
    }
}
