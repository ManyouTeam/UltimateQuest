package cn.superiormc.ultimatequest.objects.matchitem;

import cn.superiormc.ultimatequest.objects.ObjectSingleMatchItem;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionType;

public final class PotionTypeRule extends AbstractMatchItemRule {

    @Override
    public boolean getMatch(ObjectSingleMatchItem match) {
        if (!(match.getItemMeta() instanceof PotionMeta potionMeta)) {
            return false;
        }
        PotionType type = potionMeta.getBasePotionType();
        String name = type == null ? "" : type.name();
        ConfigurationSection section = match.getSection();
        Set<String> included = CommonUtil.values(section, "potion-types", "potion-type");
        Set<String> excluded = CommonUtil.values(section, "excluded-potion-types", "excluded-potion-type");
        return (included.isEmpty() || included.contains(name)) && !excluded.contains(name);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return CommonUtil.values(section, "potion-types", "potion-type").isEmpty() && CommonUtil.values(section, "excluded-potion-types", "excluded-potion-type").isEmpty();
    }
}
