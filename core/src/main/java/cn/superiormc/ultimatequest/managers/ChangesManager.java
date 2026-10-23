package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.ObjectSingleChange;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.changes.AbstractChangesRule;
import cn.superiormc.ultimatequest.objects.changes.AddAttributes;
import cn.superiormc.ultimatequest.objects.changes.AddDamage;
import cn.superiormc.ultimatequest.objects.changes.AddEnchants;
import cn.superiormc.ultimatequest.objects.changes.AddFlags;
import cn.superiormc.ultimatequest.objects.changes.AddLoreFirst;
import cn.superiormc.ultimatequest.objects.changes.AddLoreLast;
import cn.superiormc.ultimatequest.objects.changes.AddLorePrefix;
import cn.superiormc.ultimatequest.objects.changes.AddLoreSuffix;
import cn.superiormc.ultimatequest.objects.changes.AddNBTByte;
import cn.superiormc.ultimatequest.objects.changes.AddNBTDouble;
import cn.superiormc.ultimatequest.objects.changes.AddNBTInt;
import cn.superiormc.ultimatequest.objects.changes.AddNBTString;
import cn.superiormc.ultimatequest.objects.changes.AddNameFirst;
import cn.superiormc.ultimatequest.objects.changes.AddNameLast;
import cn.superiormc.ultimatequest.objects.changes.AddStoredEnchants;
import cn.superiormc.ultimatequest.objects.changes.AddTool;
import cn.superiormc.ultimatequest.objects.changes.Damage;
import cn.superiormc.ultimatequest.objects.changes.DeleteEnchants;
import cn.superiormc.ultimatequest.objects.changes.EditItem;
import cn.superiormc.ultimatequest.objects.changes.EditLore;
import cn.superiormc.ultimatequest.objects.changes.EditLoreLine;
import cn.superiormc.ultimatequest.objects.changes.Empty;
import cn.superiormc.ultimatequest.objects.changes.FixHideAttributes;
import cn.superiormc.ultimatequest.objects.changes.KeepDamage;
import cn.superiormc.ultimatequest.objects.changes.KeepEnchants;
import cn.superiormc.ultimatequest.objects.changes.KeepFlags;
import cn.superiormc.ultimatequest.objects.changes.KeepItemFormat;
import cn.superiormc.ultimatequest.objects.changes.KeepItemName;
import cn.superiormc.ultimatequest.objects.changes.KeepLore;
import cn.superiormc.ultimatequest.objects.changes.KeepName;
import cn.superiormc.ultimatequest.objects.changes.ModifyEnchants;
import cn.superiormc.ultimatequest.objects.changes.ParsePAPILore;
import cn.superiormc.ultimatequest.objects.changes.ParsePAPIName;
import cn.superiormc.ultimatequest.objects.changes.RandomChange;
import cn.superiormc.ultimatequest.objects.changes.RemoveAllEnchants;
import cn.superiormc.ultimatequest.objects.changes.RemoveAllLore;
import cn.superiormc.ultimatequest.objects.changes.RemoveAllStoredEnchants;
import cn.superiormc.ultimatequest.objects.changes.RemoveAttributeByName;
import cn.superiormc.ultimatequest.objects.changes.RemoveAttributeContainsName;
import cn.superiormc.ultimatequest.objects.changes.RemoveAttributes;
import cn.superiormc.ultimatequest.objects.changes.RemoveColor;
import cn.superiormc.ultimatequest.objects.changes.RemoveCustomModelData;
import cn.superiormc.ultimatequest.objects.changes.RemoveData;
import cn.superiormc.ultimatequest.objects.changes.RemoveEnchants;
import cn.superiormc.ultimatequest.objects.changes.RemoveFlags;
import cn.superiormc.ultimatequest.objects.changes.RemoveItemModel;
import cn.superiormc.ultimatequest.objects.changes.RemoveNBT;
import cn.superiormc.ultimatequest.objects.changes.RemoveName;
import cn.superiormc.ultimatequest.objects.changes.RemoveStoredEnchants;
import cn.superiormc.ultimatequest.objects.changes.RepairDamage;
import cn.superiormc.ultimatequest.objects.changes.ReplaceEnchants;
import cn.superiormc.ultimatequest.objects.changes.ReplaceItem;
import cn.superiormc.ultimatequest.objects.changes.ReplaceLore;
import cn.superiormc.ultimatequest.objects.changes.ReplaceName;
import cn.superiormc.ultimatequest.objects.changes.ReplaceRandomItem;
import cn.superiormc.ultimatequest.objects.changes.ReplaceStoredEnchants;
import cn.superiormc.ultimatequest.objects.changes.ResetData;
import cn.superiormc.ultimatequest.objects.changes.SetAmount;
import cn.superiormc.ultimatequest.objects.changes.SetColor;
import cn.superiormc.ultimatequest.objects.changes.SetCustomModelData;
import cn.superiormc.ultimatequest.objects.changes.SetItemModel;
import cn.superiormc.ultimatequest.objects.changes.SetLore;
import cn.superiormc.ultimatequest.objects.changes.SetName;
import cn.superiormc.ultimatequest.objects.changes.SetType;
import cn.superiormc.ultimatequest.objects.changes.SubChange;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public final class ChangesManager extends AbstractManager {

    public static ChangesManager changesManager;

    private final Collection<AbstractChangesRule> rules = new ArrayList<>();

    private final Map<String, AbstractChangesRule> rulesByConfigKey = new HashMap<>();

    public ChangesManager() {
        changesManager = this;
        registerBuiltIns();
    }

    private void registerBuiltIns() {
        register(new AddEnchants());
        register(new ModifyEnchants());
        register(new AddFlags());
        register(new AddLoreFirst());
        register(new AddLoreLast());
        register(new AddLorePrefix());
        register(new AddLoreSuffix());
        register(new AddNameFirst());
        register(new AddNameLast());
        register(new AddAttributes());
        register(new AddStoredEnchants());
        register(new DeleteEnchants());
        register(new Empty());
        register(new RemoveName());
        register(new RemoveAllLore());
        register(new RemoveAllEnchants());
        register(new RemoveAllStoredEnchants());
        register(new RemoveFlags());
        register(new RemoveAttributes());
        register(new RemoveAttributeByName());
        register(new RemoveAttributeContainsName());
        register(new RemoveEnchants());
        register(new RemoveStoredEnchants());
        register(new ReplaceName());
        register(new ReplaceItem());
        register(new ReplaceLore());
        register(new RemoveColor());
        register(new RemoveCustomModelData());
        register(new RemoveItemModel());
        register(new SetCustomModelData());
        register(new SetItemModel());
        register(new SetLore());
        register(new SetName());
        register(new SetType());
        register(new SetAmount());
        register(new FixHideAttributes());
        register(new SetColor());
        register(new ParsePAPIName());
        register(new ParsePAPILore());
        register(new EditItem());
        register(new AddDamage());
        register(new Damage());
        register(new RepairDamage());
        if (UltimateQuest.methodUtil.methodID().equals("paper")) {
            register(new RemoveData());
            register(new ResetData());
            register(new AddTool());
        }
        if (CommonUtil.checkPluginLoad("NBTAPI")) {
            register(new AddNBTString());
            register(new AddNBTByte());
            register(new AddNBTInt());
            register(new AddNBTDouble());
            register(new RemoveNBT());
        }
        register(new KeepEnchants());
        register(new KeepDamage());
        register(new KeepName());
        register(new KeepLore());
        register(new KeepFlags());
        if (CommonUtil.getMinorVersion(20, 5)) {
            register(new KeepItemName());
        }
        register(new KeepItemFormat());
        register(new EditLore());
        register(new EditLoreLine());
        register(new ReplaceEnchants());
        register(new ReplaceStoredEnchants());
        register(new ReplaceRandomItem());
        register(new RandomChange());
        register(new SubChange());
    }

    public void register(AbstractChangesRule rule) {
        rules.add(rule);
        rulesByConfigKey.clear();
    }

    public ItemStack setChange(ConfigurationSection section, ItemStack item, Player player, QuestContext context) {
        if (section == null || item == null || item.getType().isAir() || item.getItemMeta() == null) {
            return item;
        }
        ObjectSingleChange singleChange = new ObjectSingleChange(section, item, player, context);
        return setChange(singleChange);
    }

    public ItemStack setChange(ObjectSingleChange singleChange) {
        for (String configKey : singleChange.section.getKeys(false)) {
            AbstractChangesRule rule = getRuleByConfigKey(singleChange.section, configKey);
            if (rule == null) {
                continue;
            }
            if (ConfigManager.configManager.getBoolean("debug")) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fApply fake rule: " + rule.getClass().getSimpleName());
            }
            ItemStack result = rule.setChange(singleChange);
            if (singleChange.isRewritten()) {
                singleChange.replaceItem(result);
            }
        }
        return singleChange.getItem();
    }

    private AbstractChangesRule getRuleByConfigKey(ConfigurationSection section, String configKey) {
        AbstractChangesRule cachedRule = rulesByConfigKey.get(configKey);
        if (cachedRule != null) {
            return cachedRule;
        }
        ConfigurationSection singleKeySection = new MemoryConfiguration();
        singleKeySection.set(configKey, section.get(configKey));
        for (AbstractChangesRule rule : rules) {
            if (!rule.configNotContains(singleKeySection)) {
                rulesByConfigKey.put(configKey, rule);
                return rule;
            }
        }
        return null;
    }
}
