package cn.superiormc.ultimatequest.objects.time;

import cn.superiormc.ultimatequest.api.time.ResetSchedule;
import java.time.Instant;

public final class FixedResetSchedule implements ResetSchedule {

    private final Instant anchor;

    private final Instant reset;

    public FixedResetSchedule(Instant anchor, Instant reset) {
        if (!anchor.isBefore(reset)) {
            throw new IllegalArgumentException("CUSTOM reset must be after reset-anchor");
        }
        this.anchor = anchor;
        this.reset = reset;
    }

    @Override
    public Instant previous(Instant now) {
        return now.isBefore(reset) ? anchor : reset;
    }

    @Override
    public Instant next(Instant now) {
        return now.isBefore(reset) ? reset : Instant.ofEpochMilli(Long.MAX_VALUE);
    }
}
