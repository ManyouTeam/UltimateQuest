package cn.superiormc.ultimatequest.api.progression;

import java.util.Map;

public record ProgressionView(String type, String definitionId, String season, long total, int level,
                              Map<String, String> rewards) {

    public ProgressionView {
        rewards = Map.copyOf(rewards);
    }
}
