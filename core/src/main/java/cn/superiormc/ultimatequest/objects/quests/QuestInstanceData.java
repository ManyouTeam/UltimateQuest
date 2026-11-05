package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

public record QuestInstanceData(UUID instanceId, String questKey, String templateId, long acceptedAt,
        String source, String sourceId, String cycle, long expiresAt, String season, TimeWindow seasonWindow,
        String state, Map<String, BigDecimal> progress, BigDecimal total, boolean premium, double booster,
        long boosterUntil, List<String> rewardKeys, Map<String, QuestStageData> stages, List<String> objectiveIds) {

    public QuestInstanceData {
        Objects.requireNonNull(instanceId, "instanceId");
        Objects.requireNonNull(seasonWindow, "seasonWindow");
        if (questKey == null || questKey.isBlank() || questKey.length() > 512
                || templateId == null || templateId.isBlank() || templateId.length() > 256
                || sourceId == null || sourceId.length() > 128 || cycle == null || cycle.length() > 128
                || season == null || !season.isEmpty() && !season.matches("[a-z0-9_-]{1,128}")
                || !Set.of("normal", "period", "scroll", "challenge").contains(source)
                || !Set.of("ACTIVE", "COMPLETED", "CLAIMING", "CLAIMED", "REVIEW").contains(state)
                || acceptedAt < 0L || expiresAt < 0L || boosterUntil < 0L || !Double.isFinite(booster) || booster < 1D) {
            throw new IllegalArgumentException("Invalid quest instance data");
        }
        Map<String, BigDecimal> amounts = new LinkedHashMap<>();
        progress.forEach((key, value) -> {
            if (!key.matches("[a-z0-9_-]{1,128}")) {
                throw new IllegalArgumentException("Invalid objective ID: " + key);
            }
            amounts.put(key, amount(value));
        });
        progress = Map.copyOf(amounts);
        total = amount(total);
        rewardKeys = copyRewardKeys(rewardKeys);
        stages.forEach((key, value) -> {
            if (!key.equals(value.key())) {
                throw new IllegalArgumentException("Stage key does not match its data");
            }
        });
        stages = Map.copyOf(stages);
        objectiveIds = List.copyOf(objectiveIds);
        if (objectiveIds.size() > 45 || objectiveIds.stream().distinct().count() != objectiveIds.size()
                || objectiveIds.stream().anyMatch(id -> !id.matches("[a-z0-9_-]{1,128}"))) {
            throw new IllegalArgumentException("Invalid scroll objectives");
        }
    }

    public static BigDecimal amount(BigDecimal value) {
        BigDecimal result = Objects.requireNonNull(value, "amount").setScale(6, RoundingMode.UNNECESSARY).stripTrailingZeros();
        if (result.signum() < 0 || result.compareTo(BigDecimal.valueOf(Long.MAX_VALUE)) > 0) {
            throw new IllegalArgumentException("Invalid quest progress: " + value);
        }
        return result;
    }

    public static List<String> copyRewardKeys(List<String> keys) {
        if (keys == null) {
            return null;
        }
        if (keys.stream().anyMatch(key -> key == null || key.isBlank() || key.length() > 256)
                || keys.stream().distinct().count() != keys.size()) {
            throw new IllegalArgumentException("Invalid reward choices");
        }
        return List.copyOf(keys);
    }
}
