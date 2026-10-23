package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.objects.ObjectSingleMatchItem;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.matchitem.AbstractMatchItemRule;
import cn.superiormc.ultimatequest.objects.matchitem.Any;
import cn.superiormc.ultimatequest.objects.matchitem.ContainsEnchants;
import cn.superiormc.ultimatequest.objects.matchitem.ContainsEnchantsAmount;
import cn.superiormc.ultimatequest.objects.matchitem.ContainsLore;
import cn.superiormc.ultimatequest.objects.matchitem.ContainsNBT;
import cn.superiormc.ultimatequest.objects.matchitem.ContainsName;
import cn.superiormc.ultimatequest.objects.matchitem.Enchantable;
import cn.superiormc.ultimatequest.objects.matchitem.HasEnchants;
import cn.superiormc.ultimatequest.objects.matchitem.HasLore;
import cn.superiormc.ultimatequest.objects.matchitem.HasName;
import cn.superiormc.ultimatequest.objects.matchitem.HasStoredEnchants;
import cn.superiormc.ultimatequest.objects.matchitem.ItemFormat;
import cn.superiormc.ultimatequest.objects.matchitem.Items;
import cn.superiormc.ultimatequest.objects.matchitem.Material;
import cn.superiormc.ultimatequest.objects.matchitem.MaterialTag;
import cn.superiormc.ultimatequest.objects.matchitem.NBTByte;
import cn.superiormc.ultimatequest.objects.matchitem.NBTDouble;
import cn.superiormc.ultimatequest.objects.matchitem.NBTInt;
import cn.superiormc.ultimatequest.objects.matchitem.NBTString;
import cn.superiormc.ultimatequest.objects.matchitem.None;
import cn.superiormc.ultimatequest.objects.matchitem.Not;
import cn.superiormc.ultimatequest.objects.matchitem.PotionTypeRule;
import cn.superiormc.ultimatequest.objects.matchitem.Rarity;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.Collection;
import java.util.HashSet;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public class MatchItemManager extends AbstractManager {

    public static MatchItemManager matchItemManager;

    private final Collection<AbstractMatchItemRule> rules = new HashSet<>();

    public MatchItemManager() {
        matchItemManager = this;
        registerBuiltIns();
    }

    private void registerBuiltIns() {
        registerNewRule(new ContainsLore());
        registerNewRule(new ContainsName());
        registerNewRule(new HasEnchants());
        registerNewRule(new HasLore());
        registerNewRule(new HasName());
        registerNewRule(new ItemFormat());
        registerNewRule(new Items());
        registerNewRule(new Not());
        registerNewRule(new Material());
        registerNewRule(new None());
        if (CommonUtil.checkPluginLoad("NBTAPI")) {
            registerNewRule(new ContainsNBT());
            registerNewRule(new NBTString());
            registerNewRule(new NBTByte());
            registerNewRule(new NBTInt());
            registerNewRule(new NBTDouble());
        }
        if (CommonUtil.getMinorVersion(20, 5)) {
            registerNewRule(new Rarity());
        }
        registerNewRule(new ContainsEnchants());
        registerNewRule(new ContainsEnchantsAmount());
        registerNewRule(new Any());
        registerNewRule(new MaterialTag());
        registerNewRule(new Enchantable());
        registerNewRule(new HasStoredEnchants());
        registerNewRule(new PotionTypeRule());
    }

    public void registerNewRule(AbstractMatchItemRule rule) {
        rules.add(rule);
    }

    public boolean getMatch(ConfigurationSection section, ItemStack item) {
        return getMatch(section, null, item, null);
    }

    public boolean getMatch(ConfigurationSection section, Player player, ItemStack item) {
        return getMatch(section, player, item, null);
    }

    public boolean getMatch(ConfigurationSection section, Player player, ItemStack item, QuestContext context) {
        if (section == null) {
            return true;
        }
        if (item == null) {
            return false;
        }
        ItemMeta meta = item.getItemMeta();
        if (meta == null) {
            return false;
        }
        return getMatch(new ObjectSingleMatchItem(section, item, meta, player, context));
    }

    public boolean getMatch(ObjectSingleMatchItem match) {
        for (AbstractMatchItemRule rule : rules) {
            if (rule.configNotContains(match.section)) {
                continue;
            }
            if (ConfigManager.configManager.getBoolean("debug")) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fChecking match item rule: " + rule.getClass().getSimpleName());
            }
            if (!rule.getMatch(match)) {
                return false;
            }
        }
        return true;
    }

    public Collection<AbstractMatchItemRule> getRules() {
        return rules;
    }
}
