package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AddLoreFirst extends AbstractChangesRule {

    public AddLoreFirst() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ItemMeta meta = singleChange.getItemMeta();
        List<String> itemLore = singleChange.getStringList("add-lore-first");
        if (meta.hasLore()) {
            itemLore.addAll(UltimateQuest.methodUtil.getItemLore(meta));
        }
        UltimateQuest.methodUtil.setItemLore(meta, itemLore, singleChange.getPlayer());
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getStringList("add-lore-first").isEmpty();
    }
}
