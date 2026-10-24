package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.menus.ObjectMenu;
import cn.superiormc.ultimatequest.objects.quests.ObjectBattlePass;
import cn.superiormc.ultimatequest.objects.quests.ObjectCategory;
import cn.superiormc.ultimatequest.objects.quests.ObjectMilestone;
import cn.superiormc.ultimatequest.objects.quests.ObjectPeriod;
import cn.superiormc.ultimatequest.objects.quests.ObjectQuest;
import cn.superiormc.ultimatequest.objects.quests.ObjectQuestPool;
import cn.superiormc.ultimatequest.objects.quests.ObjectScroll;
import cn.superiormc.ultimatequest.objects.quests.ObjectTask;
import cn.superiormc.ultimatequest.objects.quests.ObjectTaskLimits;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.rewards.ObjectRewardPool;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class ConfigManager extends AbstractManager {

    public static ConfigManager configManager;

    private static final List<String> MENU_FILES = List.of("main", "battlepass", "milestone", "period", "scroll");

    public Map<String, ObjectQuest> questConfigs = new LinkedHashMap<>();

    public Map<String, ObjectCategory> categoryConfigs = new LinkedHashMap<>();

    public Map<String, ObjectTask> taskConfigs = new LinkedHashMap<>();

    public Map<String, ObjectBattlePass> battlePassConfigs = new LinkedHashMap<>();

    public Map<String, ObjectMilestone> milestoneConfigs = new LinkedHashMap<>();

    public Map<String, ObjectPeriod> periodConfigs = new LinkedHashMap<>();

    public Map<String, ObjectScroll> scrollConfigs = new LinkedHashMap<>();

    public Map<String, ObjectQuestPool> questPoolConfigs = new LinkedHashMap<>();

    public Map<String, ObjectRewardPool> rewardPoolConfigs = new LinkedHashMap<>();

    private ObjectTaskLimits taskLimits;

    public FileConfiguration config;

    private volatile YamlConfiguration menuConfig = new YamlConfiguration();

    private volatile List<String> menuIds = List.of();

    public ConfigManager() {
        configManager = this;
        UltimateQuest.instance.reloadConfig();
        config = UltimateQuest.instance.getConfig();
        loadMenus();
    }

    private void loadMenus() {
        try {
            reloadMenus();
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load menu configurations: " + exception.getMessage());
        }
    }

    public synchronized void reloadMenus() throws Exception {
        File directory = new File(UltimateQuest.instance.getDataFolder(), "menus");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create menu configuration directory: " + directory);
        }
        YamlConfiguration candidate = new YamlConfiguration();
        loadMenuFile(directory, "settings", null, candidate);
        Set<String> ids = new LinkedHashSet<>(MENU_FILES);
        File[] files = directory.listFiles((ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (!id.equals("settings")) {
                    ids.add(id);
                }
            }
        }
        for (String id : ids) {
            loadMenuFile(directory, id, id, candidate);
        }
        Map<String, ObjectMenu> menus = new LinkedHashMap<>();
        for (String id : ids) {
            menus.put(id, new ObjectMenu(id, candidate.getConfigurationSection(id)));
        }
        ObjectMenu.commonMenus = menus;
        menuConfig = candidate;
        menuIds = List.copyOf(ids);
    }

    public synchronized void reloadQuestConfigs(SeasonManager seasons) throws Exception {
        seasons.beginReload();
        initCategoryConfigs();
        initQuestConfigs();
        loadQuestConfigs();
        initQuestPoolConfigs();
        initRewardPoolConfigs();
        loadRewardPoolConfigs();
        initTaskConfigs();
        initBattlePassConfigs(seasons);
        initMilestoneConfigs(seasons);
        initPeriodConfigs();
        initScrollConfigs();
        loadTaskConfigs();
        initTaskLimits();
        seasons.commitReload();
    }

    private void initCategoryConfigs() throws Exception {
        categoryConfigs.clear();
        for (var entry : ConfigurationFileUtil.readFolder("categories", true).entrySet()) {
            categoryConfigs.put(entry.getKey(), new ObjectCategory(entry.getKey(), entry.getValue()));
        }
    }

    private void initQuestConfigs() throws Exception {
        questConfigs.clear();
        for (var entry : ConfigurationFileUtil.readFolder("quests").entrySet()) {
            questConfigs.put(entry.getKey(), new ObjectQuest(entry.getKey(), entry.getValue()));
        }
    }

    private void loadQuestConfigs() {
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> categories = getCategoryConfigurations();
        for (ObjectQuest quest : questConfigs.values()) {
            quest.initObjective(library, categories);
        }
    }

    private void initQuestPoolConfigs() throws Exception {
        questPoolConfigs.clear();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("pools").entrySet()) {
            questPoolConfigs.put(entry.getKey(), new ObjectQuestPool(entry.getKey(), entry.getValue(), library));
        }
    }

    private void initRewardPoolConfigs() throws Exception {
        rewardPoolConfigs.clear();
        for (var entry : ConfigurationFileUtil.readFolder("reward-pools").entrySet()) {
            rewardPoolConfigs.put(entry.getKey(), new ObjectRewardPool(entry.getKey(), entry.getValue()));
        }
    }

    private void loadRewardPoolConfigs() {
        Map<String, YamlConfiguration> pools = getRewardPoolConfigurations();
        for (ObjectRewardPool pool : rewardPoolConfigs.values()) {
            pool.initRewards(pools);
        }
    }

    private void initTaskConfigs() throws Exception {
        taskConfigs.clear();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("tasks").entrySet()) {
            taskConfigs.put(entry.getKey(), new ObjectTask(entry.getKey(), entry.getValue(), library, rewards));
        }
    }

    private void initBattlePassConfigs(SeasonManager seasons) throws Exception {
        battlePassConfigs.clear();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("battlepasses").entrySet()) {
            battlePassConfigs.put(entry.getKey(), new ObjectBattlePass(entry.getKey(), entry.getValue(), library, rewards, seasons));
        }
    }

    private void initMilestoneConfigs(SeasonManager seasons) throws Exception {
        milestoneConfigs.clear();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("milestones").entrySet()) {
            milestoneConfigs.put(entry.getKey(), new ObjectMilestone(entry.getKey(), entry.getValue(), library, rewards, seasons));
        }
    }

    private void initPeriodConfigs() throws Exception {
        periodConfigs.clear();
        Map<String, YamlConfiguration> pools = getPoolConfigurations();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("periods").entrySet()) {
            periodConfigs.put(entry.getKey(), new ObjectPeriod(entry.getKey(), entry.getValue(), pools, library, rewards));
        }
    }

    private void initScrollConfigs() throws Exception {
        scrollConfigs.clear();
        Map<String, YamlConfiguration> pools = getPoolConfigurations();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (var entry : ConfigurationFileUtil.readFolder("scrolls").entrySet()) {
            scrollConfigs.put(entry.getKey(), new ObjectScroll(entry.getKey(), entry.getValue(), pools, library, rewards));
        }
    }

    private void loadTaskConfigs() {
        Map<String, QuestDefinition> definitions = getDefinitions();
        Map<String, YamlConfiguration> categories = getCategoryConfigurations();
        taskConfigs.values().forEach(task -> task.initReferences(categories, definitions));
        battlePassConfigs.values().forEach(pass -> pass.initReferences(categories, definitions));
        milestoneConfigs.values().forEach(milestone -> milestone.initReferences(categories, definitions));
        periodConfigs.values().forEach(period -> period.initReferences(categories, definitions));
        scrollConfigs.values().forEach(scroll -> scroll.initReferences(categories, definitions));
    }

    private void initTaskLimits() {
        taskLimits = new ObjectTaskLimits(getSection("task-limits"), categoryConfigs.keySet());
        Object savePeriod = config.get("auto-save.period-tick", 6000);
        if (!(savePeriod instanceof Integer || savePeriod instanceof Long) || ((Number) savePeriod).longValue() < 1L) {
            throw new IllegalArgumentException("auto-save.period-tick must be a positive integer");
        }
    }

    public ObjectTaskLimits getTaskLimits() {
        return taskLimits;
    }

    public boolean getBoolean(String path) {
        return configuration(path).getBoolean(normalizePath(path), false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return configuration(path).getBoolean(normalizePath(path), defaultValue);
    }

    public String getString(String path, String... args) {
        String s = configuration(path).getString(normalizePath(path));
        if (s == null) {
            if (args.length == 0) {
                return null;
            }
            s = args[0];
        }
        for (int i = 1; i < args.length; i += 2) {
            String var = "{" + args[i] + "}";
            if (args[i + 1] == null) {
                s = s.replace(var, "");
            } else {
                s = s.replace(var, args[i + 1]);
            }
        }
        return s.replace("{plugin_folder}", String.valueOf(UltimateQuest.instance.getDataFolder()));
    }

    public int getInt(String path, int defaultValue) {
        return configuration(path).getInt(normalizePath(path), defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return configuration(path).getDouble(normalizePath(path), defaultValue);
    }

    public ConfigurationSection getSection(String path) {
        FileConfiguration selected = configuration(path);
        String normalized = normalizePath(path);
        if (selected.getConfigurationSection(normalized) == null) {
            return new MemoryConfiguration();
        }
        return selected.getConfigurationSection(normalized);
    }

    public ConfigurationSection getMenu(String id) {
        if (!hasMenu(id)) {
            return new MemoryConfiguration();
        }
        return getMenuObject(id).getConfig();
    }

    public ObjectMenu getMenuObject(String id) {
        ObjectMenu menu = ObjectMenu.commonMenus.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) {
            throw new IllegalArgumentException("Unknown menu: " + id);
        }
        return menu;
    }

    public boolean hasMenu(String id) {
        return id != null && menuIds.contains(id.toLowerCase(Locale.ROOT));
    }

    public Collection<String> getMenuIds() {
        return menuIds;
    }

    public ObjectQuest getQuest(String id) {
        ObjectQuest value = questConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown quest: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getQuestConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        questConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public ObjectCategory getCategory(String id) {
        ObjectCategory value = categoryConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown category: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getCategoryConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        categoryConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public ObjectTask getTask(String id) {
        QuestDefinition definition = getDefinitions().get(id);
        if (definition instanceof ObjectTask task) {
            return task;
        }
        throw new IllegalArgumentException("Unknown task: " + id);
    }

    public ObjectBattlePass getBattlePass(String id) {
        ObjectBattlePass value = battlePassConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown battlePass: " + id);
        }
        return value;
    }

    public ObjectMilestone getMilestone(String id) {
        ObjectMilestone value = milestoneConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown milestone: " + id);
        }
        return value;
    }

    public ObjectPeriod getPeriod(String id) {
        ObjectPeriod value = periodConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown period: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getPeriodConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        periodConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public ObjectScroll getScroll(String id) {
        ObjectScroll value = scrollConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown scroll: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getScrollConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        scrollConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public ObjectQuestPool getQuestPool(String id) {
        ObjectQuestPool value = questPoolConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown questPool: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getPoolConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        questPoolConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public ObjectRewardPool getRewardPool(String id) {
        ObjectRewardPool value = rewardPoolConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown rewardPool: " + id);
        }
        return value;
    }

    public Map<String, YamlConfiguration> getRewardPoolConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        rewardPoolConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public Map<String, QuestDefinition> getDefinitions() {
        Map<String, QuestDefinition> result = new LinkedHashMap<>();
        taskConfigs.values().forEach(task -> registerDefinition(result, task));
        for (ObjectBattlePass pass : battlePassConfigs.values()) {
            registerDefinition(result, pass);
            pass.getTasks().values().forEach(task -> registerDefinition(result, task));
        }
        milestoneConfigs.values().forEach(milestone -> registerDefinition(result, milestone));
        return result;
    }

    private void loadMenuFile(File directory, String fileName, String sectionName, YamlConfiguration candidate) throws Exception {
        YamlConfiguration loaded = InitManager.loadConfiguration(new File(directory, fileName + ".yml"));
        ConfigurationSection target = sectionName == null ? candidate : candidate.createSection(sectionName);
        copySection(loaded, target);
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) {
                target.set(key, source.get(key));
            } else {
                copySection(child, target.createSection(key));
            }
        }
    }

    private FileConfiguration configuration(String path) {
        return path != null && path.startsWith("menu.") ? menuConfig : config;
    }

    private static String normalizePath(String path) {
        return path != null && path.startsWith("menu.") ? path.substring("menu.".length()) : path;
    }

    private static void registerDefinition(Map<String, QuestDefinition> definitions, QuestDefinition definition) {
        if (definitions.putIfAbsent(definition.catalogKey(), definition) != null) {
            throw new IllegalArgumentException("Duplicate definition: " + definition.catalogKey());
        }
    }

}
