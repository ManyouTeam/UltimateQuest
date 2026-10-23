package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ParsePAPILore extends AbstractChangesRule {

    public ParsePAPILore() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        if (!singleChange.getBoolean("parse-papi-lore")) {
            return singleChange.getItem();
        }
        ItemMeta meta = singleChange.getItemMeta();
        if (!meta.hasLore()) {
            return singleChange.getItem();
        }
        List<String> lore = UltimateQuest.methodUtil.getItemLore(meta);
        UltimateQuest.methodUtil.setItemLore(meta, singleChange.parsePlaceholder(lore), singleChange.getPlayer());
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("parse-papi-lore");
    }
}
