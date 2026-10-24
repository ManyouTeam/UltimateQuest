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
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.rewards.ObjectRewardPool;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager extends AbstractManager {

    public static ConfigManager configManager;

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

    public Map<String, ObjectMenu> menuConfigs = new LinkedHashMap<>();

    private YamlConfiguration menuSettings = new YamlConfiguration();

    public ConfigManager() {
        configManager = this;
        try {
            reloadMainConfig();
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load configurations: " + exception.getMessage());
            throw new IllegalStateException("Could not load configurations", exception);
        }
    }

    public synchronized void reloadMainConfig() {
        UltimateQuest.instance.reloadConfig();
        reloadMenus();
        config = UltimateQuest.instance.getConfig();
    }

    public synchronized void reloadMenus() {
        initMenuConfigs();
    }

    public synchronized void reloadMenuConfigs() {
        initMenuConfigs();
    }

    private void initMenuConfigs() {
        menuConfigs.clear();
        menuSettings = new YamlConfiguration();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "menus");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage(
                    "§cError: Could not create menu configuration directory: " + directory);
            return;
        }
        File settingsFile = new File(directory, "settings.yml");
        try {
            menuSettings = YamlConfiguration.loadConfiguration(settingsFile);
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage(
                    "§cError: Could not load menu configuration menus/settings.yml: " + exception.getMessage());
        }
        for (File file : CommonUtil.getYamlFiles(directory)) {
            if (file.equals(settingsFile)) {
                continue;
            }
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            String resource = "menus/" + directory.toPath().relativize(file.toPath())
                    .toString().replace(File.separatorChar, '/');
            try {
                YamlConfiguration section = YamlConfiguration.loadConfiguration(file);
                ObjectMenu menu = new ObjectMenu(id, section);
                if (menuConfigs.putIfAbsent(id, menu) != null) {
                    throw new IllegalArgumentException("Duplicate menu id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded menu: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load menu " + resource + ": " + throwable.getMessage());
            }
        }
    }

    @Override
    public synchronized void onInit() {
        Map<String, ObjectCategory> previousCategories = categoryConfigs;
        Map<String, ObjectQuest> previousQuests = questConfigs;
        Map<String, ObjectQuestPool> previousQuestPools = questPoolConfigs;
        Map<String, ObjectRewardPool> previousRewardPools = rewardPoolConfigs;
        Map<String, ObjectTask> previousTasks = taskConfigs;
        Map<String, ObjectBattlePass> previousBattlePasses = battlePassConfigs;
        Map<String, ObjectMilestone> previousMilestones = milestoneConfigs;
        Map<String, ObjectPeriod> previousPeriods = periodConfigs;
        Map<String, ObjectScroll> previousScrolls = scrollConfigs;
        ObjectTaskLimits previousTaskLimits = taskLimits;
        SeasonManager seasons = SeasonManager.seasonManager;
        try {
            seasons.beginReload();
            initCategoryConfigs();
            initQuestConfigs();
            loadQuestConfigs();
            initQuestPoolConfigs();
            initRewardPoolConfigs();
            loadRewardPoolConfigs();
            initTaskConfigs();
            initBattlePassConfigs();
            initMilestoneConfigs();
            initPeriodConfigs();
            initScrollConfigs();
            loadTaskConfigs();
            initTaskLimits();
            for (QuestDefinition definition : getDefinitions().values()) {
                if (definition.growth()) {
                    seasons.prepare(definition, Instant.now());
                }
            }
            seasons.commitReload();
        } catch (Exception | Error exception) {
            categoryConfigs = previousCategories;
            questConfigs = previousQuests;
            questPoolConfigs = previousQuestPools;
            rewardPoolConfigs = previousRewardPools;
            taskConfigs = previousTasks;
            battlePassConfigs = previousBattlePasses;
            milestoneConfigs = previousMilestones;
            periodConfigs = previousPeriods;
            scrollConfigs = previousScrolls;
            taskLimits = previousTaskLimits;
            seasons.cancelReload();
            if (exception instanceof Error error) {
                throw error;
            }
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load quest configurations: " + exception.getMessage());
            throw new IllegalStateException("Could not load quest configurations", exception);
        }
    }

    private void initCategoryConfigs() {
        categoryConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "categories");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                ObjectCategory value = new ObjectCategory(id, config);
                if (categoryConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate category id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded category: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load category " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initQuestConfigs() {
        questConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "quests");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectQuest value = new ObjectQuest(id, config);
                if (questConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate quest id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded quest: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load quest " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void loadQuestConfigs() {
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> categories = getCategoryConfigurations();
        for (ObjectQuest quest : questConfigs.values()) {
            quest.initObjective(library, categories);
        }
    }

    private void initQuestPoolConfigs() {
        questPoolConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "pools");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectQuestPool value = new ObjectQuestPool(id, config, library);
                if (questPoolConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate quest pool id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded quest pool: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load quest pool " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initRewardPoolConfigs() {
        rewardPoolConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "reward-pools");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectRewardPool value = new ObjectRewardPool(id, config);
                if (rewardPoolConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate reward pool id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded reward pool: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load reward pool " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void loadRewardPoolConfigs() {
        Map<String, YamlConfiguration> pools = getRewardPoolConfigurations();
        for (ObjectRewardPool pool : rewardPoolConfigs.values()) {
            pool.initRewards(pools);
        }
    }

    private void initTaskConfigs() {
        taskConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "tasks");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectTask value = new ObjectTask(id, config, library, rewards);
                if (taskConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate task id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded task: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load task " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initBattlePassConfigs() {
        battlePassConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "battlepasses");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectBattlePass value = new ObjectBattlePass(id, config, library, rewards);
                if (battlePassConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate battle pass id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded battle pass: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load battle pass " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initMilestoneConfigs() {
        milestoneConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "milestones");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectMilestone value = new ObjectMilestone(id, config, library, rewards);
                if (milestoneConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate milestone id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded milestone: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load milestone " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initPeriodConfigs() {
        periodConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "periods");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> pools = getPoolConfigurations();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectPeriod value = new ObjectPeriod(id, config, pools, library, rewards);
                if (periodConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate period id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded period: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load period " + file.getName() + ": " + throwable.getMessage());
            }
        }
    }

    private void initScrollConfigs() {
        scrollConfigs = new LinkedHashMap<>();
        File directory = new File(UltimateQuest.instance.getDataFolder(), "scrolls");
        if (!directory.exists() && !directory.mkdirs()) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not create directory: " + directory);
            return;
        }
        Map<String, YamlConfiguration> pools = getPoolConfigurations();
        Map<String, YamlConfiguration> library = getQuestConfigurations();
        Map<String, YamlConfiguration> rewards = getRewardPoolConfigurations();
        for (File file : CommonUtil.getYamlFiles(directory)) {
            String id = file.getName().substring(0, file.getName().length() - 4)
                    .toLowerCase(Locale.ROOT);
            try {
                YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
                if (!config.getBoolean("enabled", true)) {
                    continue;
                }
                ObjectScroll value = new ObjectScroll(id, config, pools, library, rewards);
                if (scrollConfigs.putIfAbsent(id, value) != null) {
                    throw new IllegalArgumentException("Duplicate scroll id: " + id);
                }
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fLoaded scroll: " + id + ".yml!");
            } catch (Throwable throwable) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Failed to load scroll " + file.getName() + ": " + throwable.getMessage());
            }
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

    public synchronized ObjectTaskLimits getTaskLimits() {
        return taskLimits;
    }

    public synchronized boolean getBoolean(String path) {
        return config.getBoolean(path, false);
    }

    public synchronized boolean getBoolean(String path, boolean defaultValue) {
        return config.getBoolean(path, defaultValue);
    }

    public synchronized String getString(String path, String... args) {
        String s = config.getString(path);
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

    public synchronized int getInt(String path, int defaultValue) {
        return config.getInt(path, defaultValue);
    }

    public synchronized double getDouble(String path, double defaultValue) {
        return config.getDouble(path, defaultValue);
    }

    public synchronized ConfigurationSection getSection(String path) {
        if (config.getConfigurationSection(path) == null) {
            return new MemoryConfiguration();
        }
        return config.getConfigurationSection(path);
    }

    public synchronized ConfigurationSection getMenuSettings() {
        return menuSettings;
    }

    public synchronized ConfigurationSection getMenu(String id) {
        if (id == null) {
            return new MemoryConfiguration();
        }
        ObjectMenu menu = menuConfigs.get(id.toLowerCase(Locale.ROOT));
        return menu == null ? new MemoryConfiguration() : menu.getConfig();
    }

    public synchronized ObjectMenu getMenuObject(String id) {
        ObjectMenu menu = id == null ? null : menuConfigs.get(id.toLowerCase(Locale.ROOT));
        if (menu == null) {
            throw new IllegalArgumentException("Unknown menu: " + id);
        }
        return menu;
    }

    public synchronized boolean hasMenu(String id) {
        return id != null && menuConfigs.containsKey(id.toLowerCase(Locale.ROOT));
    }

    public synchronized Collection<String> getMenuIds() {
        return List.copyOf(menuConfigs.keySet());
    }

    public synchronized ObjectQuest getQuest(String id) {
        ObjectQuest value = questConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown quest: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getQuestConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        questConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized ObjectCategory getCategory(String id) {
        ObjectCategory value = categoryConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown category: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getCategoryConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        categoryConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized ObjectTask getTask(String id) {
        QuestDefinition definition = getDefinitions().get(id);
        if (definition instanceof ObjectTask task) {
            return task;
        }
        throw new IllegalArgumentException("Unknown task: " + id);
    }

    public synchronized ObjectBattlePass getBattlePass(String id) {
        ObjectBattlePass value = battlePassConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown battlePass: " + id);
        }
        return value;
    }

    public synchronized ObjectMilestone getMilestone(String id) {
        ObjectMilestone value = milestoneConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown milestone: " + id);
        }
        return value;
    }

    public synchronized ObjectPeriod getPeriod(String id) {
        ObjectPeriod value = periodConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown period: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getPeriodConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        periodConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized ObjectScroll getScroll(String id) {
        ObjectScroll value = scrollConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown scroll: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getScrollConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        scrollConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized ObjectQuestPool getQuestPool(String id) {
        ObjectQuestPool value = questPoolConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown questPool: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getPoolConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        questPoolConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized ObjectRewardPool getRewardPool(String id) {
        ObjectRewardPool value = rewardPoolConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown rewardPool: " + id);
        }
        return value;
    }

    public synchronized Map<String, YamlConfiguration> getRewardPoolConfigurations() {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        rewardPoolConfigs.forEach((id, value) -> result.put(id, value.getConfig()));
        return Collections.unmodifiableMap(result);
    }

    public synchronized Map<String, QuestDefinition> getDefinitions() {
        Map<String, QuestDefinition> result = new LinkedHashMap<>();
        taskConfigs.values().forEach(task -> registerDefinition(result, task));
        for (ObjectBattlePass pass : battlePassConfigs.values()) {
            registerDefinition(result, pass);
            pass.getTasks().values().forEach(task -> registerDefinition(result, task));
        }
        milestoneConfigs.values().forEach(milestone -> registerDefinition(result, milestone));
        return result;
    }

    public Collection<QuestDefinition> getListedDefinitions() {
        return getDefinitions().values().stream().filter(definition -> !definition.growth() && definition.listed()
                && !definition.isBattlePassTask()).toList();
    }

    public QuestDefinition getDefinition(String id) {
        QuestDefinition definition = getDefinitions().get(id.toLowerCase(Locale.ROOT));
        return definition != null && !definition.growth() ? definition : null;
    }

    public Collection<QuestDefinition> getGrowthDefinitions(String type) {
        return getDefinitions().values().stream().filter(definition -> definition.type().equals(type)).toList();
    }

    public QuestDefinition getGrowthDefinition(String type, String id) {
        return getDefinitions().get(type + "/" + id.toLowerCase(Locale.ROOT));
    }

    public synchronized QuestDefinition resolveDefinition(String key, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Missing quest instance: " + key);
        }
        String templateId = section.getString("template-id", "");
        String source = section.getString("source", "normal");
        if (source.equals("period")) {
            String group = section.getString("source-id", "");
            ConfigurationSection period = getPeriodConfigurations().get(group);
            if (period == null) {
                return null;
            }
            ConfigurationSection configured = period.getConfigurationSection("source");
            if (!templateId.startsWith(group + "_") || configured == null) {
                throw new IllegalArgumentException("Invalid periodic quest reference: " + key);
            }
            String objective = configured.contains("pool") ? templateId.substring(group.length() + 1) : null;
            if (objective != null && !getQuestConfigurations().containsKey(objective)) {
                return null;
            }
            return getPeriod(group).createDefinition(objective, getQuestConfigurations());
        }
        if (source.equals("scroll")) {
            if (!getScrollConfigurations().containsKey(templateId)) {
                return null;
            }
            return getScroll(templateId).createDefinition(section.getStringList("objective-ids"));
        }
        QuestDefinition definition = getDefinitions().get(templateId);
        if (definition != null && source.equals("normal")
                && !(definition.growth() ? key.equals(definition.catalogKey()) || key.startsWith(definition.catalogKey() + "/")
                : definition.storageKey().equals(key))) {
            return null;
        }
        return definition;
    }

    public List<String> getDisplayCategories(QuestDefinition definition, QuestInstance instance) {
        if (instance != null && Set.of("period", "scroll").contains(instance.source())) {
            return definition.categories();
        }
        return getDefinitions().getOrDefault(definition.catalogKey(), definition).categories();
    }

    private void registerDefinition(Map<String, QuestDefinition> definitions, QuestDefinition definition) {
        if (definitions.putIfAbsent(definition.catalogKey(), definition) != null) {
            throw new IllegalArgumentException("Duplicate definition: " + definition.catalogKey());
        }
    }

}
