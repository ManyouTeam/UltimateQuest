package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class KeepEnchants extends AbstractChangesRule {

    public KeepEnchants() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ItemMeta meta = singleChange.getItemMeta();
        ItemMeta originalMeta = singleChange.getOriginalMeta();
        Map<Enchantment, Integer> enchants = originalMeta.getEnchants();
        if (singleChange.getBoolean("keep-enchants") && !enchants.isEmpty()) {
            for (Enchantment enchantment : enchants.keySet()) {
                meta.addEnchant(enchantment, enchants.get(enchantment), true);
            }
            singleChange.setItemMeta(meta);
            return singleChange.getItem();
        }
        return singleChange.getItem();
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return !section.contains("keep-enchants");
    }
}
