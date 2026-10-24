package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import cn.superiormc.ultimatequest.objects.configs.ObjectBattlePass;
import cn.superiormc.ultimatequest.objects.configs.ObjectBattlePassTask;
import cn.superiormc.ultimatequest.objects.configs.ObjectCategory;
import cn.superiormc.ultimatequest.objects.configs.ObjectMenu;
import cn.superiormc.ultimatequest.objects.configs.ObjectMilestone;
import cn.superiormc.ultimatequest.objects.configs.ObjectPeriod;
import cn.superiormc.ultimatequest.objects.configs.ObjectQuest;
import cn.superiormc.ultimatequest.objects.configs.ObjectQuestPool;
import cn.superiormc.ultimatequest.objects.configs.ObjectRewardPool;
import cn.superiormc.ultimatequest.objects.configs.ObjectScroll;
import cn.superiormc.ultimatequest.objects.configs.ObjectTask;
import cn.superiormc.ultimatequest.objects.configs.ObjectTaskLimits;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import java.time.Instant;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;

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

    public ObjectTaskLimits getTaskLimits() {
        return taskLimits;
    }

    public FileConfiguration config;

    private volatile YamlConfiguration menuConfig = new YamlConfiguration();

    private volatile List<String> menuIds = List.of();

    private static final List<String> MENU_FILES = List.of("main", "battlepass", "milestone", "period", "scroll");

    public ConfigManager() {
        configManager = this;
        UltimateQuest.instance.reloadConfig();
        config = UltimateQuest.instance.getConfig();
        config.options().copyDefaults(true);
        loadMenus();
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

    private void loadMenus() {
        try {
            reloadMenus();
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("搂cError: Could not load menu configurations: " + exception.getMessage());
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

    private void loadMenuFile(File directory, String fileName, String sectionName, YamlConfiguration candidate) throws Exception {
        String resource = "menus/" + fileName + ".yml";
        YamlConfiguration loaded = InitManager.loadConfiguration(new File(directory, fileName + ".yml"), resource);
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
        ObjectTask value = taskConfigs.get(id);
        if (value == null) {
            throw new IllegalArgumentException("Unknown task: " + id);
        }
        return value;
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
        taskConfigs.values().forEach(value -> result.put(value.catalogKey(), value));
        battlePassConfigs.values().forEach(value -> result.put(value.catalogKey(), value));
        milestoneConfigs.values().forEach(value -> result.put(value.catalogKey(), value));
        return result;
    }

    public synchronized void reloadQuestConfigs(SeasonManager seasons) throws Exception {
        Map<String, YamlConfiguration> loadedCategories = ConfigurationFileUtil.readFolder("categories", true);
        Map<String, ObjectCategory> candidateCategories = new LinkedHashMap<>();
        loadedCategories.forEach((id, value) -> candidateCategories.put(id, new ObjectCategory(id, value)));
        Map<String, YamlConfiguration> loadedObjectives = ConfigurationFileUtil.readFolder("quests");
        ObjectTaskLimits.validateTaskLimits(ConfigManager.configManager.getSection("task-limits"), loadedCategories.keySet());
        Object savePeriod = ConfigManager.configManager.config.get("auto-save.period-tick", 6000);
        if (!(savePeriod instanceof Integer || savePeriod instanceof Long) || ((Number) savePeriod).longValue() < 1L) {
            throw new IllegalArgumentException("auto-save.period-tick must be a positive integer");
        }
        for (Map.Entry<String, YamlConfiguration> entry : loadedObjectives.entrySet()) {
            YamlConfiguration resolved = QuestObjective.resolve(entry.getKey(), entry.getValue(), loadedObjectives);
            new QuestObjective(entry.getKey(), resolved);
            ObjectCategory.validateCategories(entry.getValue(), loadedCategories, "quests/" + entry.getKey() + ".yml");
        }
        Map<String, YamlConfiguration> pools = ConfigurationFileUtil.readFolder("pools");
        Map<String, ObjectQuestPool> candidatePools = new LinkedHashMap<>();
        for (var entry : pools.entrySet()) {
            candidatePools.put(entry.getKey(), new ObjectQuestPool(entry.getKey(), entry.getValue(), pools, loadedObjectives, Map.of()));
        }
        Map<String, YamlConfiguration> rewardPools = loadRewardPoolConfigs();
        Map<String, QuestDefinition> loaded = new LinkedHashMap<>();
        seasons.beginReload();
        loadDefinitions("tasks", "normal", loaded, loadedObjectives, rewardPools, seasons);
        loadDefinitions("battlepasses", "battlepass", loaded, loadedObjectives, rewardPools, seasons);
        loadDefinitions("milestones", "milestone", loaded, loadedObjectives, rewardPools, seasons);
        for (QuestDefinition definition : loaded.values()) {
            String categoryLocation = definition.growth() ? definition.type().equals("battlepass") ? "battlepasses/" + definition.id() + ".yml" : "milestones/" + definition.id() + ".yml" : "tasks/" + definition.id() + ".yml";
            if (definition.isBattlePassTask()) {
                String pass = definition.getBattlePass();
                categoryLocation = "battlepasses/" + pass + ".yml: tasks." + definition.id().substring(pass.length() + 1);
            }
            ObjectCategory.validateCategories(definition.settings(), loadedCategories, categoryLocation);
            if (!definition.growth()) {
                checkPrerequisites(definition.id(), loaded, new HashSet<>());
            }
            validateRewardTargets(definition.rewards(), loaded);
            for (int level : definition.thresholds().keySet()) {
                for (String track : definition.tracks()) {
                    validateRewardTargets(definition.stageRewards(level, track), loaded);
                }
            }
        }
        Map<String, YamlConfiguration> periods = ConfigurationFileUtil.readFolder("periods");
        Map<String, ObjectPeriod> candidatePeriods = new LinkedHashMap<>();
        for (var entry : periods.entrySet()) {
            candidatePeriods.put(entry.getKey(), new ObjectPeriod(entry.getKey(), entry.getValue(), pools, loadedObjectives, rewardPools));
        }
        Map<String, YamlConfiguration> scrolls = ConfigurationFileUtil.readFolder("scrolls");
        Map<String, ObjectScroll> candidateScrolls = new LinkedHashMap<>();
        for (var entry : scrolls.entrySet()) {
            candidateScrolls.put(entry.getKey(), new ObjectScroll(entry.getKey(), entry.getValue(), pools, loadedObjectives, rewardPools));
        }
        for (Map.Entry<String, YamlConfiguration> entry : periods.entrySet()) {
            ObjectCategory.validateCategories(entry.getValue().getConfigurationSection("task"), loadedCategories, "periods/" + entry.getKey() + ".yml: task");
            validateRewardTargets(entry.getValue().getConfigurationSection("task.rewards"), loaded);
        }
        for (Map.Entry<String, YamlConfiguration> entry : scrolls.entrySet()) {
            ObjectCategory.validateCategories(entry.getValue(), loadedCategories, "scrolls/" + entry.getKey() + ".yml");
            validateRewardTargets(entry.getValue().getConfigurationSection("rewards"), loaded);
        }
        ObjectTaskLimits candidateLimits = new ObjectTaskLimits(getSection("task-limits"), loadedCategories.keySet());
        Map<String, ObjectQuest> candidateQuests = new LinkedHashMap<>();
        loadedObjectives.forEach((id, value) -> {
            ObjectQuest quest = new ObjectQuest(id, value);
            quest.initObjective(loadedObjectives);
            candidateQuests.put(id, quest);
        });
        Map<String, ObjectTask> candidateTasks = new LinkedHashMap<>();
        Map<String, ObjectBattlePass> candidatePasses = new LinkedHashMap<>();
        Map<String, ObjectMilestone> candidateMilestones = new LinkedHashMap<>();
        for (QuestDefinition definition : loaded.values()) {
            if (definition instanceof ObjectTask task) {
                candidateTasks.put(task.id(), task);
            }
            if (definition instanceof ObjectBattlePass pass) {
                candidatePasses.put(pass.id(), pass);
            }
            if (definition instanceof ObjectMilestone milestone) {
                candidateMilestones.put(milestone.id(), milestone);
            }
        }
        Map<String, ObjectRewardPool> candidateRewards = new LinkedHashMap<>();
        rewardPools.forEach((id, value) -> candidateRewards.put(id, new ObjectRewardPool(id, value)));
        seasons.commitReload();
        taskLimits = candidateLimits;
        questConfigs = candidateQuests;
        categoryConfigs = candidateCategories;
        taskConfigs = candidateTasks;
        battlePassConfigs = candidatePasses;
        milestoneConfigs = candidateMilestones;
        periodConfigs = candidatePeriods;
        scrollConfigs = candidateScrolls;
        questPoolConfigs = candidatePools;
        rewardPoolConfigs = candidateRewards;
    }

    private void loadDefinitions(String folder, String type, Map<String, QuestDefinition> loaded, Map<String, YamlConfiguration> library, Map<String, YamlConfiguration> rewardPools, SeasonManager seasons) throws Exception {
        File directory = new File(UltimateQuest.instance.getDataFolder(), folder);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create definition directory: " + folder);
        }
        File[] files = directory.listFiles((ignored, name) -> name.endsWith(".yml"));
        if (files == null) {
            throw new IllegalStateException("Could not list definitions: " + folder);
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        for (File file : files) {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            if (!config.getBoolean("enabled", true)) {
                continue;
            }
            if (!config.getString("type", type).equals(type)) {
                throw new IllegalArgumentException("Wrong definition type in " + file.getName());
            }
            config.set("type", type);
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            if (Set.of("battlepass", "milestone").contains(type)) {
                seasons.prepare(type, id, config, Instant.now());
            }
            RewardManager.rewardManager.resolveReferences(config, rewardPools);
            QuestDefinition definition = switch (type) {
                        case "battlepass" -> new ObjectBattlePass(id, config, library, rewardPools);
                        case "milestone" -> new ObjectMilestone(id, config, library);
                        default -> new ObjectTask(id, config, library);
                    };
            if (loaded.putIfAbsent(definition.catalogKey(), definition) != null) {
                throw new IllegalArgumentException("Duplicate definition: " + definition.catalogKey());
            }
            if (definition instanceof ObjectBattlePass pass) {
                for (ObjectBattlePassTask task : pass.getTasks().values()) {
                                if (loaded.putIfAbsent(task.catalogKey(), task) != null) {
                                    throw new IllegalArgumentException("Duplicate battle pass task: " + task.id());
                                }
                            }
            }
        }
    }

    private static void validateRewardTargets(ConfigurationSection rewards, Map<String, QuestDefinition> loaded) {
        RewardManager.rewardManager.forEachReward(rewards, (key, reward) -> {
            if (reward.getString("type", "").equals("battlepass-experience") && !loaded.containsKey("battlepass/" + reward.getString("battlepass", ""))) {
                throw new IllegalArgumentException("Unknown battle pass reward target: " + key);
            }
        });
    }

    private static void checkPrerequisites(String id, Map<String, QuestDefinition> loaded, Set<String> path) {
        if (!loaded.containsKey(id) || !path.add(id)) {
            throw new IllegalArgumentException("Missing or cyclic prerequisite: " + id);
        }
        for (String previous : loaded.get(id).prerequisites()) {
            checkPrerequisites(previous, loaded, path);
        }
        path.remove(id);
    }

    private Map<String, YamlConfiguration> loadRewardPoolConfigs() throws Exception {
        Map<String, YamlConfiguration> loaded = ConfigurationFileUtil.readFolder("reward-pools");
        for (var entry : loaded.entrySet()) {
            ObjectRewardPool pool = new ObjectRewardPool(entry.getKey(), entry.getValue());
            pool.initRewards(loaded);
        }
        return loaded;
    }
}
