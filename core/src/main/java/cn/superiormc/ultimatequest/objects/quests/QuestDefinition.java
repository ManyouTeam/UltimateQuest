package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.SeasonManager;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.counting.CountMode;
import cn.superiormc.ultimatequest.objects.rewards.ObjectReward;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.CommonUtil;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class QuestDefinition {

    private final ObjectReward reward;

    private final String id;

    private final YamlConfiguration config;

    private final List<QuestObjective> objectives;

    private final String type;

    private final Map<Integer, Long> thresholds;

    private final TimeWindow timeWindow;

    public QuestDefinition(String id, YamlConfiguration config) {
        this(id, config, Map.of());
    }

    public QuestDefinition(String id, YamlConfiguration config, Map<String, YamlConfiguration> library) {
        if (!id.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid quest id: " + id);
        }
        this.id = id;
        this.config = config;
        if (!rarity().matches("[a-z0-9_-]+") || !difficulty().matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid quest rarity or difficulty: " + id);
        }
        type = config.getString("type", "normal");
        if (!Set.of("normal", "battlepass", "milestone").contains(type)) {
            throw new IllegalArgumentException("Unknown quest type: " + type);
        }
        if (!Set.of("player", "admin").contains(config.getString("accept-mode", "player"))) {
            throw new IllegalArgumentException("Invalid accept-mode in task " + id + ": expected player or admin");
        }
        if (type.equals("battlepass") && !season().matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Battle pass " + id + " requires a valid season");
        }
        if (type.equals("milestone") && !season().isEmpty() && !season().matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid milestone season: " + id);
        }
        timeWindow = TimeWindow.read(config);
        String objectivePath = growth() ? "sources" : "objectives";
        ConfigurationSection section = config.getConfigurationSection(objectivePath);
        if (!growth() && section == null && config.isConfigurationSection("source")) {
            section = config.createSection("objectives");
            ConfigurationSection single = section.createSection("main");
            copy(config.getConfigurationSection("source"), single);
        }
        if (!growth() && (section == null || section.getKeys(false).isEmpty())) {
            throw new IllegalArgumentException("Quest " + id + " requires objectives");
        }
        List<QuestObjective> loaded = new ArrayList<>();
        if (section != null && !type.equals("battlepass")) {
            for (String key : section.getKeys(false)) {
                if (!key.matches("[a-z0-9_-]+") || !section.isConfigurationSection(key)) {
                    throw new IllegalArgumentException("Invalid objective id: " + key);
                }
                YamlConfiguration resolved = QuestObjective.resolve(key, section.getConfigurationSection(key), library);
                config.set(objectivePath + "." + key, null);
                ConfigurationSection normalized = config.createSection(objectivePath + "." + key);
                copy(resolved, normalized);
                loaded.add(new QuestObjective(key, normalized, growth()));
            }
        }
        objectives = List.copyOf(loaded);
        thresholds = new ObjectProgression(this, config).getThresholds();
        EventConditionsManager.eventConditions.validate(config.getConfigurationSection("conditions"));
        reward = new ObjectReward(config.getConfigurationSection("rewards"));
        reward.validate();
    }

    protected static YamlConfiguration prepareConfiguration(String id, String type, YamlConfiguration config, Map<String, YamlConfiguration> rewards, SeasonManager seasons) {
        if (!config.getString("type", type).equals(type)) {
            throw new IllegalArgumentException("Wrong definition type: " + id);
        }
        config.set("type", type);
        if (seasons != null) {
            seasons.prepare(type, id, config, Instant.now());
        }
        RewardManager.rewardManager.resolveReferences(config, rewards);
        return config;
    }

    public void initReferences(Map<String, YamlConfiguration> categories, Map<String, QuestDefinition> definitions) {
        String location = growth() ? (type.equals("battlepass") ? "battlepasses/" : "milestones/") + id + ".yml" : "tasks/" + id + ".yml";
        if (isBattlePassTask()) {
            String pass = getBattlePass();
            location = "battlepasses/" + pass + ".yml: tasks." + id.substring(pass.length() + 1);
        }
        ObjectCategory.validateCategories(config, categories, location);
        if (!growth()) {
            checkPrerequisites(id, definitions, new HashSet<>());
        }
        reward.validateTargets(definitions);
        for (int level : thresholds.keySet()) {
            for (String track : tracks()) {
                new ObjectReward(stageRewards(level, track)).validateTargets(definitions);
            }
        }
    }

    public boolean active(Instant now) {
        return timeWindow.active(now);
    }

    public boolean claimable(Instant now) {
        return timeWindow.claimable(now);
    }

    public boolean meetsConditions(Player player) {
        return EventConditionsManager.eventConditions.matches(config.getConfigurationSection("conditions"), new QuestContext(null, null, player));
    }

    public boolean premium(Player player) {
        String permission = config.getString("premium-permission", "ultimatequest.battlepass.{id}.{season}.premium")
                .replace("{id}", id).replace("{season}", season());
        return player.hasPermission(permission);
    }

    public int level(long total) {
        int level = 0;
        for (Map.Entry<Integer, Long> entry : thresholds.entrySet()) {
            if (total < entry.getValue()) {
                break;
            }
            level = entry.getKey();
        }
        return level;
    }

    public List<String> objectiveLines(Player player, QuestInstance instance) {
        List<String> lines = new ArrayList<>();
        for (QuestObjective objective : objectives) {
            if (growth()) {
                lines.add(LanguageManager.languageManager.getStringText(player,
                        objective.countsItems() ? "progression.objective-items"
                                : objective.countMode() == CountMode.EVENTS ? "progression.objective" : "progression.objective-counted", "",
                        "name", objective.name(player), "value", String.valueOf(objective.value()),
                        "count-unit", LanguageManager.languageManager.getStringText(player,
                                "progression.count-units." + objective.countMode().id(), objective.countMode().id()),
                        "unit", LanguageManager.languageManager.getStringText(player, "progression.unit." + type, "")));
            } else {
                lines.add(LanguageManager.languageManager.getStringText(player, "quests.objective", "",
                        "name", objective.name(player), "current", instance == null ? "0" : instance.progressAmount(objective).stripTrailingZeros().toPlainString(),
                        "required", String.valueOf(objective.required())));
            }
        }
        return lines;
    }

    public String snapshot() {
        return config.saveToString();
    }

    public String id() {
        return id;
    }

    public ConfigurationSection settings() {
        return config;
    }

    public String type() {
        return type;
    }

    public boolean growth() {
        return !type.equals("normal");
    }

    public String catalogKey() {
        return growth() ? type + "/" + id : id;
    }

    public String storageKey() {
        return growth() && !season().isEmpty() ? catalogKey() + "/" + season() : catalogKey();
    }

    public String season() {
        return config.getString("season", "");
    }

    public Instant startsAt() {
        return timeWindow.startsAt();
    }

    public Instant endsAt() {
        return timeWindow.endsAt();
    }

    public List<String> categories() {
        return config.getStringList("categories").stream().distinct().toList();
    }

    public boolean playerAcceptable() {
        return config.getString("accept-mode", "player").equals("player");
    }

    public String rarity() {
        return config.getString("rarity", "common");
    }

    public String difficulty() {
        return config.getString("difficulty", "easy");
    }

    public boolean listed() {
        return config.getBoolean("listed", true);
    }

    public String name(Player player) {
        return CommonUtil.parseLang(player, config.getString("name", id));
    }

    public List<String> description(Player player) {
        return CommonUtil.modifyList(player, config.getStringList("description"));
    }

    public ConfigurationSection icon() {
        return config.getConfigurationSection("icon");
    }

    public List<QuestObjective> objectives() {
        return objectives;
    }

    public List<String> prerequisites() {
        return config.getStringList("prerequisites");
    }

    public boolean automaticClaim() {
        return config.getBoolean("auto-claim", false);
    }

    public ConfigurationSection rewards() {
        return config.getConfigurationSection("rewards");
    }

    public ObjectReward getReward() {
        return reward;
    }

    public List<String> tracks() {
        return type.equals("battlepass") ? List.of("free", "premium") : List.of("default");
    }

    public Map<Integer, Long> thresholds() {
        return thresholds;
    }

    public long maximumTotal() {
        if (type.equals("battlepass") && config.getBoolean("cap-experience", true)) {
            return thresholds.get(thresholds.size());
        }
        return Long.MAX_VALUE;
    }

    public ConfigurationSection stageRewards(int level, String track) {
        String path = type.equals("battlepass") ? "levels." + level + "." + track : "stages." + level + ".rewards";
        return config.getConfigurationSection(path);
    }

    public boolean isBattlePassTask() {
        return config.contains("challenge.pass");
    }

    public String getBattlePass() {
        return config.getString("challenge.pass", "");
    }

    public long getPoints() {
        return config.getLong("challenge.points", 0L);
    }

    public int getUnlockWeek() {
        return Math.max(1, config.getInt("challenge.week", 1));
    }

    public boolean allowsCatchUp() {
        return config.getBoolean("challenge.catch-up", true);
    }

    public boolean isPremiumTask() {
        return config.getBoolean("challenge.premium", false);
    }

    public String getReset() {
        return config.getString("challenge.reset", "season");
    }

    public static long positiveLong(ConfigurationSection section, String key, long fallback) {
        Object value = section.get(key, fallback);
        if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
                || ((Number) value).longValue() <= 0L) {
            throw new IllegalArgumentException("Expected a positive integer for " + key);
        }
        return ((Number) value).longValue();
    }

    public static long positiveLongValue(String value) {
        long number = Long.parseLong(value);
        if (number <= 0L) {
            throw new IllegalArgumentException("Expected a positive integer");
        }
        return number;
    }

    private static void copy(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private static void checkPrerequisites(String id, Map<String, QuestDefinition> definitions, Set<String> path) {
        if (!definitions.containsKey(id) || !path.add(id)) {
            throw new IllegalArgumentException("Missing or cyclic prerequisite: " + id);
        }
        for (String previous : definitions.get(id).prerequisites()) {
            checkPrerequisites(previous, definitions, path);
        }
        path.remove(id);
    }

}
