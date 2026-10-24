package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.ResetTimeUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;

public final class QuestPeriodManager extends AbstractManager {

    public static QuestPeriodManager questPeriodManager;

    private SchedulerUtil timer;

    public QuestPeriodManager() {
        questPeriodManager = this;
    }

    public static ZonedDateTime start(ConfigurationSection config, Instant now) {
        ZonedDateTime start = ResetTimeUtil.previous(config, now).atZone(ZoneId.systemDefault());
        String configured = config.getString("starts-at", "");
        if (!configured.isBlank()) {
            Instant begins = Instant.parse(configured);
            if (!begins.isAfter(now) && begins.isAfter(start.toInstant())) {
                return begins.atZone(start.getZone());
            }
        }
        return start;
    }

    public static Instant end(ConfigurationSection config, Instant now) {
        Instant end = ResetTimeUtil.next(config, now);
        String configured = config.getString("ends-at", "");
        if (!configured.isBlank()) {
            Instant finishes = Instant.parse(configured);
            if (finishes.isAfter(now) && finishes.isBefore(end)) {
                return finishes;
            }
        }
        return end;
    }

    public static String cycle(ConfigurationSection config, Instant now) {
        return "scheduled_" + start(config, now).toEpochSecond();
    }

    public static ZonedDateTime start(String id, ConfigurationSection config, Instant now) {
        ZonedDateTime start = ResetTimeUtil.previous(config, now).atZone(ZoneId.systemDefault());
        TimeWindow window = SeasonManager.seasonManager.periodWindow(id, config);
        Instant begins = window.startsAt();
        if (begins != null && !begins.isAfter(now) && begins.isAfter(start.toInstant())) {
            return begins.atZone(start.getZone());
        }
        return start;
    }

    public static Instant end(String id, ConfigurationSection config, Instant now) {
        Instant end = ResetTimeUtil.next(config, now);
        Instant finishes = SeasonManager.seasonManager.periodWindow(id, config).endsAt();
        return finishes != null && finishes.isAfter(now) && finishes.isBefore(end) ? finishes : end;
    }

    public static String cycle(String id, ConfigurationSection config, Instant now) {
        return "scheduled_" + start(id, config, now).toEpochSecond();
    }

    @Override
    public void onInit() {
        timer = SchedulerUtil.runTaskTimer(() -> {
            SeasonManager.seasonManager.refresh();
            for (var player : Bukkit.getOnlinePlayers()) {
                SchedulerUtil.runSync(player, () -> {
                    QuestManager manager = QuestManager.questManager;
                    if (manager != null) {
                        manager.refreshGrowth(player);
                        manager.refreshPeriods(player);
                        QuestScrollManager.questScrollManager.synchronize(player);
                    }
                });
            }
        }, 20L, 20L);
    }

    @Override
    public void onPluginDisable() {
        if (timer != null) {
            timer.cancel();
        }
    }
}
