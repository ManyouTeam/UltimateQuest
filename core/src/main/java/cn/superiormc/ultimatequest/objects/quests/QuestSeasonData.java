package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.util.Map;

public record QuestSeasonData(long revision, String season, Long cycleStart, Boolean paused,
        boolean rotationDisabled, boolean windowOverride, TimeWindow window, double booster, long boosterUntil,
        Map<String, TimeWindow> history) {

    public QuestSeasonData {
        if (revision < 0L || season == null || !season.isEmpty() && !season.matches("[a-z0-9_-]{1,128}")
                || windowOverride && window == null || !Double.isFinite(booster) || booster < 1D || boosterUntil < 0L) {
            throw new IllegalArgumentException("Invalid season data");
        }
        history = Map.copyOf(history);
    }

    public static QuestSeasonData empty() {
        return new QuestSeasonData(0L, "", null, null, false, false, null, 1D, 0L, Map.of());
    }

    public QuestSeasonData withRevision(long value) {
        return new QuestSeasonData(value, season, cycleStart, paused, rotationDisabled, windowOverride, window,
                booster, boosterUntil, history);
    }
}
