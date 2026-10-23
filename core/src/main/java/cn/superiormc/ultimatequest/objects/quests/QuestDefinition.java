package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.objects.counting.CountMode;

import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class QuestDefinition {

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
        this.config.set("counting-version", 1);
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
        generateLevels();
        thresholds = loadThresholds();
        EventConditionsManager.eventConditions.validate(config.getConfigurationSection("conditions"));
        RewardManager.rewardManager.validate(config.getConfigurationSection("rewards"));
    }

    private static void copy(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private void generateLevels() {
        if (!type.equals("battlepass") || !config.isConfigurationSection("leveling")) {
            return;
        }
        int count = config.getInt("leveling.count", 0);
        long base = config.getLong("leveling.base", 100L);
        long increment = config.getLong("leveling.increment", 0L);
        double multiplier = config.getDouble("leveling.multiplier", 1D);
        if (count <= 0 || count > 10000 || base <= 0 || increment < 0
                || !Double.isFinite(multiplier) || multiplier < 1D) {
            throw new IllegalArgumentException("Invalid level curve: " + id);
        }
        long total = 0L;
        for (int level = 1; level <= count; level++) {
            double value = (base + (double) increment * (level - 1)) * Math.pow(multiplier, level - 1);
            if (!Double.isFinite(value) || value >= Long.MAX_VALUE) {
                throw new IllegalArgumentException("Level curve overflows: " + id);
            }
            total = Math.addExact(total, (long) Math.ceil(value));
            String path = "levels." + level;
            if (!config.contains(path + ".required")) {
                config.set(path + ".required", total);
            } else {
                total = config.getLong(path + ".required");
            }
        }
    }

    public ConfigurationSection settings() {
        return config;
    }

    public Instant startsAt() {
        return timeWindow.startsAt();
    }

    public Instant endsAt() {
        return timeWindow.endsAt();
    }

    public boolean claimable(Instant now) {
        return timeWindow.claimable(now);
    }

    private Map<Integer, Long> loadThresholds() {
        if (!growth()) {
            return Map.of();
        }
        ConfigurationSection section = config.getConfigurationSection(type.equals("battlepass") ? "levels" : "stages");
        if (section == null || section.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Missing progression stages: " + id);
        }
        Map<Integer, Long> sorted = new TreeMap<>();
        for (String key : section.getKeys(false)) {
            int level = Integer.parseInt(key);
            ConfigurationSection stage = section.getConfigurationSection(key);
            if (level <= 0 || !String.valueOf(level).equals(key) || stage == null) {
                throw new IllegalArgumentException("Invalid progression stage: " + key);
            }
            long required = positiveLong(stage, "required", 0L);
            sorted.put(level, required);
            for (String track : tracks()) {
                String rewardKey = type.equals("battlepass") ? track : "rewards";
                if (stage.contains(rewardKey) && !stage.isConfigurationSection(rewardKey)) {
                    throw new IllegalArgumentException("Stage rewards must be a section: " + key + "/" + rewardKey);
                }
                RewardManager.rewardManager.validate(stageRewards(level, track));
            }
        }
        int expected = 1;
        long previous = 0L;
        for (Map.Entry<Integer, Long> entry : sorted.entrySet()) {
            if (entry.getKey() != expected || entry.getValue() <= previous) {
                throw new IllegalArgumentException("Stages must start at 1 and have increasing thresholds: " + id);
            }
            expected++;
            previous = entry.getValue();
        }
        return Collections.unmodifiableMap(new LinkedHashMap<>(sorted));
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

    public String type() {
        return type;
    }

    public boolean growth() {
        return !type.equals("normal");
    }

    public String season() {
        return config.getString("season", "");
    }

    public String catalogKey() {
        return growth() ? type + "/" + id : id;
    }

    public String storageKey() {
        return growth() && !season().isEmpty() ? catalogKey() + "/" + season() : catalogKey();
    }

    public boolean active(Instant now) {
        return timeWindow.active(now);
    }

    public List<String> tracks() {
        return type.equals("battlepass") ? List.of("free", "premium") : List.of("default");
    }

    public Map<Integer, Long> thresholds() {
        return thresholds;
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

    public long maximumTotal() {
        if (type.equals("battlepass") && config.getBoolean("cap-experience", true)) {
            return thresholds.get(thresholds.size());
        }
        return Long.MAX_VALUE;
    }

    public boolean premium(Player player) {
        String permission = config.getString("premium-permission", "ultimatequest.battlepass.{id}.{season}.premium")
                .replace("{id}", id).replace("{season}", season());
        return player.hasPermission(permission);
    }

    public ConfigurationSection stageRewards(int level, String track) {
        String path = type.equals("battlepass") ? "levels." + level + "." + track : "stages." + level + ".rewards";
        return config.getConfigurationSection(path);
    }

    public String id() {
        return id;
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

    public List<String> prerequisites() {
        return config.getStringList("prerequisites");
    }

    public boolean meetsConditions(Player player) {
        return EventConditionsManager.eventConditions.matches(config.getConfigurationSection("conditions"), new QuestContext(null, null, player));
    }

    public boolean automaticClaim() {
        return config.getBoolean("auto-claim", false);
    }

    public ConfigurationSection rewards() {
        return config.getConfigurationSection("rewards");
    }

    public String snapshot() {
        return config.saveToString();
    }
}
