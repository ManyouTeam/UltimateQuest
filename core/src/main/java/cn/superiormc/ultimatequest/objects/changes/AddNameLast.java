package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class AddNameLast extends AbstractChangesRule {

    public AddNameLast() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        if (ConfigManager.configManager.getBoolean("keep-name-in-anvil") && !singleChange.isPlayerInventory() && singleChange.getPlayer().getOpenInventory().getType().equals(InventoryType.ANVIL)) {
            return singleChange.getItem();
        }
        ItemMeta meta = singleChange.getItemMeta();
        String tempVal1;
        if (!meta.hasDisplayName()) {
            tempVal1 = "§f" + UltimateQuest.methodUtil.getItemName(meta) + singleChange.getString("add-name-last");
        } else {
            tempVal1 = UltimateQuest.methodUtil.getItemName(meta) + singleChange.getString("add-name-last");
        }
        UltimateQuest.methodUtil.setItemName(meta, tempVal1, singleChange.getPlayer());
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getString("add-name-last") == null;
    }
}
