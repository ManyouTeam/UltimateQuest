package cn.superiormc.ultimatequest.utils;

import cn.superiormc.ultimatequest.api.time.ResetSchedule;
import cn.superiormc.ultimatequest.objects.time.CronResetSchedule;
import cn.superiormc.ultimatequest.objects.time.DailyResetSchedule;
import cn.superiormc.ultimatequest.objects.time.FixedResetSchedule;
import cn.superiormc.ultimatequest.objects.time.IntervalResetSchedule;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.configuration.ConfigurationSection;

public final class ResetTimeUtil {

    private static final Map<String, ResetSchedule> CACHE = new ConcurrentHashMap<>();

    private ResetTimeUtil() {
    }

    public static ResetSchedule parse(ConfigurationSection config) {
        String mode = config.getString("reset-mode", "").toUpperCase(Locale.ROOT);
        String time = config.getString("reset-time", "");
        String anchor = config.getString("reset-anchor", "1970-01-01 00:00:00");
        ZoneId zone = ZoneId.systemDefault();
        String key = mode + "\n" + time + "\n" + anchor + "\n" + zone.getId();
        return CACHE.computeIfAbsent(key, ignored -> switch (mode) {
            case "CRON" -> new CronResetSchedule(time, zone);
            case "TIMED" -> new DailyResetSchedule(time, zone);
            case "TIMER" -> new IntervalResetSchedule(time, CommonUtil.stringToTime(anchor).atZone(zone).toInstant());
            case "CUSTOM" -> new FixedResetSchedule(CommonUtil.stringToTime(anchor).atZone(zone).toInstant(),
                    CommonUtil.stringToTime(time).atZone(zone).toInstant());
            default -> throw new IllegalArgumentException("Unknown reset-mode: " + mode);
        });
    }

    public static Instant previous(ConfigurationSection config, Instant now) {
        return parse(config).previous(now);
    }

    public static Instant next(ConfigurationSection config, Instant now) {
        return parse(config).next(now);
    }

    public static void clearCache() {
        CACHE.clear();
    }
}
