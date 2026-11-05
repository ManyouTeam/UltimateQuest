package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class ObjectScrollRecord {

    private final UUID token;
    private final String profile;
    private final String rarity;
    private final String difficulty;
    private final long durationSeconds;
    private final List<String> objectiveIds;
    private long revision;
    private UUID owner;
    private String state;
    private QuestInstanceData instance;

    public ObjectScrollRecord(UUID token, String profile, String rarity, String difficulty, long durationSeconds,
            List<String> objectiveIds, long revision, UUID owner, String state, QuestInstanceData instance) {
        if (token == null || !profile.matches("[a-z0-9_-]{1,128}") || !rarity.matches("[a-z0-9_-]{1,128}")
                || !difficulty.matches("[a-z0-9_-]{1,128}") || durationSeconds < 0L || revision < 0L
                || objectiveIds.isEmpty() || objectiveIds.size() > 45
                || objectiveIds.stream().distinct().count() != objectiveIds.size()) {
            throw new IllegalArgumentException("Invalid scroll data");
        }
        this.token = token;
        this.profile = profile;
        this.rarity = rarity;
        this.difficulty = difficulty;
        this.durationSeconds = durationSeconds;
        this.objectiveIds = List.copyOf(objectiveIds);
        this.revision = revision;
        this.owner = owner;
        state(state);
        instance(instance);
    }

    public ObjectScrollRecord copy() {
        return new ObjectScrollRecord(token, profile, rarity, difficulty, durationSeconds, objectiveIds,
                revision, owner, state, instance);
    }

    public List<QuestDefinition> readQuests() {
        return List.of(ConfigManager.configManager.getScroll(profile).createDefinition(objectiveIds));
    }

    public UUID token() {
        return token;
    }

    public String profile() {
        return profile;
    }

    public String rarity() {
        return rarity;
    }

    public String difficulty() {
        return difficulty;
    }

    public long durationSeconds() {
        return durationSeconds;
    }

    public List<String> objectiveIds() {
        return objectiveIds;
    }

    public long revision() {
        return revision;
    }

    public void revision(long value) {
        if (value < 0L) {
            throw new IllegalArgumentException("Invalid scroll version");
        }
        revision = value;
    }

    public UUID owner() {
        return owner;
    }

    public void owner(UUID value) {
        owner = value;
    }

    public String state() {
        return state;
    }

    public void state(String value) {
        if (!Set.of("ISSUED", "ACTIVE", "REDEEMING", "REDEEMED", "REVIEW").contains(value)) {
            throw new IllegalArgumentException("Invalid scroll state: " + value);
        }
        state = value;
    }

    public QuestInstanceData instance() {
        return instance;
    }

    public void instance(QuestInstanceData value) {
        if (value != null && (!value.source().equals("scroll") || !value.sourceId().equals(token.toString()))) {
            throw new IllegalArgumentException("Scroll instance does not match its token");
        }
        instance = value;
    }
}
