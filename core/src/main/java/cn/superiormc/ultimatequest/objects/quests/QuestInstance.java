package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.SeasonManager;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class QuestInstance {

    private static final Set<String> STATES = Set.of("ACTIVE", "COMPLETED", "CLAIMING", "CLAIMED", "REVIEW");

    private final UUID instanceId;

    private QuestDefinition definition;

    private final String season;

    private TimeWindow seasonWindow;

    private final long acceptedAt;

    private final Map<String, Long> progress = new LinkedHashMap<>();

    private final Map<String, BigDecimal> progressRemainders = new LinkedHashMap<>();

    private BigDecimal totalRemainder = BigDecimal.ZERO;

    private final Map<String, String> stageStates = new LinkedHashMap<>();

    private final Map<String, List<String>> stageRewardKeys = new LinkedHashMap<>();

    private final Map<String, Long> stageRequirements = new LinkedHashMap<>();

    private final List<String> rewardKeys;

    private long total;

    private boolean premium;

    private double booster = 1D;

    private long boosterUntil;

    private String state;

    private final String source;

    private final String sourceId;

    private final String cycle;

    private final long expiresAt;

    public QuestInstance(QuestInstance original) {
        instanceId = original.instanceId;
        definition = original.definition;
        season = original.season;
        seasonWindow = original.seasonWindow;
        acceptedAt = original.acceptedAt;
        progress.putAll(original.progress);
        progressRemainders.putAll(original.progressRemainders);
        totalRemainder = original.totalRemainder;
        stageStates.putAll(original.stageStates);
        original.stageRewardKeys.forEach((key, value) -> stageRewardKeys.put(key, List.copyOf(value)));
        stageRequirements.putAll(original.stageRequirements);
        rewardKeys = original.rewardKeys == null ? null : List.copyOf(original.rewardKeys);
        total = original.total;
        premium = original.premium;
        booster = original.booster;
        boosterUntil = original.boosterUntil;
        state = original.state;
        source = original.source;
        sourceId = original.sourceId;
        cycle = original.cycle;
        expiresAt = original.expiresAt;
    }

    public QuestInstance(QuestDefinition definition) {
        this(definition, "normal", "", "", 0L);
    }

    public QuestInstance(QuestDefinition definition, String source, String sourceId, String cycle, long expiresAt) {
        instanceId = UUID.randomUUID();
        this.definition = definition;
        season = definition.growth() && SeasonManager.seasonManager != null
                ? SeasonManager.seasonManager.season(definition) : definition.season();
        seasonWindow = initialWindow(definition, season);
        acceptedAt = System.currentTimeMillis();
        state = "ACTIVE";
        this.source = source;
        this.sourceId = sourceId;
        this.cycle = cycle;
        this.expiresAt = expiresAt;
        rewardKeys = drawRewards(definition);
        validateSource();
    }

    public QuestInstance(String id, ConfigurationSection section) throws Exception {
        this(id, section, ConfigManager.configManager.resolveDefinition(id, section));
    }

    public QuestInstance(String id, ConfigurationSection section, QuestDefinition definition) throws Exception {
        if (section == null) {
            throw new IllegalArgumentException("Missing quest instance: " + id);
        }
        if (definition == null) {
            throw new IllegalArgumentException("Missing quest definition: " + id);
        }
        instanceId = UUID.fromString(section.getString("instance-id", ""));
        acceptedAt = section.getLong("accepted-at");
        this.definition = definition;
        String prefix = definition.catalogKey() + "/";
        season = definition.growth() ? section.getString("season", id.startsWith(prefix)
                ? id.substring(prefix.length()) : "") : definition.season();
        if (!season.isEmpty() && !season.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid saved season: " + id);
        }
        ConfigurationSection savedWindow = section.getConfigurationSection("season-time");
        seasonWindow = savedWindow == null ? initialWindow(definition, season) : TimeWindow.read(savedWindow);
        source = section.getString("source", "normal");
        sourceId = section.getString("source-id", "");
        cycle = section.getString("cycle", "");
        expiresAt = section.getLong("expires-at", 0L);
        rewardKeys = section.contains("reward-keys") ? readRewardKeys(section, "reward-keys") : null;
        if (rewardKeys == null && RewardManager.rewardManager.hasRandom(definition.rewards())) {
            throw new IllegalArgumentException("Missing saved reward keys: " + id);
        }
        validateSource();
        if (!key().equals(id)) {
            throw new IllegalArgumentException("Quest instance key does not match its definition: " + id);
        }
        state = section.getString("state", "");
        if (!STATES.contains(state)) {
            throw new IllegalArgumentException("Unknown quest state: " + state);
        }
        if (!definition.growth()) {
            for (QuestObjective objective : definition.objectives()) {
                long value = section.getLong("progress." + objective.getId(), 0L);
                if (value < 0L) {
                    throw new IllegalArgumentException("Invalid progress for " + id + "/" + objective.getId());
                }
                value = Math.min(value, objective.required());
                progress.put(objective.getId(), value);
                BigDecimal remainder = FractionalProgress.readRemainder(
                        section.getString("progress-remainders." + objective.getId(), "0"));
                if (value == objective.required()) {
                    remainder = BigDecimal.ZERO;
                }
                if (remainder.signum() > 0) {
                    progressRemainders.put(objective.getId(), remainder);
                }
            }
        }
        total = section.getLong("total", 0L);
        totalRemainder = FractionalProgress.readRemainder(section.getString("total-remainder", "0"));
        premium = section.getBoolean("premium", false);
        booster = section.getDouble("booster.multiplier", 1D);
        boosterUntil = section.getLong("booster.expires-at", 0L);
        if (!Double.isFinite(booster) || booster < 1D || boosterUntil < 0L) {
            throw new IllegalArgumentException("Invalid saved booster");
        }
        if (total < 0L || total == Long.MAX_VALUE && totalRemainder.signum() > 0) {
            throw new IllegalArgumentException("Invalid progression total: " + id);
        }
        ConfigurationSection stages = section.getConfigurationSection("stage-states");
        ConfigurationSection savedRewards = section.getConfigurationSection("stage-rewards");
        if (stages != null) {
            for (String key : stages.getKeys(false)) {
                String value = stages.getString(key, "");
                ConfigurationSection reward = savedRewards == null ? null : savedRewards.getConfigurationSection(key);
                if (!key.matches("(?:free|premium|default)/[1-9][0-9]*")
                        || !Set.of("AVAILABLE", "CLAIMING", "CLAIMED", "REVIEW").contains(value) || reward == null) {
                    throw new IllegalArgumentException("Invalid progression reward state: " + key);
                }
                Integer.parseInt(key.split("/")[1]);
                long required = QuestDefinition.positiveLong(reward, "required", 0L);
                if (!definition.growth() || !definition.tracks().contains(key.split("/")[0])
                        || required > Math.max(total, section.getLong("highest-total", total))) {
                    throw new IllegalArgumentException("Invalid progression reward: " + key);
                }
                stageStates.put(key, value);
                stageRewardKeys.put(key, readRewardKeys(reward, "reward-keys"));
                stageRequirements.put(key, required);
            }
        }
        if (savedRewards != null && !savedRewards.getKeys(false).equals(stageStates.keySet())) {
            throw new IllegalArgumentException("Progression rewards do not match reward states: " + id);
        }
        if (definition.growth()) {
            unlockStages();
        } else if (Set.of("ACTIVE", "COMPLETED").contains(state)) {
            state = complete() ? "COMPLETED" : "ACTIVE";
        }
    }

    public UUID instanceId() {
        return instanceId;
    }

    private static TimeWindow initialWindow(QuestDefinition definition, String season) {
        if (definition.growth() && SeasonManager.seasonManager != null) {
            return SeasonManager.seasonManager.window(definition, season);
        }
        return new TimeWindow(definition.startsAt(), definition.endsAt(),
                definition.settings().getLong("claim-grace-seconds", 0L), false);
    }

    public String season() {
        return season;
    }

    public Instant startsAt() {
        return currentWindow().startsAt();
    }

    public Instant endsAt() {
        return currentWindow().endsAt();
    }

    private TimeWindow currentWindow() {
        return SeasonManager.seasonManager == null || !definition.growth() ? seasonWindow
                : SeasonManager.seasonManager.window(definition, season, seasonWindow);
    }

    public boolean active(Instant now) {
        if (!definition.growth()) {
            return definition.active(now);
        }
        return !paused() && currentWindow().active(now);
    }

    public boolean claimable(Instant now) {
        if (!definition.growth()) {
            return definition.claimable(now);
        }
        return !paused() && currentWindow().claimable(now);
    }

    private boolean paused() {
        return SeasonManager.seasonManager == null ? definition.settings().getBoolean("paused", false)
                : SeasonManager.seasonManager.paused(definition);
    }

    private static List<String> drawRewards(QuestDefinition definition) {
        ConfigurationSection rewards = definition.rewards();
        if (!RewardManager.rewardManager.hasRandom(rewards)) {
            return null;
        }
        return RewardManager.rewardManager.drawKeys(rewards);
    }

    private static List<String> readRewardKeys(ConfigurationSection section, String path) {
        if (!(section.get(path) instanceof List<?> keys)) {
            throw new IllegalArgumentException("Invalid saved reward keys: " + path);
        }
        for (Object key : keys) {
            if (!(key instanceof String text) || text.isBlank()) {
                throw new IllegalArgumentException("Invalid saved reward key: " + key);
            }
        }
        List<String> result = section.getStringList(path);
        if (result.stream().distinct().count() != result.size()) {
            throw new IllegalArgumentException("Duplicate saved reward keys: " + path);
        }
        return List.copyOf(result);
    }

    public ConfigurationSection rewards() {
        if (rewardKeys == null) {
            return definition.rewards();
        }
        return RewardManager.rewardManager.resolveKeys(definition.rewards(), rewardKeys).getConfigurationSection("rewards");
    }

    private void validateSource() {
        if (!Set.of("normal", "period", "scroll", "challenge").contains(source) || expiresAt < 0L) {
            throw new IllegalArgumentException("Invalid quest instance source");
        }
        if (source.equals("normal")) {
            if (!sourceId.isEmpty() || !cycle.isEmpty() || expiresAt != 0L) {
                throw new IllegalArgumentException("Unexpected ordinary quest source metadata");
            }
        } else if (definition.growth() || !sourceId.matches("[a-z0-9_-]+")
                || (source.equals("period") && (!cycle.matches("(?:daily|weekly|monthly|scheduled)_-?[0-9]+") || expiresAt == 0L))
                || (source.equals("challenge") && !cycle.matches("[a-z0-9_-]+"))
                || (source.equals("scroll") && !cycle.isEmpty())) {
            throw new IllegalArgumentException("Invalid assigned quest metadata");
        }
        if (source.equals("scroll")) {
            UUID.fromString(sourceId);
        }
    }

    public String key() {
        if (source.equals("challenge")) {
            return "challenge/" + sourceId + "/" + cycle + "/" + definition.id();
        }
        if (source.equals("period")) {
            return "period/" + sourceId + "/" + cycle + "/" + definition.id();
        }
        if (source.equals("scroll")) {
            return "scroll/" + sourceId + "/" + definition.id();
        }
        return definition.growth() && !season.isEmpty() ? definition.catalogKey() + "/" + season : definition.catalogKey();
    }

    public String source() {
        return source;
    }

    public String sourceId() {
        return sourceId;
    }

    public String cycle() {
        return cycle;
    }

    public long expiresAt() {
        return expiresAt;
    }

    public boolean expired(Instant now) {
        return expiresAt > 0L && now.toEpochMilli() >= expiresAt;
    }

    public QuestDefinition definition() {
        return definition;
    }

    public String state() {
        return state;
    }

    public void state(String value) {
        if (!STATES.contains(value)) {
            throw new IllegalArgumentException("Unknown quest state: " + value);
        }
        state = value;
    }

    public long progress(QuestObjective objective) {
        return progress.getOrDefault(objective.getId(), 0L);
    }

    public boolean advance(QuestObjective objective, long amount) {
        return advance(objective, BigDecimal.valueOf(amount));
    }

    public BigDecimal progressAmount(QuestObjective objective) {
        return BigDecimal.valueOf(progress(objective)).add(progressRemainders.getOrDefault(objective.getId(), BigDecimal.ZERO));
    }

    public boolean advance(QuestObjective objective, BigDecimal amount) {
        long previous = progress(objective);
        if (definition.growth() || !state.equals("ACTIVE") || amount.signum() <= 0 || previous >= objective.required()) {
            return false;
        }
        BigDecimal remainder = progressRemainders.getOrDefault(objective.getId(), BigDecimal.ZERO);
        var result = FractionalProgress.add(previous, remainder, amount, objective.required());
        if (result.whole() == previous && result.remainder().compareTo(remainder) == 0) {
            return false;
        }
        progress.put(objective.getId(), result.whole());
        if (result.remainder().signum() == 0) {
            progressRemainders.remove(objective.getId());
        } else {
            progressRemainders.put(objective.getId(), result.remainder());
        }
        if (complete()) {
            state = "COMPLETED";
        }
        return true;
    }

    public boolean complete() {
        return !definition.growth() && definition.objectives().stream().allMatch(objective -> progress(objective) >= objective.required());
    }

    public long total() {
        return total;
    }

    public BigDecimal totalAmount() {
        return BigDecimal.valueOf(total).add(totalRemainder);
    }

    public boolean premium(Player player) {
        return premium || (definition.settings().getBoolean("premium-by-permission", true) && definition.premium(player, season));
    }

    public void premium(boolean value) {
        premium = value;
    }

    public void booster(double multiplier, long until) {
        if (!Double.isFinite(multiplier) || multiplier < 1D || until < 0L) {
            throw new IllegalArgumentException("Invalid booster");
        }
        booster = multiplier;
        boosterUntil = until;
    }

    public double booster(Instant now) {
        return now.toEpochMilli() < boosterUntil ? booster : 1D;
    }

    public void setTotal(long value) {
        setTotal(BigDecimal.valueOf(value));
    }

    public void setTotal(BigDecimal value) {
        if (!definition.growth() || value.signum() < 0) {
            throw new IllegalArgumentException("Invalid progression total");
        }
        var result = FractionalProgress.add(0L, BigDecimal.ZERO, value, definition.maximumTotal());
        total = result.whole();
        totalRemainder = result.remainder();
        unlockStages();
    }

    public int level() {
        return definition.level(total);
    }

    public boolean addTotal(long amount) {
        return addTotal(BigDecimal.valueOf(amount));
    }

    public boolean addTotal(BigDecimal amount) {
        if (!definition.growth() || amount.signum() <= 0) {
            return false;
        }
        long maximum = Math.max(total, definition.maximumTotal());
        var result = FractionalProgress.add(total, totalRemainder, amount, maximum);
        if (result.whole() == total && result.remainder().compareTo(totalRemainder) == 0) {
            return false;
        }
        total = result.whole();
        totalRemainder = result.remainder();
        unlockStages();
        return true;
    }

    public boolean refreshDefinition(QuestDefinition updated) {
        String currentSeason = SeasonManager.seasonManager == null ? updated.season() : SeasonManager.seasonManager.season(updated);
        if (!definition.growth() || !definition.catalogKey().equals(updated.catalogKey()) || !season.equals(currentSeason)) {
            throw new IllegalArgumentException("Cannot replace a different progression definition");
        }
        TimeWindow updatedWindow = initialWindow(updated, season);
        if ((definition == updated || definition.snapshot().equals(updated.snapshot())) && seasonWindow.equals(updatedWindow)) {
            return false;
        }
        definition = updated;
        seasonWindow = updatedWindow;
        unlockStages();
        return true;
    }

    private void unlockStages() {
        for (Map.Entry<Integer, Long> stage : definition.thresholds().entrySet()) {
            if (total < stage.getValue()) {
                continue;
            }
            for (String track : definition.tracks()) {
                String key = track + "/" + stage.getKey();
                if (!stageRewardKeys.containsKey(key)) {
                    ConfigurationSection configured = definition.stageRewards(stage.getKey(), track);
                    stageRewardKeys.put(key, RewardManager.rewardManager.drawKeys(configured));
                    stageRequirements.put(key, stage.getValue());
                    stageStates.put(key, "AVAILABLE");
                }
            }
        }
    }

    public Map<String, String> stageStates() {
        return Map.copyOf(stageStates);
    }

    public String stageState(String key) {
        return stageStates.getOrDefault(key, "LOCKED");
    }

    public void stageState(String key, String value) {
        if (!stageRewardKeys.containsKey(key) || !Set.of("AVAILABLE", "CLAIMING", "CLAIMED", "REVIEW").contains(value)) {
            throw new IllegalArgumentException("Invalid stage state: " + key);
        }
        stageStates.put(key, value);
    }

    public ConfigurationSection stageReward(String key) {
        List<String> keys = stageRewardKeys.get(key);
        if (keys == null) {
            throw new IllegalArgumentException("Stage is locked: " + key);
        }
        String[] parts = key.split("/");
        ConfigurationSection configured = definition.stageRewards(Integer.parseInt(parts[1]), parts[0]);
        return RewardManager.rewardManager.resolveKeys(configured, keys).getConfigurationSection("rewards");
    }

    public long stageRequired(String key) {
        Long required = stageRequirements.get(key);
        if (required != null) {
            return required;
        }
        String[] parts = key.split("/");
        return definition.thresholds().getOrDefault(Integer.parseInt(parts[1]), 0L);
    }

    public void write(ConfigurationSection section) {
        section.set("instance-id", instanceId.toString());
        section.set("template-id", definition.catalogKey());
        section.set("accepted-at", acceptedAt);
        if (definition.growth()) {
            section.set("season", season);
            section.set("season-time", null);
            ConfigurationSection savedWindow = section.createSection("season-time");
            savedWindow.set("starts-at", startsAt() == null ? null : startsAt().toString());
            savedWindow.set("ends-at", endsAt() == null ? null : endsAt().toString());
            savedWindow.set("claim-grace-seconds", seasonWindow.claimGraceSeconds());
        }
        if (source.equals("scroll")) {
            section.set("objective-ids", definition.objectives().stream().map(QuestObjective::getId).toList());
        }
        section.set("rewards-snapshot", null);
        section.set("reward-keys", rewardKeys);
        section.set("stage-snapshots", null);
        section.set("stage-rewards", null);
        if (definition.growth()) {
            stageRewardKeys.forEach((key, keys) -> {
                section.set("stage-rewards." + key + ".required", stageRequirements.get(key));
                section.set("stage-rewards." + key + ".reward-keys", keys);
            });
        }
        section.set("state", state);
        if (!source.equals("normal")) {
            section.set("source", source);
            section.set("source-id", sourceId);
            section.set("cycle", cycle);
            section.set("expires-at", expiresAt);
        }
        progress.forEach((key, value) -> section.set("progress." + key, value));
        section.set("progress-remainders", null);
        progressRemainders.forEach((key, value) -> section.set("progress-remainders." + key, value.toPlainString()));
        if (definition.growth()) {
            section.set("total", total);
            section.set("total-remainder", totalRemainder.toPlainString());
            long highest = total;
            for (String key : stageRewardKeys.keySet()) {
                highest = Math.max(highest, stageRequired(key));
            }
            section.set("highest-total", highest);
            section.set("premium", premium);
            section.set("booster.multiplier", booster);
            section.set("booster.expires-at", boosterUntil);
            stageStates.forEach((key, value) -> section.set("stage-states." + key, value));
        }
    }
}
