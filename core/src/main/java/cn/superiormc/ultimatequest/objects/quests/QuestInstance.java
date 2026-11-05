package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.RewardManager;
import cn.superiormc.ultimatequest.managers.SeasonManager;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.math.BigDecimal;
import java.math.RoundingMode;
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

    private final Map<String, BigDecimal> progress = new LinkedHashMap<>();

    private final Map<String, QuestStageData> stages = new LinkedHashMap<>();

    private final List<String> rewardKeys;

    private BigDecimal total = BigDecimal.ZERO;

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
        stages.putAll(original.stages);
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

    public QuestInstance(QuestInstanceData data, QuestDefinition definition) {
        if (definition == null || !definition.catalogKey().equals(data.templateId())) {
            throw new IllegalArgumentException("Missing or mismatched quest definition: " + data.templateId());
        }
        instanceId = data.instanceId();
        this.definition = definition;
        acceptedAt = data.acceptedAt();
        season = data.season();
        seasonWindow = data.seasonWindow();
        source = data.source();
        sourceId = data.sourceId();
        cycle = data.cycle();
        expiresAt = data.expiresAt();
        rewardKeys = data.rewardKeys();
        state = data.state();
        progress.putAll(data.progress());
        total = data.total();
        premium = data.premium();
        booster = data.booster();
        boosterUntil = data.boosterUntil();
        stages.putAll(data.stages());
        validateSource();
        if (!key().equals(data.questKey())) {
            throw new IllegalArgumentException("Quest key does not match its definition: " + data.questKey());
        }
        if (rewardKeys == null && RewardManager.rewardManager.hasRandom(definition.rewards())) {
            throw new IllegalArgumentException("Missing saved reward choices: " + key());
        }
        if (!definition.growth() && !stages.isEmpty()) {
            throw new IllegalArgumentException("Ordinary task contains progression stages");
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
        } else if (definition.growth() || !source.equals("scroll") && !sourceId.matches("[a-z0-9_-]+")
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
        return progressAmount(objective).longValue();
    }

    public boolean advance(QuestObjective objective, long amount) {
        return advance(objective, BigDecimal.valueOf(amount));
    }

    public BigDecimal progressAmount(QuestObjective objective) {
        return progress.getOrDefault(objective.getId(), BigDecimal.ZERO);
    }

    public boolean advance(QuestObjective objective, BigDecimal amount) {
        BigDecimal previous = progressAmount(objective);
        if (definition.growth() || !state.equals("ACTIVE") || amount.signum() <= 0
                || previous.compareTo(BigDecimal.valueOf(objective.required())) >= 0) {
            return false;
        }
        BigDecimal updated = QuestInstanceData.amount(previous.add(amount).setScale(6, RoundingMode.HALF_UP)
                .min(BigDecimal.valueOf(objective.required())));
        if (updated.compareTo(previous) == 0) {
            return false;
        }
        progress.put(objective.getId(), updated);
        if (complete()) {
            state = "COMPLETED";
        }
        return true;
    }

    public boolean complete() {
        return !definition.growth() && definition.objectives().stream().allMatch(objective -> progress(objective) >= objective.required());
    }

    public long total() {
        return total.longValue();
    }

    public BigDecimal totalAmount() {
        return total;
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
        total = QuestInstanceData.amount(value.min(BigDecimal.valueOf(definition.maximumTotal())));
        unlockStages();
    }

    public int level() {
        return definition.level(total());
    }

    public boolean addTotal(long amount) {
        return addTotal(BigDecimal.valueOf(amount));
    }

    public boolean addTotal(BigDecimal amount) {
        if (!definition.growth() || amount.signum() <= 0) {
            return false;
        }
        BigDecimal maximum = total.max(BigDecimal.valueOf(definition.maximumTotal()));
        BigDecimal updated = QuestInstanceData.amount(total.add(amount).setScale(6, RoundingMode.HALF_UP).min(maximum));
        if (updated.compareTo(total) == 0) {
            return false;
        }
        total = updated;
        unlockStages();
        return true;
    }

    public boolean refreshDefinition(QuestDefinition updated) {
        String currentSeason = SeasonManager.seasonManager == null ? updated.season() : SeasonManager.seasonManager.season(updated);
        if (!definition.growth() || !definition.catalogKey().equals(updated.catalogKey()) || !season.equals(currentSeason)) {
            throw new IllegalArgumentException("Cannot replace a different progression definition");
        }
        TimeWindow updatedWindow = initialWindow(updated, season);
        if (definition == updated && seasonWindow.equals(updatedWindow)) {
            return false;
        }
        definition = updated;
        seasonWindow = updatedWindow;
        unlockStages();
        return true;
    }

    private void unlockStages() {
        for (Map.Entry<Integer, Long> stage : definition.thresholds().entrySet()) {
            if (total.compareTo(BigDecimal.valueOf(stage.getValue())) < 0) {
                continue;
            }
            for (String track : definition.tracks()) {
                String key = track + "/" + stage.getKey();
                if (!stages.containsKey(key)) {
                    ConfigurationSection configured = definition.stageRewards(stage.getKey(), track);
                    stages.put(key, new QuestStageData(track, stage.getKey(), "AVAILABLE", stage.getValue(),
                            RewardManager.rewardManager.drawKeys(configured)));
                }
            }
        }
    }

    public Map<String, String> stageStates() {
        Map<String, String> result = new LinkedHashMap<>();
        stages.forEach((key, stage) -> result.put(key, stage.state()));
        return Map.copyOf(result);
    }

    public String stageState(String key) {
        QuestStageData stage = stages.get(key);
        return stage == null ? "LOCKED" : stage.state();
    }

    public void stageState(String key, String value) {
        QuestStageData stage = stages.get(key);
        if (stage == null) {
            throw new IllegalArgumentException("Stage is locked: " + key);
        }
        stages.put(key, stage.withState(value));
    }

    public ConfigurationSection stageReward(String key) {
        QuestStageData stage = stages.get(key);
        if (stage == null) {
            throw new IllegalArgumentException("Stage is locked: " + key);
        }
        ConfigurationSection configured = definition.stageRewards(stage.level(), stage.track());
        return RewardManager.rewardManager.resolveKeys(configured, stage.rewardKeys()).getConfigurationSection("rewards");
    }

    public long stageRequired(String key) {
        QuestStageData stage = stages.get(key);
        if (stage != null) {
            return stage.required();
        }
        String[] parts = key.split("/");
        return definition.thresholds().getOrDefault(Integer.parseInt(parts[1]), 0L);
    }

    public QuestInstanceData data() {
        return new QuestInstanceData(instanceId, key(), definition.catalogKey(), acceptedAt, source, sourceId,
                cycle, expiresAt, season, seasonWindow, state, progress, total, premium, booster, boosterUntil,
                rewardKeys, stages, source.equals("scroll")
                        ? definition.objectives().stream().map(QuestObjective::getId).toList() : List.of());
    }
}
