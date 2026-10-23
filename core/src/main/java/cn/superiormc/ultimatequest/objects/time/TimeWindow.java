package cn.superiormc.ultimatequest.objects.time;

import java.time.Instant;
import org.bukkit.configuration.ConfigurationSection;

public record TimeWindow(Instant startsAt, Instant endsAt, long claimGraceSeconds, boolean paused) {

    public TimeWindow {
        if (claimGraceSeconds < 0L || startsAt != null && endsAt != null && !startsAt.isBefore(endsAt)) {
            throw new IllegalArgumentException("Invalid time window");
        }
        if (endsAt != null) {
            endsAt.plusSeconds(claimGraceSeconds);
        }
    }

    public static TimeWindow read(ConfigurationSection config) {
        String start = config.getString("starts-at", "");
        String end = config.getString("ends-at", "");
        return new TimeWindow(start.isBlank() ? null : Instant.parse(start), end.isBlank() ? null : Instant.parse(end),
                config.getLong("claim-grace-seconds", 0L), config.getBoolean("paused", false));
    }

    public boolean active(Instant now) {
        return contains(now, false);
    }

    public boolean claimable(Instant now) {
        return contains(now, true);
    }

    private boolean contains(Instant now, boolean claiming) {
        return !paused && (startsAt == null || !now.isBefore(startsAt))
                && (endsAt == null || now.isBefore(endsAt.plusSeconds(claiming ? claimGraceSeconds : 0L)));
    }
}
