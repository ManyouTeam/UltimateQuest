package cn.superiormc.ultimatequest.objects.quests;

import java.util.List;
import java.util.Set;

public record QuestStageData(String track, int level, String state, long required, List<String> rewardKeys) {

    public QuestStageData {
        if (!Set.of("free", "premium", "default").contains(track) || level <= 0 || required < 0L
                || !Set.of("AVAILABLE", "CLAIMING", "CLAIMED", "REVIEW").contains(state)) {
            throw new IllegalArgumentException("Invalid quest stage");
        }
        rewardKeys = QuestInstanceData.copyRewardKeys(rewardKeys);
        if (rewardKeys == null) {
            throw new IllegalArgumentException("Missing stage reward choices");
        }
    }

    public String key() {
        return track + "/" + level;
    }

    public QuestStageData withState(String value) {
        return new QuestStageData(track, level, value, required, rewardKeys);
    }
}
