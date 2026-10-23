package cn.superiormc.ultimatequest.objects.time;

import cn.superiormc.ultimatequest.api.time.ResetSchedule;
import com.cronutils.model.Cron;
import com.cronutils.model.CronType;
import com.cronutils.model.definition.CronDefinitionBuilder;
import com.cronutils.model.time.ExecutionTime;
import com.cronutils.parser.CronParser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class CronResetSchedule implements ResetSchedule {

    private final ExecutionTime execution;

    private final ZoneId zone;

    public CronResetSchedule(String expression, ZoneId zone) {
        CronParser parser = new CronParser(CronDefinitionBuilder.instanceDefinitionFor(CronType.QUARTZ));
        Cron cron = parser.parse(expression);
        cron.validate();
        execution = ExecutionTime.forCron(cron);
        this.zone = zone;
    }

    @Override
    public Instant previous(Instant now) {
        ZonedDateTime current = now.atZone(zone).withNano(0);
        if (execution.isMatch(current)) {
            return current.toInstant();
        }
        return execution.lastExecution(current).orElseThrow(() ->
                new IllegalArgumentException("Reset cron has no previous execution")).toInstant();
    }

    @Override
    public Instant next(Instant now) {
        return execution.nextExecution(now.atZone(zone)).orElseThrow(() ->
                new IllegalArgumentException("Reset cron has no next execution")).toInstant();
    }
}
