package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.time.ObjectSeason;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class SeasonManager {

    public static SeasonManager seasonManager;

    private volatile YamlConfiguration data = new YamlConfiguration();

    private YamlConfiguration working;

    private String originalData;

    private Map<String, TimeWindow> workingWindows;

    private Instant candidateNext;

    private volatile Instant nextCheck = Instant.MAX;

    private final AtomicBoolean refreshing = new AtomicBoolean();

    public SeasonManager() {
        seasonManager = this;
    }

    public void editSeason(CommandSender sender, String type, String id,
            String operation, List<String> values) {
        DatabaseExecutor.executePlayerTask("server:seasons", () -> {
            synchronized (ConfigManager.configManager) {
                try {
                    validateTarget(type, id);
                    beginReload();
                    prepareDefinitions();
                    editState(type, id, operation, values);
                    workingWindows.clear();
                    candidateNext = Instant.MAX;
                    prepareDefinitions();
                    commitReload();
                } catch (Exception exception) {
                    cancelReload();
                    throw new IllegalStateException("Could not edit season " + id + ": " + exception.getMessage(), exception);
                }
            }
        }).whenComplete((ignored, failure) -> {
            Runnable feedback = () -> {
                if (failure != null) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: " + failure.getMessage());
                    LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
                } else {
                    LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-saved");
                    TextUtil.sendMessage(null, "§e[UltimateQuest] §f" + sender.getName() + " updated season " + id + ": " + operation);
                }
            };
            if (sender instanceof Player player) {
                SchedulerUtil.runSync(player, feedback);
            } else {
                SchedulerUtil.runSync(feedback);
            }
        });
    }

    private void validateTarget(String type, String id) {
        if (!id.matches("[a-z0-9_-]+") || !Set.of("battlepass", "milestone", "normal", "period").contains(type)) {
            throw new IllegalArgumentException("Invalid season target");
        }
        boolean exists = type.equals("period") ? ConfigManager.configManager.getPeriod(id) != null
                : ConfigManager.configManager.getDefinitions().values().stream()
                        .anyMatch(definition -> definition.type().equals(type) && definition.id().equals(id));
        if (!exists) {
            throw new IllegalArgumentException("Unknown season target: " + type + "/" + id);
        }
    }

    private void editState(String type, String id, String operation, List<String> values) {
        String path = statePath(type, id);
        Instant now = Instant.now();
        switch (operation) {
            case "pause", "resume" -> {
                requireValues(values, 0);
                working.set(path + ".paused", operation.equals("pause"));
            }
            case "end" -> {
                requireValues(values, 0);
                TimeWindow window = targetWindow(type, id);
                working.set(path + ".starts-at", window.startsAt() != null && window.startsAt().isBefore(now)
                        ? window.startsAt().toString() : "");
                working.set(path + ".ends-at", now.toString());
                working.set(path + ".rotation-disabled", true);
            }
            case "start" -> {
                requireValues(values, 2);
                long seconds = Long.parseLong(values.get(1));
                if (seconds <= 0L) {
                    throw new IllegalArgumentException("Season duration must be positive");
                }
                startSeason(type, id, values.get(0), now, now.plusSeconds(seconds));
            }
            case "schedule" -> {
                requireValues(values, 3);
                startSeason(type, id, values.get(0), Instant.parse(values.get(1)), Instant.parse(values.get(2)));
            }
            case "globalboost" -> {
                requireValues(values, 2);
                double multiplier = Double.parseDouble(values.get(0));
                long seconds = Long.parseLong(values.get(1));
                if (!Double.isFinite(multiplier) || multiplier < 1D || seconds <= 0L) {
                    throw new IllegalArgumentException("Invalid booster");
                }
                working.set(path + ".global-booster.multiplier", multiplier);
                working.set(path + ".global-booster.expires-at", now.plusSeconds(seconds).toEpochMilli());
            }
            default -> {
                throw new IllegalArgumentException("Unknown season operation");
            }
        }
    }

    private void requireValues(List<String> values, int expected) {
        if (values.size() != expected) {
            throw new IllegalArgumentException("Invalid season arguments");
        }
    }

    private void startSeason(String type, String id, String season, Instant start, Instant end) {
        if (!season.matches("[a-z0-9_-]+") || !start.isBefore(end)) {
            throw new IllegalArgumentException("Invalid season ID or time window");
        }
        String path = statePath(type, id);
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().values().stream()
                .filter(candidate -> candidate.type().equals(type) && candidate.id().equals(id)).findFirst().orElse(null);
        String previous = definition == null ? working.getString(path + ".season", "")
                : currentSeason(definition, working, targetWindow(type, id));
        if (season.equals(previous) || working.contains(path + ".history." + season)) {
            throw new IllegalArgumentException("Use a new season ID");
        }
        if (!previous.isEmpty()) {
            TimeWindow window = targetWindow(type, id);
            Instant now = Instant.now();
            Instant previousEnd = window.endsAt() == null || window.endsAt().isAfter(now) ? now : window.endsAt();
            Instant previousStart = window.startsAt() != null && window.startsAt().isBefore(previousEnd) ? window.startsAt() : null;
            saveWindow(path + ".history." + previous,
                    new TimeWindow(previousStart, previousEnd, window.claimGraceSeconds(), false));
        }
        working.set(path + ".season", season);
        working.set(path + ".starts-at", start.toString());
        working.set(path + ".ends-at", end.toString());
        working.set(path + ".rotation-disabled", true);
        working.set(path + ".paused", false);
    }

    private TimeWindow targetWindow(String type, String id) {
        if (type.equals("period")) {
            return resolveWindow(working, type, id, TimeWindow.read(ConfigManager.configManager.getPeriodConfigurations().get(id)));
        }
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().values().stream()
                .filter(candidate -> candidate.type().equals(type) && candidate.id().equals(id)).findFirst().orElseThrow();
        return workingWindows.getOrDefault(definition.catalogKey(), configuredWindow(definition, working));
    }

    public void beginReload() throws Exception {
        working = QuestStorage.questStorage.loadSeasons();
        if (!working.getKeys(false).isEmpty() && working.getInt("schema-version") != 1) {
            throw new IllegalStateException("Unsupported season data version");
        }
        originalData = working.saveToString();
        working.set("schema-version", 1);
        if (working.getLong("revision") == 0L) {
            File legacyFile = new File(UltimateQuest.instance.getDataFolder(), "season-cycles.yml");
            if (legacyFile.isFile()) {
                YamlConfiguration legacy = new YamlConfiguration();
                legacy.load(legacyFile);
                for (String key : legacy.getKeys(false)) {
                    working.set("cycles." + key, legacy.getLong(key));
                }
            }
        }
        workingWindows = new LinkedHashMap<>();
        candidateNext = Instant.MAX;
    }

    private void prepareDefinitions() {
        Instant now = Instant.now();
        for (QuestDefinition definition : ConfigManager.configManager.getDefinitions().values()) {
            if (definition.growth()) {
                prepare(definition, now);
            }
        }
    }

    public void prepare(QuestDefinition definition, Instant now) {
        ObjectSeason season = definition.seasonConfig();
        String key = definition.catalogKey();
        if (!rotating(definition, working)) {
            workingWindows.put(key, configuredWindow(definition, working));
            return;
        }
        Instant start = season.getPrevious(now);
        long saved = working.getLong("cycles." + key, Long.MIN_VALUE);
        if (saved > start.getEpochSecond()) {
            start = Instant.ofEpochSecond(saved);
        }
        Instant end = season.getNext(start);
        TimeWindow window = new TimeWindow(start, end, definition.settings().getLong("claim-grace-seconds", 0L),
                working.getBoolean(statePath(definition) + ".paused", definition.settings().getBoolean("paused", false)));
        workingWindows.put(key, window);
        working.set("cycles." + key, start.getEpochSecond());
        if (end.isBefore(candidateNext)) {
            candidateNext = end;
        }
    }

    public void commitReload() {
        if (!working.saveToString().equals(originalData)) {
            long revision = working.getLong("revision");
            working.set("revision", Math.addExact(revision, 1L));
            QuestStorage.questStorage.saveSeasons(working, revision);
        }
        data = working;
        Instant poll = Instant.now().plusSeconds(5L);
        nextCheck = candidateNext.isBefore(poll) ? candidateNext : poll;
        cancelReload();
    }

    public void cancelReload() {
        working = null;
        originalData = null;
        workingWindows = null;
        candidateNext = Instant.MAX;
    }

    private boolean rotating(QuestDefinition definition, YamlConfiguration state) {
        return definition.growth() && definition.seasonConfig().isRotating()
                && !state.getBoolean(statePath(definition) + ".rotation-disabled", false);
    }

    private String currentSeason(QuestDefinition definition, YamlConfiguration state, TimeWindow window) {
        return rotating(definition, state) ? definition.seasonConfig().getId(window.startsAt())
                : state.getString(statePath(definition) + ".season", definition.season());
    }

    public String season(QuestDefinition definition) {
        YamlConfiguration current = data;
        return currentSeason(definition, current, window(definition, current));
    }

    public String storageKey(QuestDefinition definition) {
        String season = season(definition);
        return definition.growth() && !season.isEmpty() ? definition.catalogKey() + "/" + season : definition.catalogKey();
    }

    public TimeWindow window(QuestDefinition definition) {
        return window(definition, data);
    }

    private TimeWindow window(QuestDefinition definition, YamlConfiguration state) {
        if (rotating(definition, state)) {
            long saved = state.getLong("cycles." + definition.catalogKey(), Long.MIN_VALUE);
            Instant start = saved == Long.MIN_VALUE ? definition.seasonConfig().getPrevious(Instant.now()) : Instant.ofEpochSecond(saved);
            return new TimeWindow(start, definition.seasonConfig().getNext(start),
                    definition.settings().getLong("claim-grace-seconds", 0L),
                    state.getBoolean(statePath(definition) + ".paused", definition.settings().getBoolean("paused", false)));
        }
        return configuredWindow(definition, state);
    }

    public TimeWindow window(QuestDefinition definition, String season) {
        TimeWindow window = window(definition, season, null);
        return window == null ? new TimeWindow(null, Instant.EPOCH, 0L, false) : window;
    }

    public TimeWindow window(QuestDefinition definition, String season, TimeWindow fallback) {
        if (season.equals(season(definition))) {
            return window(definition);
        }
        ConfigurationSection history = data.getConfigurationSection(statePath(definition) + ".history." + season);
        if (history != null) {
            return TimeWindow.read(history);
        }
        if (fallback != null) {
            return fallback;
        }
        if (definition.seasonConfig().isRotating()) {
            String prefix = definition.settings().getString("season-prefix", "season") + "_";
            if (season.startsWith(prefix)) {
                try {
                    Instant start = Instant.ofEpochSecond(Long.parseLong(season.substring(prefix.length())));
                    return new TimeWindow(start, definition.seasonConfig().getNext(start),
                            definition.settings().getLong("claim-grace-seconds", 0L), false);
                } catch (RuntimeException ignored) {
                }
            }
        }
        return fallback;
    }

    public boolean paused(QuestDefinition definition) {
        return data.getBoolean(statePath(definition) + ".paused", definition.settings().getBoolean("paused", false));
    }

    public double globalBooster(QuestDefinition definition, Instant now) {
        String path = statePath(definition) + ".global-booster";
        return now.toEpochMilli() < data.getLong(path + ".expires-at", 0L)
                ? data.getDouble(path + ".multiplier", 1D) : 1D;
    }

    public TimeWindow periodWindow(String id, ConfigurationSection settings) {
        return resolveWindow(data, "period", id, TimeWindow.read(settings));
    }

    private TimeWindow configuredWindow(QuestDefinition definition, YamlConfiguration state) {
        TimeWindow defaults = new TimeWindow(definition.startsAt(), definition.endsAt(),
                definition.settings().getLong("claim-grace-seconds", 0L), definition.settings().getBoolean("paused", false));
        return resolveWindow(state, definition.type(), definition.id(), defaults);
    }

    private TimeWindow resolveWindow(YamlConfiguration state, String type, String id, TimeWindow defaults) {
        String path = statePath(type, id);
        String start = state.getString(path + ".starts-at", defaults.startsAt() == null ? "" : defaults.startsAt().toString());
        String end = state.getString(path + ".ends-at", defaults.endsAt() == null ? "" : defaults.endsAt().toString());
        return new TimeWindow(start.isBlank() ? null : Instant.parse(start), end.isBlank() ? null : Instant.parse(end),
                defaults.claimGraceSeconds(), state.getBoolean(path + ".paused", defaults.paused()));
    }

    private void saveWindow(String path, TimeWindow window) {
        working.set(path + ".starts-at", window.startsAt() == null ? "" : window.startsAt().toString());
        working.set(path + ".ends-at", window.endsAt() == null ? "" : window.endsAt().toString());
        working.set(path + ".claim-grace-seconds", window.claimGraceSeconds());
    }

    private String statePath(QuestDefinition definition) {
        return statePath(definition.type(), definition.id());
    }

    private String statePath(String type, String id) {
        return "definitions." + type + "." + id;
    }

    public void refresh() {
        if (Instant.now().isBefore(nextCheck) || !refreshing.compareAndSet(false, true)) {
            return;
        }
        DatabaseExecutor.executePlayerTask("server:seasons", () -> {
            synchronized (ConfigManager.configManager) {
                try {
                    beginReload();
                    prepareDefinitions();
                    commitReload();
                } catch (Exception exception) {
                    cancelReload();
                    nextCheck = Instant.now().plusSeconds(30L);
                    throw new IllegalStateException("Could not rotate seasons: " + exception.getMessage(), exception);
                }
            }
        }).whenComplete((ignored, failure) -> {
            refreshing.set(false);
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: " + failure.getMessage());
            }
        });
    }
}
