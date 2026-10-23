package cn.superiormc.ultimatequest.objects.time;

import cn.superiormc.ultimatequest.api.time.ResetSchedule;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

public final class DailyResetSchedule implements ResetSchedule {

    private final List<LocalTime> times;

    private final ZoneId zone;

    public DailyResetSchedule(String configured, ZoneId zone) {
        times = Arrays.stream(configured.split(";;", -1)).map(LocalTime::parse).distinct().sorted().toList();
        this.zone = zone;
    }

    @Override
    public Instant previous(Instant now) {
        return boundary(now, false);
    }

    @Override
    public Instant next(Instant now) {
        return boundary(now, true);
    }

    private Instant boundary(Instant now, boolean next) {
        Instant result = null;
        LocalDate date = now.atZone(zone).toLocalDate();
        for (LocalTime time : times) {
            Instant candidate = date.atTime(time).atZone(zone).toInstant();
            if (next && !candidate.isAfter(now)) {
                candidate = date.plusDays(1L).atTime(time).atZone(zone).toInstant();
            } else if (!next && candidate.isAfter(now)) {
                candidate = date.minusDays(1L).atTime(time).atZone(zone).toInstant();
            }
            if (result == null || (next ? candidate.isBefore(result) : candidate.isAfter(result))) {
                result = candidate;
            }
        }
        return result;
    }
}
