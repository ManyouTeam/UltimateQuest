package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AddLoreLast extends AbstractChangesRule {

    public AddLoreLast() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ItemMeta meta = singleChange.getItemMeta();
        List<String> itemLore = UltimateQuest.methodUtil.getItemLore(meta);
        if (itemLore == null) {
            itemLore = new ArrayList<>();
        }
        itemLore.addAll(singleChange.getStringList("add-lore-last"));
        UltimateQuest.methodUtil.setItemLore(meta, itemLore, singleChange.getPlayer());
        ;
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getStringList("add-lore-last").isEmpty();
    }
}
