package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestSeasonData;
import cn.superiormc.ultimatequest.objects.time.ObjectSeason;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class SeasonManager {

    public static SeasonManager seasonManager;

    private volatile Map<String, QuestSeasonData> data = Map.of();

    private Map<String, QuestSeasonData> working;

    private Map<String, QuestSeasonData> originalData;

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
        String key = statePath(type, id);
        QuestSeasonData state = working.getOrDefault(key, QuestSeasonData.empty());
        Instant now = Instant.now();
        switch (operation) {
            case "pause", "resume" -> {
                requireValues(values, 0);
                working.put(key, new QuestSeasonData(state.revision(), state.season(), state.cycleStart(),
                        operation.equals("pause"), state.rotationDisabled(), state.windowOverride(), state.window(),
                        state.booster(), state.boosterUntil(), state.history()));
            }
            case "end" -> {
                requireValues(values, 0);
                TimeWindow window = targetWindow(type, id);
                TimeWindow ended = new TimeWindow(window.startsAt() != null && window.startsAt().isBefore(now)
                        ? window.startsAt() : null, now, window.claimGraceSeconds(), false);
                working.put(key, new QuestSeasonData(state.revision(), state.season(), state.cycleStart(),
                        state.paused(), true, true, ended, state.booster(), state.boosterUntil(), state.history()));
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
                working.put(key, new QuestSeasonData(state.revision(), state.season(), state.cycleStart(),
                        state.paused(), state.rotationDisabled(), state.windowOverride(), state.window(), multiplier,
                        now.plusSeconds(seconds).toEpochMilli(), state.history()));
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
        QuestSeasonData state = working.getOrDefault(path, QuestSeasonData.empty());
        String previous = definition == null ? state.season() : currentSeason(definition, working, targetWindow(type, id));
        if (season.equals(previous) || state.history().containsKey(season)) {
            throw new IllegalArgumentException("Use a new season ID");
        }
        Map<String, TimeWindow> history = new LinkedHashMap<>(state.history());
        TimeWindow window = targetWindow(type, id);
        if (!previous.isEmpty()) {
            Instant now = Instant.now();
            Instant previousEnd = window.endsAt() == null || window.endsAt().isAfter(now) ? now : window.endsAt();
            Instant previousStart = window.startsAt() != null && window.startsAt().isBefore(previousEnd) ? window.startsAt() : null;
            history.put(previous, new TimeWindow(previousStart, previousEnd, window.claimGraceSeconds(), false));
        }
        working.put(path, new QuestSeasonData(state.revision(), season, state.cycleStart(), false, true, true,
                new TimeWindow(start, end, window.claimGraceSeconds(), false), state.booster(), state.boosterUntil(), history));
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
        originalData = QuestStorage.questStorage.loadSeasons();
        working = new LinkedHashMap<>(originalData);
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
        QuestSeasonData state = working.getOrDefault(key, QuestSeasonData.empty());
        long saved = state.cycleStart() == null ? Long.MIN_VALUE : state.cycleStart();
        if (saved > start.getEpochSecond()) {
            start = Instant.ofEpochSecond(saved);
        }
        Instant end = season.getNext(start);
        TimeWindow window = new TimeWindow(start, end, definition.settings().getLong("claim-grace-seconds", 0L),
                state.paused() == null ? definition.settings().getBoolean("paused", false) : state.paused());
        workingWindows.put(key, window);
        working.put(key, new QuestSeasonData(state.revision(), state.season(), start.getEpochSecond(), state.paused(),
                state.rotationDisabled(), state.windowOverride(), state.window(), state.booster(), state.boosterUntil(), state.history()));
        if (end.isBefore(candidateNext)) {
            candidateNext = end;
        }
    }

    public void commitReload() {
        data = QuestStorage.questStorage.saveSeasons(working, originalData);
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

    private boolean rotating(QuestDefinition definition, Map<String, QuestSeasonData> state) {
        return definition.growth() && definition.seasonConfig().isRotating()
                && !state.getOrDefault(statePath(definition), QuestSeasonData.empty()).rotationDisabled();
    }

    private String currentSeason(QuestDefinition definition, Map<String, QuestSeasonData> state, TimeWindow window) {
        return rotating(definition, state) ? definition.seasonConfig().getId(window.startsAt())
                : (state.getOrDefault(statePath(definition), QuestSeasonData.empty()).season().isEmpty() ? definition.season()
                        : state.get(statePath(definition)).season());
    }

    public String season(QuestDefinition definition) {
        Map<String, QuestSeasonData> current = data;
        return currentSeason(definition, current, window(definition, current));
    }

    public String storageKey(QuestDefinition definition) {
        String season = season(definition);
        return definition.growth() && !season.isEmpty() ? definition.catalogKey() + "/" + season : definition.catalogKey();
    }

    public TimeWindow window(QuestDefinition definition) {
        return window(definition, data);
    }

    private TimeWindow window(QuestDefinition definition, Map<String, QuestSeasonData> state) {
        if (rotating(definition, state)) {
            QuestSeasonData savedState = state.getOrDefault(statePath(definition), QuestSeasonData.empty());
            long saved = savedState.cycleStart() == null ? Long.MIN_VALUE : savedState.cycleStart();
            Instant start = saved == Long.MIN_VALUE ? definition.seasonConfig().getPrevious(Instant.now()) : Instant.ofEpochSecond(saved);
            return new TimeWindow(start, definition.seasonConfig().getNext(start),
                    definition.settings().getLong("claim-grace-seconds", 0L),
                    savedState.paused() == null ? definition.settings().getBoolean("paused", false) : savedState.paused());
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
        TimeWindow history = data.getOrDefault(statePath(definition), QuestSeasonData.empty()).history().get(season);
        if (history != null) {
            return history;
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
        Boolean paused = data.getOrDefault(statePath(definition), QuestSeasonData.empty()).paused();
        return paused == null ? definition.settings().getBoolean("paused", false) : paused;
    }

    public double globalBooster(QuestDefinition definition, Instant now) {
        QuestSeasonData state = data.getOrDefault(statePath(definition), QuestSeasonData.empty());
        return now.toEpochMilli() < state.boosterUntil() ? state.booster() : 1D;
    }

    public TimeWindow periodWindow(String id, ConfigurationSection settings) {
        return resolveWindow(data, "period", id, TimeWindow.read(settings));
    }

    private TimeWindow configuredWindow(QuestDefinition definition, Map<String, QuestSeasonData> state) {
        TimeWindow defaults = new TimeWindow(definition.startsAt(), definition.endsAt(),
                definition.settings().getLong("claim-grace-seconds", 0L), definition.settings().getBoolean("paused", false));
        return resolveWindow(state, definition.type(), definition.id(), defaults);
    }

    private TimeWindow resolveWindow(Map<String, QuestSeasonData> state, String type, String id, TimeWindow defaults) {
        QuestSeasonData saved = state.getOrDefault(statePath(type, id), QuestSeasonData.empty());
        TimeWindow window = saved.windowOverride() ? saved.window() : defaults;
        return new TimeWindow(window.startsAt(), window.endsAt(), defaults.claimGraceSeconds(),
                saved.paused() == null ? defaults.paused() : saved.paused());
    }

    private String statePath(QuestDefinition definition) {
        return statePath(definition.type(), definition.id());
    }

    private String statePath(String type, String id) {
        return type + "/" + id;
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
