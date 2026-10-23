package cn.superiormc.ultimatequest.objects.changes;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class ReplaceLore extends AbstractChangesRule {

    public ReplaceLore() {
        super();
    }

    @Override
    public ItemStack setChange(ObjectSingleChange singleChange) {
        ConfigurationSection tempVal1 = singleChange.getConfigurationSection("replace-lore");
        ItemMeta meta = singleChange.getItemMeta();
        if (tempVal1 == null || !meta.hasLore()) {
            return singleChange.getItem();
        }
        List<String> newLore = new ArrayList<>();
        for (String hasLore : UltimateQuest.methodUtil.getItemLore(meta)) {
            for (Map.Entry<String, Object> entry : tempVal1.getValues(true).entrySet()) {
                if (entry.getValue() instanceof ConfigurationSection) {
                    continue;
                }
                String requiredLore = TextUtil.withPAPI(singleChange.parsePlaceholder(entry.getKey()), singleChange.getPlayer());
                String replacement = entry.getValue() == null ? "" : entry.getValue().toString();
                replacement = TextUtil.withPAPI(singleChange.parsePlaceholder(replacement), singleChange.getPlayer());
                if (hasLore.contains(requiredLore)) {
                    hasLore = hasLore.replace(requiredLore, replacement);
                }
            }
            String[] tempVal2 = hasLore.split("\\\\n");
            if (tempVal2.length > 1) {
                for (String string : tempVal2) {
                    if (string.isEmpty()) {
                        continue;
                    }
                    newLore.add(string);
                }
                continue;
            }
            newLore.add(hasLore);
        }
        UltimateQuest.methodUtil.setItemLore(meta, newLore, singleChange.getPlayer());
        return singleChange.setItemMeta(meta);
    }

    @Override
    public boolean configNotContains(ConfigurationSection section) {
        return section.getConfigurationSection("replace-lore") == null;
    }
}
