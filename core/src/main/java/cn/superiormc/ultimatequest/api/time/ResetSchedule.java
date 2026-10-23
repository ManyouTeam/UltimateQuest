package cn.superiormc.ultimatequest.api.time;

import java.time.Instant;

public interface ResetSchedule {

    Instant previous(Instant now);

    Instant next(Instant now);
}
