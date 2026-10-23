package cn.superiormc.ultimatequest.objects.quests;

import java.util.Objects;
import java.util.UUID;

public record QuestPlayerData(UUID playerUUID, String playerName, long revision, String payload) {

    public QuestPlayerData {
        Objects.requireNonNull(playerUUID, "playerUUID");
        Objects.requireNonNull(playerName, "playerName");
        Objects.requireNonNull(payload, "payload");
        if (revision < 0L) {
            throw new IllegalArgumentException("Negative player data revision");
        }
    }

    public String storageId() {
        return playerUUID.toString();
    }

    public String dataFileName() {
        return playerUUID + ".yml";
    }
}
