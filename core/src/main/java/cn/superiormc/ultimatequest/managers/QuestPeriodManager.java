package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.ResetTimeUtil;
import java.time.ZoneId;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.Random;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class QuestPeriodManager extends AbstractManager {

    public static QuestPeriodManager questPeriodManager;

    private Map<String, YamlConfiguration> periods = Map.of();

    private SchedulerUtil timer;

    public QuestPeriodManager() {
        questPeriodManager = this;
    }

    public Map<String, YamlConfiguration> load(Map<String, YamlConfiguration> pools,
            Map<String, YamlConfiguration> objectives) throws Exception {
        return load(pools, objectives, RewardManager.rewardManager.loadPools());
    }

    public Map<String, YamlConfiguration> load(Map<String, YamlConfiguration> pools,
            Map<String, YamlConfiguration> objectives, Map<String, YamlConfiguration> rewardPools) throws Exception {
        Map<String, YamlConfiguration> loaded = ConfigurationFileUtil.readFolder("periods");
        for (Map.Entry<String, YamlConfiguration> entry : loaded.entrySet()) {
            YamlConfiguration config = entry.getValue();
            RewardManager.rewardManager.resolveReferences(config, rewardPools);
            RewardManager.rewardManager.validate(config.getConfigurationSection("task.rewards"));
            ConfigurationSection source = config.getConfigurationSection("source");
            if (source == null) {
                throw new IllegalArgumentException("Periodic quest " + entry.getKey()
                        + " requires a source section");
            }
            if (source.contains("pool")) {
                try {
                    QuestPoolManager.draw(source, pools, objectives, new Random(0L));
                } catch (IllegalArgumentException exception) {
                    throw new IllegalArgumentException("Invalid periodic quest pool in " + entry.getKey() + ": "
                            + exception.getMessage(), exception);
                }
            } else {
                YamlConfiguration resolved = QuestObjective.resolve(
                        entry.getKey(), source, objectives);
                new QuestObjective(entry.getKey(), resolved);
            }
            start(config, Instant.now());
            end(config, Instant.now());
            TimeWindow.read(config);
        }
        return loaded;
    }

    public synchronized void install(Map<String, YamlConfiguration> loaded) {
        periods = Map.copyOf(loaded);
    }

    public synchronized Map<String, YamlConfiguration> definitions() {
        return periods;
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

    @Override
    public void onInit() {
        timer = SchedulerUtil.runTaskTimer(() -> {
            QuestManager coordinator = QuestManager.questManager;
            if (coordinator != null) {
                coordinator.refreshSeasons();
            }
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
