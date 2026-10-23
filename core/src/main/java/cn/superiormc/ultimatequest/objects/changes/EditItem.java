package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class EditItem extends AbstractChangesRule {

    public EditItem() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        if (singleChange.getBoolean("edit-item.replace-item", false)) {
            singleChange.setNeedRewriteItem();
        }
        return BuildItem.editItemStack(singleChange.getItem(), singleChange.getPlayer(), singleChange.getConfigurationSection("edit-item"));
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getConfigurationSection("edit-item") == null;
    }
}
