package cn.superiormc.ultimatequest.gui.inv;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.commands.SubInfo;
import cn.superiormc.ultimatequest.commands.SubMenu;
import cn.superiormc.ultimatequest.gui.InvGUI;
import cn.superiormc.ultimatequest.gui.action.GUIActionContext;
import cn.superiormc.ultimatequest.gui.action.GUIPageController;
import cn.superiormc.ultimatequest.gui.action.ObjectSingleGUIAction;
import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.SeasonManager;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.buttons.ObjectButton;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.menus.ObjectMenu;
import cn.superiormc.ultimatequest.objects.menus.ObjectMenuContent;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

public final class ConfiguredMenuGUI extends InvGUI implements GUIPageController {

    private ObjectCache questSnapshot;

    private record MenuEntry(QuestDefinition definition, QuestInstance instance,
                             String kind, String stage, ConfigurationSection content) {
    }

    private record MenuContent(ConfigurationSection section, List<Integer> slots,
                               List<MenuEntry> entries) {
    }

    private static final List<String> VIEW_ORDER = List.of("ALL", "ACTIVE", "AVAILABLE", "COMPLETED");

    private static final Set<String> VIEWS = Set.of("ALL", "ACTIVE", "AVAILABLE", "COMPLETED", "CLAIMABLE", "CLAIMED", "LOCKED", "INACTIVE");

    private static final List<String> CHALLENGE_GROUP_ORDER = List.of("ALL", "DAILY", "WEEKLY", "SEASON");

    private final String menuId;

    private final ObjectMenu menu;

    private final ConfigurationSection config;

    private final String selected;

    private final boolean challenges;

    private final Map<Integer, MenuEntry> shownEntries = new LinkedHashMap<>();

    private final Map<Integer, ObjectButton> shownStaticItems = new LinkedHashMap<>();

    private int page;

    private int maximumPage;

    private boolean initialPage = true;

    private String view = "ALL";

    private String challengeGroup = "ALL";

    private String periodGroup = "all";

    private String scrollGroup = "all";

    private String category = "all";

    private boolean choosingCategory;

    private int taskPage;

    private final Map<Integer, String> shownCategories = new LinkedHashMap<>();

    private Map<String, List<Integer>> categorySlots = Map.of();

    private List<MenuEntry> categoryScopeEntries;

    private String pendingAbandonKey;

    private long pendingAbandonUntil;

    public ConfiguredMenuGUI(Player player, String menuId) {
        super(player);
        String[] parts = (menuId == null ? "main" : menuId.toLowerCase(Locale.ROOT)).split("/", 3);
        this.menuId = parts[0];
        this.selected = parts.length >= 2 ? parts[1] : null;
        this.challenges = parts.length == 3 && parts[2].equals("challenges");
        this.menu = ConfigManager.configManager.getMenuObject(this.menuId);
        this.config = menu.getConfig();
    }

    private Collection<QuestInstance> normalSnapshots() {
        return questSnapshot == null ? List.of() : questSnapshot.quests().values().stream()
                .filter(quest -> !quest.definition().growth() && quest.source().equals("normal")).toList();
    }

