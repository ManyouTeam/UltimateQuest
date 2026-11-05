package cn.superiormc.ultimatequest.objects.quests;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record QuestPlayerData(UUID playerUUID, String playerName, long revision,
        Map<String, QuestInstanceData> quests, Map<String, String> periodRolls) {

    public QuestPlayerData {
        Objects.requireNonNull(playerUUID, "playerUUID");
        Objects.requireNonNull(playerName, "playerName");
        if (revision < 0L || playerName.length() > 32) {
            throw new IllegalArgumentException("Invalid player name or data revision");
        }
        quests.forEach((key, value) -> {
            if (!key.equals(value.questKey())) {
                throw new IllegalArgumentException("Quest key does not match its data");
            }
        });
        quests = Map.copyOf(quests);
        periodRolls = Map.copyOf(periodRolls);
        periodRolls.forEach((group, cycle) -> {
            if (!group.matches("[a-z0-9_-]{1,128}") || !cycle.matches("(?:daily|weekly|monthly|scheduled)_-?[0-9]+")) {
                throw new IllegalArgumentException("Invalid period assignment");
            }
        });
    }

    public static QuestPlayerData empty(UUID id) {
        return new QuestPlayerData(id, "", 0L, Map.of(), Map.of());
    }

    public QuestPlayerData withRevision(long value) {
        return new QuestPlayerData(playerUUID, playerName, value, quests, periodRolls);
    }

    public String storageId() {
        return playerUUID.toString();
    }

    public String dataFileName() {
        return playerUUID + ".yml";
    }
}
