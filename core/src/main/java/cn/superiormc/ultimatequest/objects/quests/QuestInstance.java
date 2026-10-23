package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import java.time.Instant;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class QuestInstance {

    private static final Set<String> STATES = Set.of("ACTIVE", "COMPLETED", "CLAIMING", "CLAIMED", "REVIEW");

    private final UUID instanceId;

    private QuestDefinition definition;

    private final long acceptedAt;

    private final Map<String, Long> progress = new LinkedHashMap<>();

    private final Map<String, BigDecimal> progressRemainders = new LinkedHashMap<>();

    private BigDecimal totalRemainder = BigDecimal.ZERO;

    private final Map<String, String> stageStates = new LinkedHashMap<>();

    private final Map<String, String> stageSnapshots = new LinkedHashMap<>();

    private final String rewardsSnapshot;

    private long total;

    private boolean premium;

    private double booster = 1D;

    private long boosterUntil;

    private String state;

    private final String source;

    private final String sourceId;

    private final String cycle;

    private final long expiresAt;

    public QuestInstance(QuestDefinition definition) {
        this(definition, "normal", "", "", 0L);
    }

    public QuestInstance(QuestDefinition definition, String source, String sourceId, String cycle, long expiresAt) {
        instanceId = UUID.randomUUID();
        this.definition = definition;
        acceptedAt = System.currentTimeMillis();
        state = "ACTIVE";
        this.source = source;
        this.sourceId = sourceId;
        this.cycle = cycle;
        this.expiresAt = expiresAt;
        rewardsSnapshot = drawRewards(definition);
        validateSource();
    }

    public QuestInstance(String id, ConfigurationSection section) throws Exception {
        this(id, section, QuestManager.questManager.resolveDefinition(id, section));
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
        source = section.getString("source", "normal");
        sourceId = section.getString("source-id", "");
        cycle = section.getString("cycle", "");
        expiresAt = section.getLong("expires-at", 0L);
        String savedRewards = section.getString("rewards-snapshot");
        if (savedRewards == null || savedRewards.isBlank()) {
            rewardsSnapshot = drawRewards(definition);
        } else {
            YamlConfiguration parsed = new YamlConfiguration();
            parsed.loadFromString(savedRewards);
            RewardManager.rewardManager.validate(parsed.getConfigurationSection("rewards"));
            rewardsSnapshot = savedRewards;
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
        if (stages != null) {
            for (String key : stages.getKeys(false)) {
                String value = stages.getString(key, "");
                String snapshot = section.getString("stage-snapshots." + key);
                if (!key.matches("(?:free|premium|default)/[1-9][0-9]*")
                        || !Set.of("AVAILABLE", "CLAIMING", "CLAIMED", "REVIEW").contains(value) || snapshot == null) {
                    throw new IllegalArgumentException("Invalid progression reward state: " + key);
                }
                Integer.parseInt(key.split("/")[1]);
                YamlConfiguration reward = new YamlConfiguration();
                reward.loadFromString(snapshot);
                if (!definition.growth() || !definition.tracks().contains(key.split("/")[0])
                        || !reward.isConfigurationSection("rewards")
                        || QuestDefinition.positiveLong(reward, "required", 0L) > Math.max(total, section.getLong("highest-total", total))) {
                    throw new IllegalArgumentException("Invalid progression reward snapshot: " + key);
                }
                RewardManager.rewardManager.validate(reward.getConfigurationSection("rewards"));
                stageStates.put(key, value);
                stageSnapshots.put(key, snapshot);
            }
        }
        ConfigurationSection snapshots = section.getConfigurationSection("stage-snapshots");
        if (snapshots != null && !snapshots.getKeys(false).equals(stageStates.keySet())) {
            throw new IllegalArgumentException("Progression snapshots do not match reward states: " + id);
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

    /**
     * Rolls every random reward once so the outcome is fixed for this instance.
     * Returns null when the definition has no random rewards to draw.
     */
    private static String drawRewards(QuestDefinition definition) {
        ConfigurationSection rewards = definition.rewards();
        if (!RewardManager.rewardManager.hasRandom(rewards)) {
            return null;
        }
        return RewardManager.rewardManager.resolveSnapshot(rewards).saveToString();
    }

    /**
     * Returns the rewards that should actually be granted: the draw fixed at accept time,
     * or the plain definition rewards when nothing had to be rolled.
     */
    public ConfigurationSection rewards() {
        if (rewardsSnapshot == null) {
            return definition.rewards();
        }
        try {
            YamlConfiguration rewards = new YamlConfiguration();
            rewards.loadFromString(rewardsSnapshot);
            return rewards.getConfigurationSection("rewards");
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid reward snapshot: " + definition.id(), exception);
        }
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
        return definition.storageKey();
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
        return premium || (definition.settings().getBoolean("premium-by-permission", true) && definition.premium(player));
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
        if (!definition.growth() || !definition.storageKey().equals(updated.storageKey())) {
            throw new IllegalArgumentException("Cannot replace a different progression definition");
        }
        if (definition == updated || definition.snapshot().equals(updated.snapshot())) {
            return false;
        }
        definition = updated;
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
                if (!stageSnapshots.containsKey(key)) {
                    YamlConfiguration reward = new YamlConfiguration();
                    reward.set("required", stage.getValue());
                    ConfigurationSection configured = definition.stageRewards(stage.getKey(), track);
                    // Draw random rewards now so the outcome is fixed for this stage.
                    if (RewardManager.rewardManager.hasRandom(configured)) {
                        configured = RewardManager.rewardManager.resolveSnapshot(configured).getConfigurationSection("rewards");
                    }
                    ConfigurationSection target = reward.createSection("rewards");
                    if (configured != null) {
                        configured.getValues(true).forEach((path, value) -> {
                            if (!(value instanceof ConfigurationSection)) {
                                target.set(path, value);
                            }
                        });
                    }
                    stageSnapshots.put(key, reward.saveToString());
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
        if (!stageSnapshots.containsKey(key) || !Set.of("AVAILABLE", "CLAIMING", "CLAIMED", "REVIEW").contains(value)) {
            throw new IllegalArgumentException("Invalid stage state: " + key);
        }
        stageStates.put(key, value);
    }

    public ConfigurationSection stageReward(String key) {
        String snapshot = stageSnapshots.get(key);
        if (snapshot == null) {
            throw new IllegalArgumentException("Stage is locked: " + key);
        }
        try {
            YamlConfiguration reward = new YamlConfiguration();
            reward.loadFromString(snapshot);
            return reward.getConfigurationSection("rewards");
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stage reward snapshot: " + key, exception);
        }
    }

    public long stageRequired(String key) {
        String snapshot = stageSnapshots.get(key);
        if (snapshot == null) {
            String[] parts = key.split("/");
            return definition.thresholds().getOrDefault(Integer.parseInt(parts[1]), 0L);
        }
        try {
            YamlConfiguration reward = new YamlConfiguration();
            reward.loadFromString(snapshot);
            return reward.getLong("required", 0L);
        } catch (Exception exception) {
            throw new IllegalStateException("Invalid stage reward snapshot: " + key, exception);
        }
    }

    public void write(ConfigurationSection section) {
        section.set("instance-id", instanceId.toString());
        section.set("template-id", definition.catalogKey());
        section.set("accepted-at", acceptedAt);
        if (source.equals("scroll")) {
            section.set("objective-ids", definition.objectives().stream().map(QuestObjective::getId).toList());
        }
        if (rewardsSnapshot != null) {
            section.set("rewards-snapshot", rewardsSnapshot);
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
            for (String key : stageSnapshots.keySet()) {
                highest = Math.max(highest, stageRequired(key));
            }
            section.set("highest-total", highest);
            section.set("premium", premium);
            section.set("booster.multiplier", booster);
            section.set("booster.expires-at", boosterUntil);
            stageStates.forEach((key, value) -> section.set("stage-states." + key, value));
            stageSnapshots.forEach((key, value) -> section.set("stage-snapshots." + key, value));
        }
    }
}
