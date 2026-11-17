package cn.superiormc.ultimatequest.gui.inv;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.gui.action.GUIActionContext;
import cn.superiormc.ultimatequest.gui.action.GUIPageController;
import cn.superiormc.ultimatequest.gui.action.ObjectSingleGUIAction;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.objects.buttons.ObjectButton;
import cn.superiormc.ultimatequest.objects.menus.ObjectMenu;
import cn.superiormc.ultimatequest.objects.menus.ObjectMenuContent;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public final class CategoryMenuGUI extends InvGUI implements GUIPageController {

    private final ConfiguredMenuGUI source;

    private final String menuId;

    private final ObjectMenu menu;

    private final Map<Integer, ObjectButton> shownButtons = new LinkedHashMap<>();

    private final Map<Integer, String> shownCategories = new LinkedHashMap<>();

    private int page;

    private int maximumPage;

    public CategoryMenuGUI(ConfiguredMenuGUI source, String menuId) {
        super(source.getPlayer());
        this.source = source;
        this.menuId = menuId;
        YamlConfiguration config = normalizedConfig(ConfigManager.configManager.getSection("category-menu"), true);
        mergeConfig(normalizedConfig(source.getSection().getConfigurationSection("category-filter"), false), config);
        this.menu = new ObjectMenu(menuId + "/categories", config);
    }

    @Override
    protected void constructGUI() {
        source.refreshMenuData();
        List<String> layout = menu.getLayout(false, false);
        Map<String, List<Integer>> slots = menu.layoutSlots(layout);
        List<Integer> categorySlots = slots.getOrDefault("Q", List.of());
        ObjectMenuContent content = menu.getContents().get("Q");
        if (categorySlots.isEmpty() || content == null) {
            throw new IllegalArgumentException("Category menu requires Q slots and contents.Q in menu " + menuId);
        }
        List<String> choices = source.categoryChoices();
        maximumPage = Math.max(0, (choices.size() - 1) / categorySlots.size());
        page = Math.max(0, Math.min(page, maximumPage));
        String[] args = actionArgs(null);
        String updatedTitle = CommonUtil.modifyString(player, menu.getConfig().getString("title", "&8Categories"), args);
        Inventory previousInventory = inv;
        int size = layout.size() * 9;
        if (inv == null || inv.getSize() != size || !updatedTitle.equals(title)) {
            inv = UltimateQuest.methodUtil.createNewInv(player, size, updatedTitle, this);
        } else {
            inv.clear();
        }
        title = updatedTitle;
        shownButtons.clear();
        shownCategories.clear();
        for (ObjectButton button : menu.getButtons().values()) {
            boolean hidden = button.isHidden(page, maximumPage);
            ConfigurationSection display = button.getDisplayConfig(hidden, "all", "all", "ALL");
            if (display == null) {
                continue;
            }
            ItemStack item = buildItem(display, args);
            for (int slot : menu.symbolSlots(button.getId(), slots)) {
                setItem(slot, item.clone());
                if (!hidden) {
                    shownButtons.put(slot, button);
                }
            }
        }
        Map<String, YamlConfiguration> definitions = ConfigManager.configManager.getCategoryConfigurations();
        for (int index = 0; index < categorySlots.size() && page * categorySlots.size() + index < choices.size(); index++) {
            String id = choices.get(page * categorySlots.size() + index);
            ConfigurationSection definition = definitions.get(id);
            if (definition == null) {
                definition = menu.getConfig().getConfigurationSection(id);
            }
            YamlConfiguration display = new YamlConfiguration();
            display.set("material", id.equals("all") ? Material.BOOK.name() : Material.CHEST.name());
            if (definition != null) {
                ConfigurationFileUtil.copy(definition.getConfigurationSection("icon"), display);
            }
            ConfigurationFileUtil.copy(content.getConfig().getConfigurationSection("item"), display);
            if (source.isSelectedCategory(id)) {
                ConfigurationFileUtil.copy(content.getConfig().getConfigurationSection("state-items.SELECTED"), display);
            }
            int slot = categorySlots.get(index);
            setItem(slot, buildItem(display, actionArgs(id)));
            shownCategories.put(slot, id);
            shownButtons.put(slot, content.getButton());
        }
        if (previousInventory != null && previousInventory != inv) {
            SchedulerUtil.runSync(player, () -> {
                if (player.getOpenInventory().getTopInventory() == previousInventory) {
                    player.openInventory(inv);
                }
            });
        }
    }

    @Override
    public boolean clickEventHandle(Inventory inventory, ClickType type, int slot) {
        ObjectButton button = shownButtons.get(slot);
        if (button != null) {
            button.clickEvent(player, new GUIActionContext(this, inventory, type, slot, menuId,
                    null, null, "category", shownCategories.get(slot), bypassPermission));
        }
        return true;
    }

    public void executeMenuAction(ObjectSingleGUIAction action, GUIActionContext context) {
        String type = action.getString("type").toLowerCase(Locale.ROOT).replace('-', '_');
        switch (type) {
            case "select_category" -> {
                String id = action.contains("category") ? action.getString("category", player, context) : context.stage();
                if (id != null && source.categoryChoices().contains(id)) {
                    source.openTaskMenu(id);
                }
            }
            case "back" -> {
                source.openTaskMenu(null);
            }
            default -> {
            }
        }
    }

    public String[] actionArgs(String category) {
        return merge(new String[] { "page", String.valueOf(page + 1), "pages", String.valueOf(maximumPage + 1) },
                category == null ? source.baseArgs() : source.categoryArgs(category));
    }

    public void refresh() {
        constructGUI();
    }

    @Override
    public void openPage(int page) {
        this.page = Math.max(0, Math.min(page, maximumPage));
        openGUI();
    }

    @Override
    public int getCurrentPage() {
        return page;
    }

    @Override
    public int getPageCount() {
        return maximumPage + 1;
    }

    @Override
    public ConfigurationSection getSection() {
        return source.getSection();
    }

    private static YamlConfiguration normalizedConfig(ConfigurationSection source, boolean defaults) {
        YamlConfiguration config = new YamlConfiguration();
        ConfigurationFileUtil.copy(source, config);
        ConfigurationSection items = config.getConfigurationSection("items");
        if (items != null) {
            for (String id : items.getKeys(false)) {
                ConfigurationSection button = items.getConfigurationSection(id);
                if (button == null || button.isConfigurationSection("item")) {
                    continue;
                }
                YamlConfiguration item = new YamlConfiguration();
                for (String key : button.getKeys(false)) {
                    if (!Set.of("actions", "click-actions", "hidden-item", "hide-on-first-page", "hide-on-last-page").contains(key)) {
                        item.set(key, button.get(key));
                    }
                }
                for (String key : item.getKeys(false)) {
                    button.set(key, null);
                }
                ConfigurationFileUtil.copy(item, button.createSection("item"));
                String action = Map.of("P", "previous_page", "N", "next_page", "B", "back", "C", "close").get(id);
                if (action != null && !button.contains("actions")) {
                    button.set("actions.type", action);
                }
                if (Set.of("P", "N").contains(id)) {
                    String hidden = id.equals("P") ? "hide-on-first-page" : "hide-on-last-page";
                    if (!button.contains(hidden)) {
                        button.set(hidden, true);
                    }
                    if (!button.contains("hidden-item")) {
                        ConfigurationSection filler = config.getConfigurationSection("items.F.item");
                        if (filler == null) {
                            filler = config.getConfigurationSection("items.F");
                        }
                        if (filler != null) {
                            ConfigurationFileUtil.copy(filler, button.createSection("hidden-item"));
                        }
                    }
                }
            }
        }
        if (!config.isConfigurationSection("contents.Q") && (defaults || config.contains("item") || config.contains("selected-item"))) {
            ConfigurationSection content = config.createSection("contents.Q");
            content.set("type", "categories");
            ConfigurationFileUtil.copy(config.getConfigurationSection("item"), content.createSection("item"));
            ConfigurationFileUtil.copy(config.getConfigurationSection("selected-item"), content.createSection("state-items.SELECTED"));
            content.set("state-items.SELECTED.glow", true);
            content.set("actions.type", "select_category");
            config.set("item", null);
            config.set("selected-item", null);
        }
        return config;
    }

    private static void mergeConfig(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) {
                target.set(key, source.get(key));
            } else if (key.equals("actions") || target.getName().equals("click-actions") || child.getKeys(false).isEmpty()) {
                ConfigurationFileUtil.copy(child, target.createSection(key));
            } else {
                ConfigurationSection existing = target.getConfigurationSection(key);
                mergeConfig(child, existing == null ? target.createSection(key) : existing);
            }
        }
    }
}
