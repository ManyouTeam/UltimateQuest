package cn.superiormc.ultimatequest.api.progression;

import java.util.UUID;

public record ProgressionEvent(String action, UUID playerId, String type, String definitionId,
                               String season, long total, int level, String stage) {
}
