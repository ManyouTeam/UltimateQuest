package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class ReplaceItem extends AbstractChangesRule {

    public ReplaceItem() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ConfigurationSection itemSection = singleChange.getConfigurationSection("replace-item");
        ItemStack result = BuildItem.buildItemStack(singleChange.getPlayer(), itemSection);
        if (result.getType() == Material.BARRIER) {
            return singleChange.getItem();
        }
        singleChange.setNeedRewriteItem();
        return result;
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getConfigurationSection("replace-item") == null;
    }
}
