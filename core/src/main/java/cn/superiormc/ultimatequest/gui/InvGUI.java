package cn.superiormc.ultimatequest.gui;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.methods.Dupe;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

public abstract class InvGUI extends AbstractGUI implements InventoryHolder {

    protected Inventory inv;

    protected boolean bypassPermission;

    public String title;

    public InvGUI(Player player) {
        super(player);
    }

    public abstract boolean clickEventHandle(Inventory inventory, ClickType type, int slot);

    public boolean allowsPlayerInventoryClick(ClickType type) {
        return false;
    }

    public void afterClickEventHandle(ItemStack item, ItemStack currentItem, int slot) {
        return;
    }

    public void closeEventHandle(Inventory inventory) {
        return;
    }

    @Override
    public void openGUI() {
        ConfigurationSection section = getSection();
        if (section != null) {
            if (!section.getBoolean("enabled", true)) {
                TextUtil.sendMessage(player, CommonUtil.parseLang(player, "{lang:menu-disabled}"));
                return;
            }
            String permission = section.getString("permission", "").strip();
            if (!bypassPermission && !permission.isEmpty() && !player.hasPermission(permission)) {
                TextUtil.sendMessage(player, CommonUtil.parseLang(player, "{lang:menu-no-permission}"));
                return;
            }
        }
        constructGUI();
        if (inv != null) {
            SchedulerUtil.runSync(player, () -> player.openInventory(inv));
        }
    }

    public void openGUI(boolean bypass) {
        bypassPermission = bypass;
        openGUI();
    }

    @NotNull
    public Inventory getInventory() {
        return inv;
    }

    public void setItem(int slot, ItemStack item) {
        inv.setItem(slot, Dupe.markGuiDisplayItem(item));
    }

    protected final ItemStack buildTemplatedItem(ConfigurationSection base, ConfigurationSection template,
                                                 Material fallback, String[] args) {
        ItemStack item = base == null ? new ItemStack(fallback) : BuildItem.buildItemStack(player, base, args);
        if (template == null) {
            return item;
        }
        if (template.contains("material")) {
            item = BuildItem.buildItemStack(player, template, args);
        }
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            UltimateQuest.methodUtil.setItemName(meta, CommonUtil.modifyString(player,
                    template.getString("name", "&f{name}"), args), player);
            UltimateQuest.methodUtil.setItemLore(meta,
                    wrapLore(CommonUtil.modifyList(player, template.getStringList("lore"), args)), player);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(UltimateQuest.methodUtil.finalizeItemText(meta));
        }
        return item;
    }

    protected final ItemStack buildItem(ConfigurationSection section, String[] args) {
        ItemStack item = BuildItem.buildItemStack(player, section, args);
        ItemMeta meta = item.getItemMeta();
        if (meta != null && !section.getStringList("lore").isEmpty()) {
            UltimateQuest.methodUtil.setItemLore(meta,
                    wrapLore(CommonUtil.modifyList(player, section.getStringList("lore"), args)), player);
            meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES, ItemFlag.HIDE_ENCHANTS,
                    ItemFlag.HIDE_ADDITIONAL_TOOLTIP);
            item.setItemMeta(UltimateQuest.methodUtil.finalizeItemText(meta));
        }
        return item;
    }

    protected final List<String> wrapLore(List<String> configured) {
        List<String> result = new ArrayList<>();
        int wrapLength = ConfigManager.configManager.getMenuSettings().getInt(
                "lore-wrap-length", 30);
        for (String value : configured) {
            if (value.isBlank()) {
                result.add("");
                continue;
            }
            for (String line : value.split("\\\\n", -1)) {
                result.addAll(TextUtil.wrapKeepColor(line, wrapLength));
            }
        }
        return result;
    }

    protected final String[] merge(String[] first, String[] second) {
        String[] result = new String[first.length + second.length];
        System.arraycopy(first, 0, result, 0, first.length);
        System.arraycopy(second, 0, result, first.length, second.length);
        return result;
    }

    public ConfigurationSection getSection() {
        return null;
    }
}
