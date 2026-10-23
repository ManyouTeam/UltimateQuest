package cn.superiormc.ultimatequest.objects.time;

import cn.superiormc.ultimatequest.api.time.ResetSchedule;
import java.time.Instant;

public final class IntervalResetSchedule implements ResetSchedule {

    private final Instant anchor;

    private final long seconds;

    public IntervalResetSchedule(String configured, Instant anchor) {
        String[] parts = configured.split(":", -1);
        if (parts.length != 3 && parts.length != 4) {
            throw new IllegalArgumentException("TIMER requires [days:]hours:minutes:seconds");
        }
        long[] units = parts.length == 4 ? new long[] {86400L, 3600L, 60L, 1L}
                : new long[] {3600L, 60L, 1L};
        long duration = 0L;
        for (int index = 0; index < parts.length; index++) {
            long value = Long.parseLong(parts[index]);
            if (value < 0L) {
                throw new IllegalArgumentException("Negative reset duration");
            }
            duration = Math.addExact(duration, Math.multiplyExact(value, units[index]));
        }
        if (duration == 0L) {
            throw new IllegalArgumentException("Reset duration must be positive");
        }
        seconds = duration;
        this.anchor = anchor;
    }

    @Override
    public Instant previous(Instant now) {
        long cycle = Math.floorDiv(Math.subtractExact(now.getEpochSecond(), anchor.getEpochSecond()), seconds);
        return anchor.plusSeconds(Math.multiplyExact(cycle, seconds));
    }

    @Override
    public Instant next(Instant now) {
        return previous(now).plusSeconds(seconds);
    }
}