    @Override
    protected void constructGUI() {
        QuestManager.questManager.refreshGrowth(player);
        QuestManager.questManager.refreshPeriods(player);
        questSnapshot = CacheManager.cacheManager.snapshot(player.getUniqueId());
        categoryScopeEntries = null;
        List<String> views = viewOrder();
        if (!views.contains(view)) {
            view = views.getFirst();
            page = 0;
        }
        if (periodFilterEnabled() && !periodGroups().contains(periodGroup)) {
            periodGroup = "all";
            page = 0;
        }
        if (scrollFilterEnabled() && !scrollGroups().contains(scrollGroup)) {
            scrollGroup = "all";
            page = 0;
        }
        ConfigurationSection currentCategory = ConfigManager.configManager.getCategoryConfigurations().get(category);
        if (!Set.of("all", "uncategorized").contains(category)
                && (currentCategory == null || !currentCategory.getBoolean("enabled", true))) {
            category = "all";
            page = 0;
        }
        if (choosingCategory) {
            constructCategoryGUI();
            return;
        }
        List<String> layout = taskLayout();
        if (layout.isEmpty() || layout.size() > 6
                || layout.stream().anyMatch(row -> row.length() != 9)) {
            throw new IllegalArgumentException("Menu '" + menuId
                    + "' requires a layout containing 1-6 rows of exactly 9 characters");
        }
        Map<String, List<Integer>> layoutSlots = layoutSlots(layout);
        List<MenuContent> contents = loadContents(layoutSlots);
        maximumPage = 0;
        for (MenuContent content : contents) {
            if (!content.slots().isEmpty()) {
                maximumPage = Math.max(maximumPage,
                        Math.max(0, (content.entries().size() - 1) / content.slots().size()));
            }
        }
        page = Math.max(0, Math.min(page, maximumPage));
        if (initialPage && selected != null && menuId.equals("battlepass") && !challenges) {
            QuestInstance growth = QuestManager.questManager.growthInstance(questSnapshot, menuId, selected);
            if (growth != null) {
                int targetLevel = growth.stageStates().entrySet().stream()
                        .filter(entry -> entry.getValue().equals("AVAILABLE")
                                && (!entry.getKey().startsWith("premium/") || growth.premium(player)))
                        .mapToInt(entry -> Integer.parseInt(entry.getKey().split("/")[1])).min().orElse(growth.level() + 1);
                int capacity = contents.stream().filter(content -> !content.slots().isEmpty())
                        .mapToInt(content -> content.slots().size()).findFirst().orElse(7);
                page = Math.min(maximumPage, Math.max(0, (targetLevel - 1) / capacity));
            }
        }
        initialPage = false;
        String[] args = baseArgs();
        String titlePath = selected == null ? "catalog-title" : "detail-title";
        String configuredTitle = config.getString(titlePath, config.getString("title", "&8" + menuId));
        String updatedTitle = CommonUtil.modifyString(player, configuredTitle, args);
        Inventory previousInventory = inv;
        int size = layout.size() * 9;
        if (inv == null || inv.getSize() != size || !updatedTitle.equals(title)) {
            inv = UltimateQuest.methodUtil.createNewInv(player, size, updatedTitle, this);
        } else {
            inv.clear();
        }
        title = updatedTitle;
        shownEntries.clear();
        shownStaticItems.clear();
        renderStaticItems(args, layoutSlots);
        for (MenuContent content : contents) {
            renderContent(content);
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
        if (choosingCategory) {
            String choice = shownCategories.get(slot);
            if (choice != null) {
                category = choice;
                choosingCategory = false;
                page = 0;
                inv = null;
                openGUI();
            } else if (categorySlots.getOrDefault("B", List.of()).contains(slot)) {
                choosingCategory = false;
                page = taskPage;
                inv = null;
                openGUI();
            } else if (categorySlots.getOrDefault("P", List.of()).contains(slot) && page > 0) {
                openPage(page - 1);
            } else if (categorySlots.getOrDefault("N", List.of()).contains(slot) && page < maximumPage) {
                openPage(page + 1);
            } else if (categorySlots.getOrDefault("C", List.of()).contains(slot)) {
                player.closeInventory();
            }
            return true;
        }
        MenuEntry entry = shownEntries.get(slot);
        if (entry != null && type.isLeftClick() && awaitingAbandonConfirmation(entry)) {
            pendingAbandonKey = null;
            pendingAbandonUntil = 0L;
            constructGUI();
            return true;
        }
        ObjectButton button = entry == null ? shownStaticItems.get(slot) : menu.getContentButton(entry.content());
        if (button == null) {
            return true;
        }
        button.clickEvent(player,
                new GUIActionContext(this, inventory, type, slot, menuId,
                        entry == null ? null : entry.definition(), entry == null ? null : entry.instance(),
                        entry == null ? null : entry.kind(), entry == null ? null : entry.stage(),
                        bypassPermission));
        return true;
    }

    @Override
    public void openPage(int page) {
        this.page = Math.max(0, Math.min(page, maximumPage));
        if (choosingCategory) {
            inv = null;
            openGUI();
        } else {
            constructGUI();
        }
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
        return config;
    }

    private List<MenuContent> loadContents(Map<String, List<Integer>> layoutSlots) {
        List<MenuContent> result = new ArrayList<>();
        for (ObjectMenuContent content : menu.getContents().values()) {
            String id = content.getId();
            ConfigurationSection section = content.getConfig();
            if (section == null || !displayedInCurrentMode(section)) {
                continue;
            }
            List<Integer> slots = symbolSlots(id, layoutSlots);
            if (slots.isEmpty()) {
                continue;
            }
            result.add(new MenuContent(section, slots, questEntries(section)));
        }
        return result;
    }

    private List<String> taskLayout() {
        return menu.getLayout(selected != null, challenges);
    }

    private List<MenuEntry> questEntries(ConfigurationSection content) {
        return questEntries(content, true);
    }

    private List<MenuEntry> questEntries(ConfigurationSection content, boolean applyFilters) {
        QuestManager manager = QuestManager.questManager;
        String type = content.getString("type", "quests").toLowerCase(Locale.ROOT);
        List<MenuEntry> result = new ArrayList<>();
        if (type.equals("quests")) {
            Map<String, QuestDefinition> available = new LinkedHashMap<>();
            ConfigManager.configManager.getListedDefinitions().forEach(definition -> available.put(definition.id(), definition));
            normalSnapshots().forEach(quest -> available.put(quest.definition().id(), quest.definition()));
            for (QuestDefinition definition : available.values()) {
                QuestInstance quest = questSnapshot == null ? null : questSnapshot.quests().get(definition.id());
                if (visibleInMenu(definition, quest)) {
                    result.add(new MenuEntry(definition, quest, "normal", null, content));
                }
            }
        } else if (Set.of("period", "scroll", "challenge").contains(type)) {
            for (QuestInstance quest : manager.assignedInstances(questSnapshot, type)) {
                result.add(new MenuEntry(quest.definition(), quest, type, null, content));
            }
        } else if (Set.of("battlepass", "milestone").contains(type)) {
            if (selected == null) {
                for (QuestDefinition definition : ConfigManager.configManager.getGrowthDefinitions(type)) {
                    result.add(new MenuEntry(definition,
                            manager.growthInstance(questSnapshot, type, definition.id()), type, null, content));
                }
            } else {
                QuestInstance saved = manager.growthInstance(questSnapshot, type, selected);
                QuestDefinition definition = saved == null ? ConfigManager.configManager.getGrowthDefinition(type, selected) : saved.definition();
                if (definition != null) {
                    QuestInstance quest = manager.growthInstance(questSnapshot, type, selected);
                    Set<String> stages = new LinkedHashSet<>();
                    for (int level : definition.thresholds().keySet()) {
                        for (String track : definition.tracks()) {
                            stages.add(track + "/" + level);
                        }
                    }
                    if (quest != null) {
                        stages.addAll(quest.stageStates().keySet());
                    }
                    for (String stage : stages.stream()
                            .sorted(Comparator.comparingInt((String key) -> Integer.parseInt(key.split("/")[1]))
                                    .thenComparing(key -> key.startsWith("premium/") ? 1 : 0)).toList()) {
                        result.add(new MenuEntry(definition, quest, type, stage, content));
                    }
                }
            }
        }
        List<String> include = normalizedList(content, "include");
        List<String> exclude = normalizedList(content, "exclude");
        String track = content.getString("track", "").toLowerCase(Locale.ROOT);
        result.removeIf(entry -> !include.isEmpty() && !include.contains(entry.definition().id()));
        result.removeIf(entry -> exclude.contains(entry.definition().id()));
        if (!track.isEmpty()) {
            result.removeIf(entry -> entry.stage() == null || !entry.stage().startsWith(track + "/"));
        }
        if (type.equals("challenge") && selected != null) {
            QuestInstance pass = manager.growthInstance(questSnapshot, "battlepass", selected);
            result.removeIf(entry -> pass == null || !entry.instance().sourceId().equals(pass.definition().id())
                    || !(entry.instance().cycle().equals(pass.season())
                    || entry.instance().cycle().startsWith(pass.season() + "--")));
            if (!challengeGroup.equals("ALL")) {
                result.removeIf(entry -> !entry.definition().settings().getString("group", "season")
                        .equalsIgnoreCase(challengeGroup));
            }
        }
        if (type.equals("period") && content.getBoolean("filterable", false) && periodFilterEnabled()
                && !periodGroup.equals("all")) {
            result.removeIf(entry -> !entry.instance().sourceId().equals(periodGroup));
        }
        if (type.equals("scroll") && content.getBoolean("filterable", false) && scrollFilterEnabled()
                && !scrollGroup.equals("all")) {
            result.removeIf(entry -> !entry.definition().id().equals(scrollGroup));
        }
        List<String> fixedCategories = normalizedList(content, "categories");
        result.removeIf(entry -> !fixedCategories.isEmpty()
                && entryCategories(entry).stream().noneMatch(fixedCategories::contains));
        if (applyFilters && categoryFilterEnabled()) {
            result.removeIf(entry -> !matchesCategory(entry, category));
        }
        if (applyFilters && content.getBoolean("filterable", false)) {
            result.removeIf(entry -> !matchesView(entry));
        }
        if (content.getString("sort", "actionable").equalsIgnoreCase("actionable")
                && result.stream().noneMatch(entry -> entry.stage() != null)) {
            result.sort(Comparator.comparingInt(this::stateRank)
                    .thenComparing(entry -> entry.definition().id()));
        }
        return result;
    }

    private boolean categoryFilterEnabled() {
        return config.getBoolean("category-filter.enabled", false)
                && (selected == null || challenges || !Set.of("battlepass", "milestone").contains(menuId));
    }

    private List<String> viewOrder() {
        List<String> configured = normalizedList(config, "view-filter.values").stream()
                .map(value -> value.toUpperCase(Locale.ROOT)).filter(VIEWS::contains).distinct().toList();
        if (!configured.isEmpty()) {
            return configured;
        }
        if ("scroll".equals(menuId)) {
            return List.of("ALL", "ACTIVE", "CLAIMABLE");
        }
        if ("period".equals(menuId) || challenges) {
            return List.of("ALL", "ACTIVE", "CLAIMABLE", "CLAIMED");
        }
        return VIEW_ORDER;
    }

    private boolean periodFilterEnabled() {
        return config.getBoolean("period-filter.enabled", "period".equals(menuId));
    }

    private List<String> periodGroups() {
        Set<String> groups = new LinkedHashSet<>();
        groups.add("all");
        Map<String, YamlConfiguration> definitions = ConfigManager.configManager.getPeriodConfigurations();
        List<String> configured = normalizedList(config, "period-filter.groups");
        if (!configured.isEmpty()) {
            configured.stream().filter(definitions::containsKey).forEach(groups::add);
        } else {
            for (String id : List.of("daily", "weekly", "monthly")) {
                if (definitions.containsKey(id)) {
                    groups.add(id);
                }
            }
            definitions.keySet().stream().sorted().forEach(groups::add);
            QuestManager.questManager.assignedInstances(questSnapshot, "period").stream()
                    .map(QuestInstance::sourceId).sorted().forEach(groups::add);
        }
        return List.copyOf(groups);
    }

    private String periodGroupName() {
        if (periodGroup.equals("all")) {
            return CommonUtil.parseLang(player, "{lang:period.filter-all}");
        }
        ConfigurationSection definition = ConfigManager.configManager.getPeriodConfigurations().get(periodGroup);
        if (definition != null) {
            return CommonUtil.parseLang(player, definition.getString("name", periodGroup));
        }
        return QuestManager.questManager.assignedInstances(questSnapshot, "period").stream()
                .filter(quest -> quest.sourceId().equals(periodGroup)).findFirst()
                .map(quest -> CommonUtil.parseLang(player, quest.definition().settings().getString("source-name", periodGroup)))
                .orElse(periodGroup);
    }

    private boolean scrollFilterEnabled() {
        return config.getBoolean("scroll-filter.enabled", "scroll".equals(menuId));
    }

    private List<String> scrollGroups() {
        Set<String> groups = new LinkedHashSet<>();
        groups.add("all");
        Map<String, YamlConfiguration> definitions = ConfigManager.configManager.getScrollConfigurations();
        List<String> configured = normalizedList(config, "scroll-filter.groups");
        if (!configured.isEmpty()) {
            configured.stream().filter(definitions::containsKey).forEach(groups::add);
        } else {
            for (String id : List.of("novice", "mining", "adventure", "veteran")) {
                if (definitions.containsKey(id)) {
                    groups.add(id);
                }
            }
            definitions.keySet().stream().sorted().forEach(groups::add);
            QuestManager.questManager.assignedInstances(questSnapshot, "scroll").stream()
                    .map(quest -> quest.definition().id()).sorted().forEach(groups::add);
        }
        return List.copyOf(groups);
    }

    private String scrollGroupName() {
        if (scrollGroup.equals("all")) {
            return CommonUtil.parseLang(player, "{lang:scroll.filter-all}");
        }
        ConfigurationSection definition = ConfigManager.configManager.getScrollConfigurations().get(scrollGroup);
        if (definition != null) {
            return CommonUtil.parseLang(player, definition.getString("name", scrollGroup));
        }
        return QuestManager.questManager.assignedInstances(questSnapshot, "scroll").stream()
                .filter(quest -> quest.definition().id().equals(scrollGroup)).findFirst()
                .map(quest -> quest.definition().name(player)).orElse(scrollGroup);
    }

    private boolean visibleInMenu(QuestDefinition definition, QuestInstance quest) {
        if (definition.growth() || quest != null && !quest.source().equals("normal")) {
            return true;
        }
        QuestDefinition current = ConfigManager.configManager.getDefinitions().getOrDefault(definition.catalogKey(), definition);
        return !current.settings().getBoolean("hide-in-menu", false)
                && (quest != null || !current.settings().getBoolean("auto-hide", false)
                || QuestManager.questManager.meetsRequirements(player, current));
    }

    private List<String> entryCategories(MenuEntry entry) {
        return ConfigManager.configManager.getDisplayCategories(entry.definition(), entry.instance());
    }

    private boolean matchesCategory(MenuEntry entry, String id) {
        List<String> ids = entryCategories(entry);
        return id.equals("all") || id.equals("uncategorized") && ids.isEmpty() || ids.contains(id);
    }

    private String categoryName(String id) {
        if (Set.of("all", "uncategorized").contains(id)) {
            return CommonUtil.parseLang(player, categorySection(id).getString("name", id));
        }
        ConfigurationSection definition = ConfigManager.configManager.getCategoryConfigurations().get(id);
        return definition == null ? id : CommonUtil.parseLang(player, definition.getString("name", id));
    }

    private ConfigurationSection categorySection(String path) {
        ConfigurationSection section = config.getConfigurationSection("category-filter." + path);
        return section == null ? ConfigManager.configManager.getSection("category-menu." + path) : section;
    }

    private String categoryText(String path) {
        return CommonUtil.parseLang(player, config.getString("category-filter." + path,
                ConfigManager.configManager.getString("category-menu." + path, "")));
    }

    private List<MenuEntry> categoryEntries() {
        if (categoryScopeEntries != null) {
            return categoryScopeEntries;
        }
        ConfigurationSection contents = config.getConfigurationSection("contents");
        Map<String, MenuEntry> unique = new LinkedHashMap<>();
        Map<String, List<Integer>> slots = layoutSlots(taskLayout());
        if (contents != null) {
            for (String id : contents.getKeys(false)) {
                ConfigurationSection content = contents.getConfigurationSection(id);
                if (content == null || !displayedInCurrentMode(content) || symbolSlots(id, slots).isEmpty()) {
                    continue;
                }
                for (MenuEntry entry : questEntries(content, false)) {
                    String key = entry.instance() == null ? entry.definition().catalogKey() : entry.instance().key();
                    unique.putIfAbsent(key, entry);
                }
            }
        }
        categoryScopeEntries = List.copyOf(unique.values());
        return categoryScopeEntries;
    }

    private List<String> categoryChoices() {
        Map<String, YamlConfiguration> definitions = ConfigManager.configManager.getCategoryConfigurations();
        List<String> choices = new ArrayList<>();
        choices.add("all");
        definitions.entrySet().stream().filter(entry -> entry.getValue().getBoolean("enabled", true))
                .sorted(Comparator.comparingLong((Map.Entry<String, YamlConfiguration> entry) -> entry.getValue().getLong("order"))
                        .thenComparing(Map.Entry::getKey)).forEach(entry -> choices.add(entry.getKey()));
        choices.add("uncategorized");
        List<MenuEntry> entries = categoryEntries();
        if (config.getBoolean("category-filter.hide-empty", false)) {
            choices.removeIf(id -> !id.equals("all") && entries.stream().noneMatch(entry -> matchesCategory(entry, id)));
        }
        return choices;
    }

    private void constructCategoryGUI() {
        List<String> layout = config.getStringList("category-filter.layout");
        if (layout.isEmpty()) {
            layout = ConfigManager.configManager.config.getStringList("category-menu.layout");
        }
        if (layout.isEmpty() || layout.size() > 6 || layout.stream().anyMatch(row -> row.length() != 9)) {
            throw new IllegalArgumentException("Invalid category-filter.layout in menu " + menuId);
        }
        categorySlots = layoutSlots(layout);
        List<Integer> slots = categorySlots.getOrDefault("Q", List.of());
        if (slots.isEmpty()) {
            throw new IllegalArgumentException("category-filter.layout requires Q slots in menu " + menuId);
        }
        Map<String, YamlConfiguration> definitions = ConfigManager.configManager.getCategoryConfigurations();
        List<String> choices = categoryChoices();
        List<MenuEntry> entries = categoryEntries();
        maximumPage = (choices.size() - 1) / slots.size();
        page = Math.max(0, Math.min(page, maximumPage));
        title = CommonUtil.modifyString(player, categoryText("title"), baseArgs());
        inv = UltimateQuest.methodUtil.createNewInv(player, layout.size() * 9, title, this);
        shownCategories.clear();
        for (Map.Entry<String, List<Integer>> symbol : categorySlots.entrySet()) {
            if (symbol.getKey().equals("Q")) {
                continue;
            }
            ConfigurationSection configured = categorySection("items." + symbol.getKey());
            if (configured.getKeys(false).isEmpty()) {
                configured = categorySection("items.F");
            }
            ItemStack item = buildItem(configured, baseArgs());
            for (int slot : symbol.getValue()) {
                setItem(slot, item.clone());
            }
        }
        for (int index = 0; index < slots.size() && page * slots.size() + index < choices.size(); index++) {
            String id = choices.get(page * slots.size() + index);
            ConfigurationSection definition = definitions.containsKey(id) ? definitions.get(id) : categorySection(id);
            List<MenuEntry> matches = entries.stream().filter(entry -> matchesCategory(entry, id)).toList();
            String[] args = merge(new String[] { "category-id", id, "category-name", categoryName(id),
                    "category-description", String.join("\\n", CommonUtil.modifyList(player, definition.getStringList("description"))),
                    "category-count", String.valueOf(matches.size()),
                    "category-active", String.valueOf(matches.stream().filter(entry -> entryState(entry).equals("ACTIVE")).count()),
                    "category-claimable", String.valueOf(matches.stream().filter(entry -> Set.of("COMPLETED", "AVAILABLE").contains(entryState(entry))
                            && entry.instance() != null).count()),
                    "category-selected", categoryText(id.equals(category) ? "selected-label" : "select-label") }, baseArgs());
            ConfigurationSection template = categorySection(id.equals(category) ? "selected-item" : "item");
            int slot = slots.get(index);
            ItemStack item = buildTemplatedItem(definition.getConfigurationSection("icon"),
                    template, id.equals("all") ? Material.BOOK : Material.CHEST, args);
            ItemMeta meta = item.getItemMeta();
            if (meta != null) {
                meta.setEnchantmentGlintOverride(id.equals(category));
                item.setItemMeta(meta);
            }
            setItem(slot, item);
            shownCategories.put(slot, id);
        }
    }

    private void renderContent(MenuContent content) {
        if (content.slots().isEmpty()) {
            return;
        }
        if (content.entries().isEmpty()) {
            ConfigurationSection emptyItem = content.section().getConfigurationSection("empty-item");
            if (categoryFilterEnabled() && !category.equals("all")) {
                emptyItem = categorySection("empty-item");
            }
            if (emptyItem != null) {
                int defaultSlot = content.slots().get(content.slots().size() / 2);
                int slot = content.section().getInt("empty-slot", defaultSlot);
                if (slot >= 0 && slot < inv.getSize()) {
                    setItem(slot, buildItem(emptyItem, baseArgs()));
                }
            }
            return;
        }
        int first = page * content.slots().size();
        for (int index = 0;
             index < content.slots().size() && first + index < content.entries().size(); index++) {
            int slot = content.slots().get(index);
            if (slot < 0 || slot >= inv.getSize()) {
                continue;
            }
            MenuEntry entry = content.entries().get(first + index);
            setItem(slot, buildQuestItem(entry));
            shownEntries.put(slot, entry);
        }
    }

    private void renderStaticItems(String[] args, Map<String, List<Integer>> layoutSlots) {
        for (ObjectButton button : menu.getButtons().values()) {
            String id = button.getId();
            ConfigurationSection definition = button.getConfig();
            if (definition == null || !displayedInCurrentMode(definition)) {
                continue;
            }
            if ((definition.getBoolean("category-button", false)
                    || usesMenuAction(definition, "open_categories")) && !categoryFilterEnabled()) {
                continue;
            }
            if ((definition.getBoolean("period-button", false)
                    || usesMenuAction(definition, "cycle_period_group", "set_period_group")) && !periodFilterEnabled()) {
                continue;
            }
            if ((definition.getBoolean("scroll-button", false)
                    || usesMenuAction(definition, "cycle_scroll_group", "set_scroll_group")) && !scrollFilterEnabled()) {
                continue;
            }
            boolean hidden = button.isHidden(page, maximumPage);
            ConfigurationSection displayed = button.getDisplayConfig(hidden, periodGroup, scrollGroup, view);
            if (displayed == null) {
                continue;
            }
            ItemStack item = buildItem(displayed, args);
            for (int slot : symbolSlots(id, layoutSlots)) {
                if (slot < 0 || slot >= inv.getSize()) {
                    continue;
                }
                setItem(slot, item.clone());
                if (!hidden) {
                    shownStaticItems.put(slot, button);
                }
            }
        }
    }

    private ItemStack buildQuestItem(MenuEntry entry) {
        QuestDefinition definition = entry.definition();
        QuestInstance quest = entry.instance();
        String state = entryState(entry);
        String hint = questHint(entry, state);
        boolean abandonConfirmation = awaitingAbandonConfirmation(entry);
        if (abandonConfirmation) {
            hint = LanguageManager.languageManager.getStringText(player, "menu-abandon-confirm-hint");
        }
        String stateText;
        if (definition.growth() && entry.stage() != null) {
            stateText = LanguageManager.languageManager.getStringText(player, "progression.state." + state);
        } else if (definition.growth()) {
            stateText = hint;
        } else {
            stateText = LanguageManager.languageManager.getStringText(player, "quests.state." + state);
        }
        String track = "";
        String level = String.valueOf(quest == null ? 0 : quest.level());
        long required = 0L;
        BigDecimal progress = quest == null ? BigDecimal.ZERO : quest.totalAmount();
        if (definition.growth() && entry.stage() != null) {
            String[] parts = entry.stage().split("/");
            track = LanguageManager.languageManager.getStringText(player, "progression.track." + parts[0]);
            level = parts[1];
            required = quest == null ? definition.thresholds().getOrDefault(Integer.parseInt(parts[1]), 0L)
                    : quest.stageRequired(entry.stage());
        }
        List<String> objectives = definition.objectiveLines(player, quest);
        List<String> prerequisites = new ArrayList<>();
        for (String prerequisite : definition.prerequisites()) {
            QuestDefinition previous = ConfigManager.configManager.getDefinition(prerequisite);
            QuestInstance completed = questSnapshot == null ? null : questSnapshot.quests().get(prerequisite);
            String template = ConfigManager.configManager.getString("task-display."
                    + (completed != null && completed.state().equals("CLAIMED") ? "prerequisite-complete" : "prerequisite-pending"), "{name}");
            prerequisites.add(CommonUtil.modifyString(player, template,
                    "name", previous == null ? prerequisite : previous.name(player)));
        }
        String prerequisiteText = prerequisites.isEmpty()
                ? CommonUtil.parseLang(player, ConfigManager.configManager.getString("task-display.no-prerequisites", ""))
                : String.join(CommonUtil.parseLang(player, ConfigManager.configManager.getString("task-display.prerequisite-separator", " / ")), prerequisites);
        if (!definition.growth()) {
            progress = BigDecimal.ZERO;
            required = 0L;
            for (QuestObjective objective : definition.objectives()) {
                BigDecimal current = quest == null ? BigDecimal.ZERO : quest.progressAmount(objective);
                progress = progress.add(current);
                required += objective.required();
            }
        }
        String expires = quest == null ? "" : formatRemaining(quest.expiresAt());
        ConfigurationSection rewards = quest == null ? definition.rewards() : quest.rewards();
        if (entry.stage() != null) {
            String[] stage = entry.stage().split("/");
            rewards = quest != null && !quest.stageState(entry.stage()).equals("LOCKED")
                    ? quest.stageReward(entry.stage()) : definition.stageRewards(Integer.parseInt(stage[1]), stage[0]);
        }
        String[] args = merge(new String[] {
                "id", definition.id(), "name", definition.name(player),
                "description", String.join("\\n", definition.description(player)),
                "objectives", String.join("\\n", objectives), "state", stateText, "hint", hint,
                "prerequisites", prerequisiteText,
                "level", level, "total", quest == null ? "0" : quest.totalAmount().stripTrailingZeros().toPlainString(),
                "required", String.valueOf(required), "progress", progress.stripTrailingZeros().toPlainString(),
                "progress-required", String.valueOf(required),
                "progress-percent", String.valueOf(progressPercent(progress.doubleValue(), required)),
                "progress-bar", progressBar(progress.doubleValue(), required), "track", track, "season",
                CommonUtil.parseLang(player, definition.settings().getString("season-name", quest == null
                        ? SeasonManager.seasonManager.season(definition) : quest.season())),
                "rewards", String.join("\\n", RewardManager.rewardManager.preview(player, rewards)),
                "rarity", definition.rarity(), "difficulty", definition.difficulty(),
                "points", String.valueOf(definition.getPoints()),
                "categories", String.join(", ", entryCategories(entry).stream().map(this::categoryName).toList()),
                "group", CommonUtil.parseLang(player, definition.settings().getString("group-name",
                        definition.settings().getString("group", "season"))),
                "unlock-week", String.valueOf(definition.getUnlockWeek()),
                "source", CommonUtil.parseLang(player, definition.settings().getString("source-name",
                        quest == null ? "" : quest.sourceId())), "expires", expires, "expires-in", expires }, baseArgs());
        boolean catalog = definition.growth() && entry.stage() == null;
        ConfigurationSection template = entry.content().getConfigurationSection(catalog ? "catalog-item"
                : "state-items." + (abandonConfirmation ? "ABANDON_CONFIRM" : state));
        if (template == null && !catalog) {
            template = entry.content().getConfigurationSection("item");
        }
        ConfigurationSection icon = entry.stage() == null && entry.content().getBoolean("use-quest-icon", true)
                ? definition.icon() : null;
        return buildTemplatedItem(icon, template, entry.stage() == null ? Material.PAPER : Material.CHEST, args);
    }

    private ItemStack buildTemplatedItem(ConfigurationSection base, ConfigurationSection template,
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

    private ItemStack buildItem(ConfigurationSection section, String[] args) {
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

    public void executeMenuAction(ObjectSingleGUIAction singleAction, GUIActionContext context) {
        ConfigurationSection action = singleAction.getSection();
        MenuEntry entry = context.definition() == null ? null
                : new MenuEntry(context.definition(), context.instance(), context.kind(), context.stage(), null);
        String type = action.getString("type", "").toLowerCase(Locale.ROOT).replace('-', '_');
        switch (type) {
            case "open_categories" -> {
                if (categoryFilterEnabled()) {
                    taskPage = page;
                    page = 0;
                    choosingCategory = true;
                    inv = null;
                    openGUI();
                }
            }
            case "cycle_view" -> {
                List<String> views = viewOrder();
                int current = views.indexOf(view);
                view = views.get((current + 1) % views.size());
                page = 0;
                constructGUI();
            }
            case "cycle_period_group" -> {
                if (periodFilterEnabled()) {
                    List<String> groups = periodGroups();
                    int current = groups.indexOf(periodGroup);
                    periodGroup = groups.get((current + 1) % groups.size());
                    page = 0;
                    constructGUI();
                }
            }
            case "set_period_group" -> {
                String group = action.getString("group", "all").toLowerCase(Locale.ROOT);
                if (periodFilterEnabled() && periodGroups().contains(group)) {
                    periodGroup = group;
                    page = 0;
                    constructGUI();
                }
            }
            case "cycle_scroll_group" -> {
                if (scrollFilterEnabled()) {
                    List<String> groups = scrollGroups();
                    int current = groups.indexOf(scrollGroup);
                    scrollGroup = groups.get((current + 1) % groups.size());
                    page = 0;
                    constructGUI();
                }
            }
            case "set_scroll_group" -> {
                String group = action.getString("group", "all").toLowerCase(Locale.ROOT);
                if (scrollFilterEnabled() && scrollGroups().contains(group)) {
                    scrollGroup = group;
                    page = 0;
                    constructGUI();
                }
            }
            case "cycle_challenge_group" -> {
                int current = CHALLENGE_GROUP_ORDER.indexOf(challengeGroup);
                challengeGroup = CHALLENGE_GROUP_ORDER.get((current + 1) % CHALLENGE_GROUP_ORDER.size());
                page = 0;
                constructGUI();
            }
            case "quest" -> {
                interactWithQuest(entry);
            }
            case "claim_all" -> {
                if (selected != null) {
                    QuestManager.questManager.claimAll(player, menuId, selected);
                }
            }
            case "open_challenges" -> {
                if (selected != null) {
                    SubMenu.open(player, "battlepass/" + selected + "/challenges", bypassPermission);
                }
            }
            case "quest_info" -> {
                if (entry != null) {
                    SubInfo.showInfo(player,
                            entry.instance() == null ? entry.definition().id() : entry.instance().key());
                }
            }
            case "abandon_quest" -> {
                abandonQuest(action, entry);
            }
            default -> {
            }
        }
    }

    private void interactWithQuest(MenuEntry entry) {
        if (entry == null) {
            return;
        }
        if (entry.definition().growth()) {
            if (entry.stage() == null) {
                SubMenu.open(player, entry.kind() + "/" + entry.definition().id(), bypassPermission);
                return;
            }
            if (entry.instance() == null || !QuestManager.questManager.claimableGrowth(entry.instance())) {
                LanguageManager.languageManager.sendStringText(player, "progression.inactive");
                constructGUI();
                return;
            }
            String[] parts = entry.stage().split("/");
            String pass = !entry.instance().season().isEmpty() ? entry.definition().id() + "@" + entry.instance().season()
                    : entry.definition().id();
            QuestManager.questManager.claimStage(player, entry.kind(), pass,
                    Integer.parseInt(parts[1]), parts[0]);
            constructGUI();
            return;
        }
        if (entry.instance() == null) {
            QuestManager.questManager.accept(player, entry.definition().id());
            constructGUI();
        } else if (entry.instance().state().equals("COMPLETED")) {
            QuestManager.questManager.claim(player, entry.instance().key());
            constructGUI();
        } else {
            SubInfo.showInfo(player, entry.instance().key());
        }
    }

    private void abandonQuest(ConfigurationSection action, MenuEntry entry) {
        if (entry == null || entry.instance() == null) {
            return;
        }
        if (!Set.of("ACTIVE", "COMPLETED").contains(entry.instance().state())) {
            SubInfo.showInfo(player, entry.instance().key());
            return;
        }
        if (!QuestManager.questManager.canAbandon(entry.instance())) {
            pendingAbandonKey = null;
            pendingAbandonUntil = 0L;
            QuestManager.questManager.abandon(player, entry.instance().key());
            return;
        }
        String key = entry.instance().key();
        long now = System.currentTimeMillis();
        if (action.getBoolean("confirm", true)
                && (!key.equals(pendingAbandonKey) || now > pendingAbandonUntil)) {
            pendingAbandonKey = key;
            pendingAbandonUntil = now + Math.max(1, action.getInt("confirm-seconds", 5)) * 1000L;
            LanguageManager.languageManager.sendStringText(player, "menu-abandon-confirm",
                    "name", entry.definition().name(player));
            constructGUI();
            return;
        }
        pendingAbandonKey = null;
        pendingAbandonUntil = 0L;
        QuestManager.questManager.abandon(player, key);
        constructGUI();
    }

    public void refresh() {
        constructGUI();
    }

    private String[] baseArgs() {
        QuestManager manager = QuestManager.questManager;
        QuestDefinition definition = selectedDefinition();
        QuestInstance growth = definition == null ? null
                : manager.growthInstance(questSnapshot, menuId, selected);
        long total = growth == null ? 0L : growth.total();
        int level = growth == null ? 0 : growth.level();
        long nextRequired = 0L;
        if (definition != null) {
            nextRequired = definition.thresholds().values().stream()
                    .filter(required -> required > total).findFirst()
                    .orElse(definition.thresholds().values().stream().reduce((first, second) -> second).orElse(0L));
        }
        long active = 0L;
        long claimable = 0L;
        long claimed = 0L;
        long available = 0L;
        long locked = 0L;
        long count;
        if (menuId.equals("main")) {
            Map<String, QuestDefinition> definitions = new LinkedHashMap<>();
            ConfigManager.configManager.getListedDefinitions().forEach(current -> definitions.put(current.id(), current));
            Collection<QuestInstance> instances = normalSnapshots();
            instances.forEach(current -> definitions.putIfAbsent(current.definition().id(), current.definition()));
            for (QuestDefinition current : definitions.values()) {
                QuestInstance instance = questSnapshot == null ? null : questSnapshot.quests().get(current.id());
                if (instance == null) {
                    if (manager.meetsRequirements(player, current)) {
                        available++;
                    } else {
                        locked++;
                    }
                } else if (instance.state().equals("ACTIVE")) {
                    active++;
                } else if (instance.state().equals("COMPLETED")) {
                    claimable++;
                } else if (instance.state().equals("CLAIMED")) {
                    claimed++;
                }
            }
            count = definitions.size();
        } else if (Set.of("period", "scroll").contains(menuId)) {
            List<QuestInstance> instances = manager.assignedInstances(questSnapshot, menuId);
            active = instances.stream().filter(current -> current.state().equals("ACTIVE")).count();
            claimable = instances.stream().filter(current -> current.state().equals("COMPLETED")).count();
            claimed = instances.stream().filter(current -> current.state().equals("CLAIMED")).count();
            count = instances.size();
        } else {
            count = ConfigManager.configManager.getGrowthDefinitions(menuId).size();
            if (growth != null) {
                claimable = growth.stageStates().values().stream().filter(state -> state.equals("AVAILABLE")).count();
                claimed = growth.stageStates().values().stream().filter(state -> state.equals("CLAIMED")).count();
            }
        }
        String viewLabel = LanguageManager.languageManager.getStringText(player,
                "menu-view." + view.toLowerCase(Locale.ROOT));
        if (categoryFilterEnabled() || periodFilterEnabled() || scrollFilterEnabled()) {
            List<MenuEntry> entries = categoryEntries().stream()
                    .filter(entry -> !categoryFilterEnabled() || matchesCategory(entry, category)).toList();
            count = entries.size();
            active = entries.stream().filter(entry -> entryState(entry).equals("ACTIVE")).count();
            claimable = entries.stream().filter(entry -> entryState(entry).equals("COMPLETED")).count();
            claimed = entries.stream().filter(entry -> entryState(entry).equals("CLAIMED")).count();
            available = entries.stream().filter(entry -> entryState(entry).equals("AVAILABLE")).count();
            locked = entries.stream().filter(entry -> entryState(entry).equals("LOCKED")).count();
        }
        String challengeGroupLabel = LanguageManager.languageManager.getStringText(player,
                "battlepass.group." + challengeGroup.toLowerCase(Locale.ROOT));
        Instant seasonEnd = definition == null ? null
                : growth == null ? SeasonManager.seasonManager.window(definition).endsAt() : growth.endsAt();
        return new String[] {
                "menu", menuId, "player", player.getName(), "challenge-group", challengeGroupLabel,
                "period-id", periodGroup, "period-group", periodFilterEnabled() ? periodGroupName() : "",
                "scroll-id", scrollGroup, "scroll-group", scrollFilterEnabled() ? scrollGroupName() : "",
                "category-id", category, "category-name", categoryName(category),
                "name", definition == null ? "" : definition.name(player),
                "objectives", definition == null ? "" : String.join("\\n", definition.objectiveLines(player, growth)),
                "season", definition == null ? "" : CommonUtil.parseLang(player,
                        definition.settings().getString("season-name", growth == null
                                ? SeasonManager.seasonManager.season(definition) : growth.season())),
                "page", String.valueOf(page + 1), "pages", String.valueOf(maximumPage + 1),
                "view", viewLabel, "count", String.valueOf(count),
                "active", String.valueOf(active), "claimable", String.valueOf(claimable),
                "claimed", String.valueOf(claimed), "available", String.valueOf(available),
                "locked", String.valueOf(locked), "level", String.valueOf(level),
                "total", growth == null ? "0" : growth.totalAmount().stripTrailingZeros().toPlainString(),
                "next-required", String.valueOf(nextRequired),
                "remaining-xp", BigDecimal.valueOf(nextRequired)
                        .subtract(growth == null ? BigDecimal.ZERO : growth.totalAmount())
                        .max(BigDecimal.ZERO).stripTrailingZeros().toPlainString(),
                "season-time", seasonEnd == null
                        ? LanguageManager.languageManager.getStringText(player, "menu-time.unlimited")
                        : formatRemaining(seasonEnd.toEpochMilli()),
                "progress-bar", progressBar(growth == null ? 0D : growth.totalAmount().doubleValue(), nextRequired)
        };
    }

    private QuestDefinition selectedDefinition() {
        if (selected == null || !Set.of("battlepass", "milestone").contains(menuId)) {
            return null;
        }
        QuestInstance growth = QuestManager.questManager.growthInstance(questSnapshot, menuId, selected);
        return growth == null ? ConfigManager.configManager.getGrowthDefinition(menuId, selected) : growth.definition();
    }

    private String entryState(MenuEntry entry) {
        QuestDefinition definition = entry.definition();
        QuestInstance quest = entry.instance();
        if (!definition.growth()) {
            if (quest != null) {
                return quest.state();
            }
            return QuestManager.questManager.meetsRequirements(player, definition) ? "AVAILABLE" : "LOCKED";
        }
        if (entry.stage() == null) {
            boolean active = quest == null ? !definition.settings().getBoolean("paused", false)
                    && SeasonManager.seasonManager.window(definition).active(Instant.now()) : quest.active(Instant.now());
            return active ? "ACTIVE" : "INACTIVE";
        }
        String state = quest == null ? "LOCKED" : quest.stageState(entry.stage());
        if (quest == null || !QuestManager.questManager.claimableGrowth(quest)) {
            return "INACTIVE";
        }
        if (state.equals("AVAILABLE") && entry.stage().startsWith("premium/") && !quest.premium(player)) {
            return "PREMIUM_REQUIRED";
        }
        return state;
    }

    private String questHint(MenuEntry entry, String state) {
        if (entry.instance() == null && !entry.definition().playerAcceptable() && !entry.definition().growth()) {
            return CommonUtil.parseLang(player, ConfigManager.configManager.getString("task-display.admin-only", ""));
        }
        if (entry.definition().growth()) {
            if (entry.stage() == null) {
                return LanguageManager.languageManager.getStringText(player,
                        state.equals("INACTIVE") ? "progression.inactive" : "progression.open");
            }
            return LanguageManager.languageManager.getStringText(player, "progression.state." + state);
        }
        String key = switch (state) {
            case "AVAILABLE" -> "menu-accept";
            case "COMPLETED" -> "menu-claim";
            case "ACTIVE" -> QuestManager.questManager.canAbandon(entry.instance()) ? "menu-active" : "menu-assigned-active";
            case "LOCKED" -> "menu-locked";
            default -> "menu-unavailable";
        };
        return LanguageManager.languageManager.getStringText(player, key);
    }

    private int stateRank(MenuEntry entry) {
        return switch (entryState(entry)) {
            case "COMPLETED" -> 0;
            case "ACTIVE" -> 1;
            case "AVAILABLE" -> 2;
            case "LOCKED", "PREMIUM_REQUIRED" -> 3;
            case "CLAIMING" -> 4;
            case "REVIEW" -> 5;
            case "CLAIMED" -> 6;
            case "INACTIVE" -> 7;
            default -> 8;
        };
    }

    private boolean matchesView(MenuEntry entry) {
        String state = entryState(entry);
        return switch (view) {
            case "ACTIVE" -> state.equals("ACTIVE");
            case "AVAILABLE" -> Set.of("AVAILABLE", "LOCKED").contains(state) && entry.instance() == null;
            case "COMPLETED" -> Set.of("COMPLETED", "CLAIMING", "CLAIMED", "REVIEW").contains(state);
            case "CLAIMABLE" -> state.equals("COMPLETED") || state.equals("AVAILABLE") && entry.stage() != null;
            case "CLAIMED" -> state.equals("CLAIMED");
            case "LOCKED" -> Set.of("LOCKED", "PREMIUM_REQUIRED").contains(state);
            case "INACTIVE" -> state.equals("INACTIVE");
            default -> true;
        };
    }

    private boolean awaitingAbandonConfirmation(MenuEntry entry) {
        return QuestManager.questManager.canAbandon(entry.instance()) && entry.instance().key().equals(pendingAbandonKey)
                && System.currentTimeMillis() <= pendingAbandonUntil;
    }

    private boolean displayedInCurrentMode(ConfigurationSection section) {
        String display = section.getString("display", "always").toLowerCase(Locale.ROOT);
        return display.equals("always") || display.equals("catalog") && selected == null
                || display.equals("detail") && selected != null && !challenges
                || display.equals("challenges") && challenges;
    }

    private boolean usesMenuAction(ConfigurationSection section, String... types) {
        Set<String> expected = Set.of(types);
        for (Map.Entry<String, Object> entry : section.getValues(true).entrySet()) {
            String path = entry.getKey();
            if ((path.startsWith("actions.") || path.startsWith("click-actions.")) && path.endsWith(".type")
                    && entry.getValue() instanceof String type
                    && expected.contains(type.toLowerCase(Locale.ROOT).replace('-', '_'))) {
                return true;
            }
        }
        return false;
    }

    private int progressPercent(double current, long required) {
        if (required <= 0L) {
            return 0;
        }
        if (current >= required) {
            return 100;
        }
        return (int) Math.min(100D, (double) current * 100D / (double) required);
    }

    private String progressBar(double current, long required) {
        ConfigurationSection settings = ConfigManager.configManager.getMenuSettings();
        int length = Math.max(1, settings.getInt("progress-bar.length", 12));
        int filled = required <= 0L ? 0
                : (int) Math.min(length, (double) current * (double) length / (double) required);
        if (required > 0L && current >= required) {
            filled = length;
        }
        String complete = CommonUtil.parseLang(player,
                settings.getString("progress-bar.complete", "&a■"));
        String remaining = CommonUtil.parseLang(player,
                settings.getString("progress-bar.remaining", "&8■"));
        String suffix = CommonUtil.parseLang(player,
                settings.getString("progress-bar.suffix", " &7{percent}%"));
        return complete.repeat(filled) + remaining.repeat(length - filled)
                + suffix.replace("{percent}", String.valueOf(progressPercent(current, required)));
    }

    private String formatRemaining(long expiresAt) {
        if (expiresAt <= 0L) {
            return LanguageManager.languageManager.getStringText(player, "menu-time.unlimited");
        }
        Duration remaining = Duration.between(Instant.now(), Instant.ofEpochMilli(expiresAt));
        if (remaining.isNegative() || remaining.isZero()) {
            return LanguageManager.languageManager.getStringText(player, "menu-time.expired");
        }
        long minutes = Math.max(1L, remaining.toMinutes());
        long days = minutes / 1440L;
        long hours = minutes % 1440L / 60L;
        long restMinutes = minutes % 60L;
        if (days > 0L) {
            return LanguageManager.languageManager.getStringText(player, "menu-time.days", "",
                    "days", String.valueOf(days), "hours", String.valueOf(hours));
        }
        if (hours > 0L) {
            return LanguageManager.languageManager.getStringText(player, "menu-time.hours", "",
                    "hours", String.valueOf(hours), "minutes", String.valueOf(restMinutes));
        }
        return LanguageManager.languageManager.getStringText(player, "menu-time.minutes", "",
                "minutes", String.valueOf(restMinutes));
    }

    private Map<String, List<Integer>> layoutSlots(List<String> layout) {
        return menu.layoutSlots(layout);
    }

    private List<Integer> symbolSlots(String id, Map<String, List<Integer>> layoutSlots) {
        return menu.symbolSlots(id, layoutSlots);
    }

    private List<String> normalizedList(ConfigurationSection section, String path) {
        return section.getStringList(path).stream()
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
    }
}
