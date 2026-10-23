package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class SetAmount extends AbstractChangesRule {

    public SetAmount() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        singleChange.getItem().setAmount(singleChange.getInt("set-amount"));
        return singleChange.getItem();
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getString("set-amount") == null;
    }
}
