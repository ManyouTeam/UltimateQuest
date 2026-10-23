package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;

public class Empty extends AbstractChangesRule {

    public Empty() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        return singleChange.getItem();
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("empty");
    }
}
