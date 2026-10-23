package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AddLoreSuffix extends AbstractChangesRule {

    public AddLoreSuffix() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ItemMeta meta = singleChange.getItemMeta();
        if (!meta.hasLore()) {
            return singleChange.getItem();
        }
        List<String> itemLore = UltimateQuest.methodUtil.getItemLore(meta);
        List<String> newLore = new ArrayList<>();
        for (String lore : itemLore) {
            if (lore.startsWith("§w")) {
                newLore.add(lore);
                continue;
            }
            newLore.add("§w" + lore + singleChange.getString("add-lore-suffix"));
        }
        UltimateQuest.methodUtil.setItemLore(meta, newLore, singleChange.getPlayer());
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getString("add-lore-suffix") == null;
    }
}
