package cn.superiormc.ultimatequest.managers;

import static cn.superiormc.ultimatequest.managers.CacheManager.cacheManager;

import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class QuestManager extends AbstractManager {

    public static QuestManager questManager;

    private final ProgressionManager progression = new ProgressionManager(this);

    private final Map<UUID, Long> progressFeedbackTimes = new ConcurrentHashMap<>();

    public QuestManager() {
        questManager = this;
        TriggerManager.triggerManager.subscribe(this::handleTrigger);
    }

    public QuestInstance growthInstance(UUID playerId, String type, String id) {
        return growthInstance(cacheManager.snapshot(playerId), type, id);
    }

    public QuestInstance growthInstance(ObjectCache snapshot, String type, String id) {
        if (snapshot == null) {
            return null;
        }
        if (id.contains("@") && Set.of("battlepass", "milestone").contains(type)) {
            String[] parts = id.split("@", 2);
            return snapshot.quests().get(type + "/" + parts[0] + "/" + parts[1]);
        }
        QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, id);
        return definition == null ? null : snapshot.quests().get(SeasonManager.seasonManager.storageKey(definition));
    }

    private QuestInstance liveGrowthInstance(UUID playerId, String type, String id) {
        ObjectCache cache = cacheManager.getObjectCache(playerId);
        if (cache == null) {
            return null;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return null;
            }
            ensureGrowth(playerId);
            if (id.contains("@") && Set.of("battlepass", "milestone").contains(type)) {
                String[] parts = id.split("@", 2);
                return cacheManager.liveInstance(playerId, type + "/" + parts[0] + "/" + parts[1]);
            }
            QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, id);
            return definition == null ? null : cacheManager.liveInstance(playerId, SeasonManager.seasonManager.storageKey(definition));
        }
    }

    public void playerLoaded(Player player) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return;
            }
            UUID id = player.getUniqueId();
            ensureGrowth(id);
            rebuildIndex(id);
            if (cacheManager.questsOrEmpty(id).values().stream().anyMatch(quest -> Set.of("CLAIMING", "REVIEW").contains(quest.state())
                    || quest.stageStates().values().stream().anyMatch(value -> Set.of("CLAIMING", "REVIEW").contains(value)))) {
                LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
            }
            refreshPeriods(player);
            QuestScrollManager.questScrollManager.synchronize(player);
            claimAutomatically(player);
            if (TriggerManager.triggerManager != null) {
                TriggerManager.triggerManager.runtime().playerReady(player);
            }
        }
    }

    public void accept(Player player, String rawId) {
        accept(player, rawId, false, false);
    }

    public void accept(Player player, String rawId, boolean administrative, boolean bypass) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            UUID id = player.getUniqueId();
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestDefinition definition = ConfigManager.configManager.getDefinition(rawId);
            if (definition == null || !definition.listed() && !administrative || definition.isBattlePassTask()) {
                LanguageManager.languageManager.sendStringText(player, "quests.not-found");
                return;
            }
            Map<String, QuestInstance> quests = cacheManager.quests(id);
            if (!administrative && !definition.playerAcceptable()) {
                LanguageManager.languageManager.sendStringText(player, "quests.admin-only");
                return;
            }
            if (quests.containsKey(definition.id())) {
                LanguageManager.languageManager.sendStringText(player, "quests.already-accepted");
                return;
            }
            if (!withinLimits(id, "normal", List.of(definition), Set.of())) {
                LanguageManager.languageManager.sendStringText(player, "quests.limit");
                return;
            }
            boolean prerequisites = definition.prerequisites().stream().allMatch(previous -> quests.containsKey(previous) && quests.get(previous).state().equals("CLAIMED"));
            if (!bypass && (!prerequisites || !definition.meetsConditions(player) || !definition.active(Instant.now()))) {
                LanguageManager.languageManager.sendStringText(player, "quests.requirements");
                return;
            }
            QuestInstance quest = new QuestInstance(definition);
            quests.put(definition.id(), quest);
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "quests.accepted", quest);
            }, () -> {
                quests.remove(definition.id());
                cacheManager.changed(id);
                notifyPlayer(player, "quests.save-failed", quest);
            });
        }
    }

    public boolean canAbandon(QuestInstance quest) {
        if (quest == null || quest.definition().growth() || !quest.source().equals("normal")
                || !Set.of("ACTIVE", "COMPLETED").contains(quest.state())) {
            return false;
        }
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().getOrDefault(quest.definition().catalogKey(), quest.definition());
        return definition.settings().getBoolean("allow-abandon", true);
    }

    public void abandon(Player player, String rawId) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            UUID id = player.getUniqueId();
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = cacheManager.liveInstance(id, rawId);
            if (!canAbandon(quest)) {
                LanguageManager.languageManager.sendStringText(player, "quests.cannot-abandon");
                return;
            }
            String questId = quest.key();
            cacheManager.quests(id).remove(questId);
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "quests.abandoned", quest);
            }, () -> {
                cacheManager.quests(id).put(questId, quest);
                cacheManager.changed(id);
                notifyPlayer(player, "quests.save-failed", quest);
            });
        }
    }

    public void claim(Player player, String rawId) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            UUID id = player.getUniqueId();
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = cacheManager.liveInstance(id, rawId);
            if (quest == null || quest.definition().growth() || !assignmentActive(id, quest, Instant.now()) || !quest.state().equals("COMPLETED")) {
                LanguageManager.languageManager.sendStringText(player, "quests.cannot-claim");
                return;
            }
            if (quest.source().equals("scroll")) {
                QuestScrollManager.questScrollManager.reserveReward(player, quest,
                        () -> reserveClaim(player, quest, null));
                return;
            }
            reserveClaim(player, quest, null);
        }
    }

    public void claimStage(Player player, String type, String rawId, int level, String track) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = liveGrowthInstance(player.getUniqueId(), type, rawId);
            String key = track + "/" + level;
            if (quest == null || !claimableGrowth(quest) || !quest.definition().tracks().contains(track)
                    || !quest.stageState(key).equals("AVAILABLE")) {
                LanguageManager.languageManager.sendStringText(player, "quests.cannot-claim");
                return;
            }
            if (track.equals("premium") && !quest.premium(player)) {
                LanguageManager.languageManager.sendStringText(player, "progression.premium-required");
                return;
            }
            reserveClaim(player, quest, key);
        }
    }

    private void claimState(QuestInstance quest, String key, String state) {
        if (key == null) {
            quest.state(state);
        } else {
            quest.stageState(key, state);
        }
    }

    private void reserveClaim(Player player, QuestInstance quest, String key) {
        UUID id = player.getUniqueId();
        ObjectCache cache = cacheManager.getObjectCache(id);
        if (cache == null || !Thread.holdsLock(cache)) {
            return;
        }
        claimState(quest, key, "CLAIMING");
        cache.changed();
        cache.pending(true);
        UUID operation = cache.operationToken();
        cacheManager.saveCritical(id).whenComplete((ignored, failure) -> {
            if (failure != null) {
                reviewClaim(cache, operation, player, quest, key);
                ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim reservation failed for " + id
                        + ": " + failure.getMessage());
                notifyPlayer(player, "quests.review-needed", quest);
                return;
            }
            if (cacheManager.isStopping()) {
                return;
            }
            try {
                SchedulerUtil.runSync(player, () -> grant(cache, operation, player, quest, key),
                        () -> reviewClaim(cache, operation, player, quest, key));
            } catch (RuntimeException exception) {
                reviewClaim(cache, operation, player, quest, key);
                ErrorManager.errorManager.sendErrorMessage("§cError: Quest reward could not be scheduled for " + id
                        + ": " + exception.getMessage());
            }
        });
    }

    private void reviewClaim(ObjectCache cache, UUID operation, Player player, QuestInstance quest, String key) {
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache) || !cache.operation(operation)
                    || cache.quests().get(quest.key()) != quest) {
                return;
            }
            claimState(quest, key, "REVIEW");
            cache.finishOperation(operation);
            cache.bulkClaim(null);
            cache.changed();
            cacheManager.save(cache.playerId());
            if (quest.source().equals("scroll")) {
                cacheManager.endScroll(UUID.fromString(quest.sourceId()), operation);
            }
        }
    }

    private void grant(ObjectCache cache, UUID operation, Player player, QuestInstance quest, String key) {
        UUID id = cache.playerId();
        ConfigurationSection rewards;
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache) || !cache.operation(operation)
                    || cache.quests().get(quest.key()) != quest) {
                return;
            }
            ensureGrowth(id);
            if (!cacheManager.isSession(cache, player) || !assignmentActive(id, quest, Instant.now())
                    || key != null && (!claimableGrowth(quest) || key.startsWith("premium/") && !quest.premium(player))) {
                reviewClaim(cache, operation, player, quest, key);
                return;
            }
            rewards = key == null ? quest.rewards() : quest.stageReward(key);
        }
        CompletableFuture<Void> delivery;
        try {
            delivery = RewardManager.rewardManager.grant(player, rewards, quest.definition().id());
        } catch (RuntimeException exception) {
            delivery = CompletableFuture.failedFuture(exception);
        }
        delivery.whenComplete((ignored, failure) -> {
            synchronized (cache) {
                if (!cacheManager.isCurrent(cache) || !cache.operation(operation)
                        || cache.quests().get(quest.key()) != quest) {
                    return;
                }
                claimState(quest, key, failure == null ? "CLAIMED" : "REVIEW");
                cache.changed();
            }
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Quest reward delivery failed for " + id
                        + ": " + failure.getMessage());
            }
            cacheManager.saveCritical(id).whenComplete((saved, saveFailure) -> {
                QuestInstance notification;
                synchronized (cache) {
                    if (!cacheManager.isCurrent(cache) || !cache.operation(operation)) {
                        return;
                    }
                    if (saveFailure != null) {
                        claimState(quest, key, "REVIEW");
                        cache.changed();
                        cache.dirty(true);
                        ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim result failed for " + id
                                + ": " + saveFailure.getMessage());
                    }
                    rebuildIndex(id);
                    if (saveFailure != null || failure != null) {
                        cache.bulkClaim(null);
                    }
                    notification = new QuestInstance(quest);
                    cache.finishOperation(operation);
                }
                if (key == null && quest.source().equals("scroll")) {
                    if (saveFailure == null && failure == null) {
                        QuestScrollManager.questScrollManager.complete(player, notification, operation);
                    } else {
                        cacheManager.endScroll(UUID.fromString(quest.sourceId()), operation);
                    }
                }
                cacheManager.discardOffline(id);
                notifyPlayer(player, failure == null && saveFailure == null ? "quests.claimed" : "quests.review-needed", notification);
            });
        });
    }

    private void handleTrigger(QuestContext context) {
        ObjectCache cache = cacheManager.getObjectCache(context.player().getUniqueId());
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, context.player())) {
                return;
            }
            UUID id = context.player().getUniqueId();
            if (!cacheManager.isReady(id)) {
                return;
            }
            ensureGrowth(id);
            refreshPeriods(context.player());
            List<String> candidates = List.copyOf(cacheManager.index(id).getOrDefault(context.trigger().key().toString(), List.of()));
            for (String questId : candidates) {
                QuestInstance quest = cacheManager.liveInstance(id, questId);
                if (quest == null || !quest.state().equals("ACTIVE") || !assignmentActive(id, quest, Instant.now())) {
                    continue;
                }
                if (!challengeUnlocked(context.player(), quest.definition())) {
                    continue;
                }
                if (!quest.active(Instant.now())) {
                    continue;
                }
                if (quest.source().equals("period")) {
                    ConfigurationSection period = ConfigManager.configManager.getPeriodConfigurations().get(quest.sourceId());
                    if (period == null || !SeasonManager.seasonManager.periodWindow(quest.sourceId(), period).active(Instant.now())) {
                        continue;
                    }
                }
                if (quest.source().equals("scroll") && !QuestScrollManager.questScrollManager.hasToken(
                        context.player(), UUID.fromString(quest.sourceId()))) {
                    continue;
                }
                QuestDefinition liveDefinition = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
                if (liveDefinition != null && (SeasonManager.seasonManager.paused(liveDefinition)
                        || !liveDefinition.growth() && !liveDefinition.active(Instant.now()))) {
                    continue;
                }
                if (quest.source().equals("challenge")) {
                    QuestInstance pass = liveGrowthInstance(id, "battlepass", quest.sourceId());
                    if (pass == null || !pass.season().equals(challengeSeason(quest.cycle()))) {
                        continue;
                    }
                }
                if (quest.definition().growth()) {
                    if (!currentGrowth(quest) || !quest.definition().meetsConditions(context.player())) {
                        continue;
                    }
                    int previous = quest.level();
                    BigDecimal previousProgress = quest.totalAmount();
                    boolean feedbackChanged = false;
                    for (QuestObjective objective : quest.definition().objectives()) {
                        if (quest.addTotal(progression.boostedAmount(context.player(), quest,
                                objective.contribution(context, quest.key() + "/" + objective.getId())))) {
                            cacheManager.changed(id);
                            feedbackChanged |= objective.getBoolean("progress-feedback", true)
                                    && objective.getBoolean("progress-feedback.enabled", true);
                        }
                    }
                    if (quest.level() > previous) {
                        progression.levelFeedback(context.player(), quest);
                    }
                    if (feedbackChanged) {
                        sendProgressFeedback(context.player(), quest, previousProgress);
                    }
                    claimAutomatically(context.player());
                    continue;
                }
                BigDecimal previousProgress = taskProgress(quest);
                boolean feedbackChanged = false;
                for (QuestObjective objective : quest.definition().objectives()) {
                    if (quest.progress(objective) >= objective.required()) {
                        continue;
                    }
                    BigDecimal amount = objective.contribution(context, quest.instanceId() + "/" + objective.getId());
                    if (quest.advance(objective, amount)) {
                        cacheManager.changed(id);
                        feedbackChanged |= objective.getBoolean("progress-feedback", true)
                                && objective.getBoolean("progress-feedback.enabled", true);
                        if (quest.source().equals("scroll")) {
                            QuestScrollManager.questScrollManager.saveProgress(context.player(), quest);
                        }
                    }
                }
                if (feedbackChanged) {
                    sendProgressFeedback(context.player(), quest, previousProgress);
                }
                if (quest.state().equals("COMPLETED")) {
                    if (quest.source().equals("challenge")) {
                        long points = quest.definition().getPoints();
                        addExperience(context.player(), quest.sourceId(), points);
                        quest.state("CLAIMED");
                        cacheManager.changed(id);
                        sendQuestMessage(context.player(), quest, "battlepass.task-completed");
                        continue;
                    }
                    sendQuestMessage(context.player(), quest, "quests.completed");
                    if (quest.definition().automaticClaim() && !cacheManager.isPending(id)) {
                        claim(context.player(), questId);
                    }
                }
            }
        }
    }

    private BigDecimal taskProgress(QuestInstance quest) {
        if (quest.definition().growth()) {
            return quest.totalAmount();
        }
        BigDecimal progress = BigDecimal.ZERO;
        for (QuestObjective objective : quest.definition().objectives()) {
            progress = progress.add(quest.progressAmount(objective));
        }
        return progress;
    }

    private ConfigurationSection feedbackSettings(QuestInstance quest) {
        ConfigurationSection settings = quest.definition().settings();
        if (quest.source().equals("period")) {
            ConfigurationSection period = ConfigManager.configManager.getPeriodConfigurations().get(quest.sourceId());
            if (period != null && period.isConfigurationSection("task")) {
                return period.getConfigurationSection("task");
            }
        } else if (quest.source().equals("scroll")) {
            ConfigurationSection scroll = ConfigManager.configManager.getScrollConfigurations().get(quest.definition().id());
            if (scroll != null) {
                return scroll;
            }
        } else {
            QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
            if (current != null) {
                settings = current.settings();
            }
        }
        return settings;
    }

    public String[] questPlaceholders(Player player, QuestInstance quest) {
        QuestDefinition definition = quest.definition();
        String name = definition.name(player);
        String passId = definition.type().equals("battlepass") ? definition.id() : definition.getBattlePass();
        QuestDefinition pass = passId.isEmpty() ? null : ConfigManager.configManager.getGrowthDefinition("battlepass", passId);
        String passName = definition.type().equals("battlepass") ? name : pass == null ? "" : pass.name(player);
        ConfigurationSection period = quest.source().equals("period")
                ? ConfigManager.configManager.getPeriodConfigurations().get(quest.sourceId()) : null;
        return new String[] {
                "quest", name, "task", name, "name", name, "id", definition.id(),
                "player", player.getName(), "type", definition.type(), "source", quest.source(),
                "battlepass", passName, "battlepass-id", passId,
                "milestone", definition.type().equals("milestone") ? name : "",
                "scroll", quest.source().equals("scroll") ? name : "",
                "period", period == null ? "" : CommonUtil.parseLang(player, period.getString("name", quest.sourceId())),
                "level", String.valueOf(quest.level()),
                "total", taskProgress(quest).stripTrailingZeros().toPlainString(), "season", quest.season()
        };
    }

    public void sendQuestMessage(Player player, QuestInstance quest, String key, String... extra) {
        ObjectCache owner = cacheManager.getObjectCache(player.getUniqueId());
        if (owner != null && Thread.holdsLock(owner)) {
            QuestInstance snapshot = new QuestInstance(quest);
            SchedulerUtil.runSync(player, () -> {
                if (cacheManager.isSession(owner, player)) {
                    sendQuestMessage(player, snapshot, key, extra);
                }
            });
            return;
        }

        String[] names = questPlaceholders(player, quest);
        String[] placeholders = new String[extra.length + names.length];
        System.arraycopy(extra, 0, placeholders, 0, extra.length);
        System.arraycopy(names, 0, placeholders, extra.length, names.length);
        sendConfiguredMessage(player, feedbackSettings(quest), key, placeholders);
    }

    public void sendConfiguredMessage(Player player, ConfigurationSection settings, String key, String... placeholders) {
        String messageKey = key.substring(key.lastIndexOf('.') + 1);
        String message = settings.getString("messages." + messageKey);
        if (message == null) {
            message = LanguageManager.languageManager.getStringText(player, key);
        }
        TextUtil.sendMessage(player, CommonUtil.modifyString(player, message, placeholders));
    }

    private String progressFeedbackString(ConfigurationSection settings, String path, String fallback) {
        return settings.getString("progress-feedback." + path,
                ConfigManager.configManager.getString("task-progress." + path, fallback));
    }

    private String progressFeedbackColor(QuestInstance quest, ConfigurationSection settings) {
        String color = ConfigManager.configManager.getString("task-progress.bossbar.color", "BLUE");
        Map<String, YamlConfiguration> categories = ConfigManager.configManager.getCategoryConfigurations();
        for (String id : ConfigManager.configManager.getDisplayCategories(quest.definition(), quest)) {
            ConfigurationSection category = categories.get(id);
            if (category != null && category.isString("progress-feedback.bossbar.color")) {
                color = category.getString("progress-feedback.bossbar.color");
                break;
            }
        }
        return settings.getString("progress-feedback.bossbar.color", color);
    }

    private void sendProgressFeedback(Player player, QuestInstance quest, BigDecimal previousProgress) {
        ObjectCache owner = cacheManager.getObjectCache(player.getUniqueId());
        if (owner != null && Thread.holdsLock(owner)) {
            QuestInstance snapshot = new QuestInstance(quest);
            SchedulerUtil.runSync(player, () -> {
                if (cacheManager.isSession(owner, player)) {
                    sendProgressFeedback(player, snapshot, previousProgress);
                }
            });
            return;
        }

        ConfigurationSection settings = feedbackSettings(quest);
        if (!ConfigManager.configManager.getBoolean("task-progress.enabled", true)
                || !settings.getBoolean("progress-feedback", true)
                || !settings.getBoolean("progress-feedback.enabled", true)) {
            return;
        }
        BigDecimal progress = taskProgress(quest);
        BigDecimal gained = progress.subtract(previousProgress);
        if (gained.signum() <= 0) {
            return;
        }
        long now = System.currentTimeMillis();
        long cooldown = Math.max(0L, settings.getInt("progress-feedback.cooldown-ticks",
                ConfigManager.configManager.getInt("task-progress.cooldown-ticks", 5))) * 50L;
        Long previous = progressFeedbackTimes.get(player.getUniqueId());
        if (previous != null && now - previous < cooldown) {
            return;
        }
        progressFeedbackTimes.put(player.getUniqueId(), now);
        BigDecimal required = BigDecimal.ZERO;
        for (QuestObjective objective : quest.definition().objectives()) {
            required = required.add(BigDecimal.valueOf(objective.required()));
        }
        if (quest.definition().growth()) {
            long previousThreshold = quest.definition().thresholds().getOrDefault(quest.level(), 0L);
            Long nextThreshold = quest.definition().thresholds().get(quest.level() + 1);
            if (nextThreshold == null) {
                required = BigDecimal.valueOf(quest.definition().maximumTotal());
            } else {
                progress = progress.subtract(BigDecimal.valueOf(previousThreshold));
                required = BigDecimal.valueOf(nextThreshold).subtract(BigDecimal.valueOf(previousThreshold));
            }
        }
        BigDecimal ratio = required.signum() <= 0 ? BigDecimal.ZERO
                : progress.divide(required, 8, RoundingMode.DOWN).max(BigDecimal.ZERO).min(BigDecimal.ONE);
        String current = progress.stripTrailingZeros().toPlainString();
        List<String> values = new ArrayList<>(List.of(questPlaceholders(player, quest)));
        values.addAll(List.of(
                "progress", current, "current", current, "required", required.toPlainString(),
                "gained", gained.stripTrailingZeros().toPlainString(),
                "percent", ratio.movePointRight(2).setScale(0, RoundingMode.DOWN).toPlainString()
        ));
        String[] placeholders = values.toArray(String[]::new);
        String display = progressFeedbackString(settings, "display", "BOSS_BAR")
                .trim().toUpperCase(Locale.ROOT).replace('-', '_');
        if (Set.of("BOSS_BAR", "BOSSBAR", "BOTH").contains(display)) {
            String title = progressFeedbackString(settings, "bossbar.title", "{lang:quest-progress}");
            if (!title.isBlank()) {
                String message = "[bossbar=" + progressFeedbackColor(quest, settings)
                        + "," + progressFeedbackString(settings, "bossbar.style", "SOLID")
                        + "," + ratio.toPlainString() + "]" + CommonUtil.modifyString(player, title, placeholders) + "[/bossbar]";
                TextUtil.sendMessage(player, message, "quest-progress");
            }
        }
        if (Set.of("ACTION_BAR", "ACTIONBAR", "BOTH").contains(display)) {
            String message = progressFeedbackString(settings, "actionbar",
                    "[actionbar]{lang:quest-progress}[/actionbar]");
            if (!message.isBlank()) {
                TextUtil.sendMessage(player, CommonUtil.modifyString(player, message, placeholders));
            }
        }
        String sound = progressFeedbackString(settings, "sound.name", "");
        if (!sound.isBlank()) {
            player.playSound(player.getLocation(), sound,
                    (float) Math.max(0D, settings.getDouble("progress-feedback.sound.volume",
                            ConfigManager.configManager.getDouble("task-progress.sound.volume", 0.25D))),
                    (float) Math.max(0D, settings.getDouble("progress-feedback.sound.pitch",
                            ConfigManager.configManager.getDouble("task-progress.sound.pitch", 1.35D))));
        }
    }

    public void clearProgressFeedback(UUID playerId) {
        progressFeedbackTimes.remove(playerId);
    }

    private void ensureGrowth(UUID id) {
        Map<String, QuestInstance> quests = cacheManager.quests(id);
        if (quests == null) {
            return;
        }
        SeasonManager.seasonManager.refresh();
        boolean modified = false;
        for (QuestDefinition definition : ConfigManager.configManager.getDefinitions().values()) {
            if (!definition.growth()) {
                continue;
            }
            String key = SeasonManager.seasonManager.storageKey(definition);
            QuestInstance quest = quests.get(key);
            if (quest == null) {
                if (SeasonManager.seasonManager.paused(definition)
                        || !SeasonManager.seasonManager.window(definition).active(Instant.now())) {
                    continue;
                }
                quests.put(key, new QuestInstance(definition));
                modified = true;
            } else if (quest.refreshDefinition(definition)) {
                modified = true;
            }
        }
        if (modified) {
            cacheManager.changed(id);
            rebuildIndex(id);
        }
    }

    private boolean currentGrowth(QuestInstance quest) {
        QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        return current != null && SeasonManager.seasonManager.storageKey(current).equals(quest.key())
                && quest.active(Instant.now()) && !SeasonManager.seasonManager.paused(current);
    }

    public void refreshGrowth(Player player) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            refreshChallenges(player);
        }
    }

    public boolean claimableGrowth(QuestInstance quest) {
        QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        return quest.claimable(Instant.now())
                && current != null && !SeasonManager.seasonManager.paused(current);
    }

    public void manageProgression(CommandSender sender, UUID id, String type, String pass, String operation, List<String> values) {
        QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, pass.split("@", 2)[0]);
        if (definition == null) {
            LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
            return;
        }
        ObjectCache cache = cacheManager.getObjectCache(id);
        if (cache != null) {
            synchronized (cache) {
                if (!cacheManager.isCurrent(cache) || cache.pending() || cache.loadingToken() != null) {
                    LanguageManager.languageManager.sendStringText(sender, "quests.busy");
                    return;
                }
                if (cache.ready()) {
                    QuestInstance quest = liveGrowthInstance(id, type, pass);
                    if (quest == null) {
                        LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
                        return;
                    }
                    try {
                        progression.applyChange(quest, operation, values);
                        cache.changed();
                        cacheManager.persist(id, () -> finishAdmin(sender, id, null),
                                () -> finishAdmin(sender, id, new IllegalStateException("Quest save failed")));
                    } catch (RuntimeException exception) {
                        LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
                    }
                    return;
                }
            }
        }
        cacheManager.editOffline(id, data -> {
            try {
                String key = pass.contains("@") ? type + "/" + pass.replace('@', '/') : SeasonManager.seasonManager.storageKey(definition);
                ConfigurationSection section = data.getConfigurationSection("quests." + key);
                QuestInstance quest = section == null ? new QuestInstance(definition) : new QuestInstance(key, section);
                if (!quest.key().equals(key)) {
                    throw new IllegalArgumentException("Archived season not found");
                }
                progression.applyChange(quest, operation, values);
                data.set("quests." + key, null);
                quest.write(data.createSection("quests." + key));
            } catch (Exception exception) {
                throw new IllegalArgumentException("Could not edit progression data", exception);
            }
        }).whenComplete((ignored, failure) -> finishAdmin(sender, id, failure));
    }

    private void finishAdmin(CommandSender sender, UUID id, Throwable failure) {
        if (failure != null) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Quest admin edit failed for " + id
                    + ": " + failure.getMessage());
        }
        if (cacheManager.isStopping()) {
            return;
        }
        Runnable feedback = () -> LanguageManager.languageManager.sendStringText(sender,
                failure == null ? "battlepass.admin-saved" : "battlepass.admin-failed");
        if (sender instanceof Player player) {
            SchedulerUtil.runSync(player, feedback);
        } else {
            SchedulerUtil.runSync(feedback);
        }
    }

    public void claimAll(Player player, String type, String pass) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = liveGrowthInstance(player.getUniqueId(), type, pass);
            if (quest == null) {
                LanguageManager.languageManager.sendStringText(player, "quests.not-found");
                return;
            }
            if (!claimableGrowth(quest)) {
                LanguageManager.languageManager.sendStringText(player, "progression.claim-unavailable");
                return;
            }
            boolean premium = quest.premium(player);
            boolean claimable = quest.stageStates().entrySet().stream().anyMatch(entry -> entry.getValue().equals("AVAILABLE")
                    && (!entry.getKey().startsWith("premium/") || premium));
            if (!claimable) {
                if (quest.stageStates().values().stream().anyMatch(state -> Set.of("CLAIMING", "REVIEW").contains(state))) {
                    LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
                } else if (!premium && quest.stageStates().entrySet().stream().anyMatch(entry ->
                        entry.getKey().startsWith("premium/") && entry.getValue().equals("AVAILABLE"))) {
                    LanguageManager.languageManager.sendStringText(player, "progression.premium-required");
                } else {
                    LanguageManager.languageManager.sendStringText(player, "quests.cannot-claim");
                }
                return;
            }
            cacheManager.bulkClaim(player.getUniqueId(), quest.key());
            claimAutomatically(player);
        }
    }

    private boolean challengeUnlocked(Player player, QuestDefinition definition) {
        String pass = definition.getBattlePass();
        if (pass.isEmpty()) {
            return true;
        }
        QuestInstance growth = liveGrowthInstance(player.getUniqueId(), "battlepass", pass);
        if (growth == null || !currentGrowth(growth)
                || (definition.isPremiumTask() && !growth.premium(player))) {
            return false;
        }
        Instant starts = growth.startsAt();
        long elapsed = starts == null ? 0L : Math.max(0L, Instant.now().getEpochSecond() - starts.getEpochSecond());
        long week = elapsed / 604800L + 1L;
        int unlock = Math.max(1, definition.getUnlockWeek());
        return week >= unlock && (definition.allowsCatchUp() || week == unlock);
    }

    private void refreshChallenges(Player player) {
        Map<String, QuestInstance> quests = cacheManager.quests(player.getUniqueId());
        if (quests == null) {
            return;
        }
        for (QuestDefinition definition : ConfigManager.configManager.getDefinitions().values()) {
        String pass = definition.getBattlePass();
            if (pass.isEmpty() || definition.growth() || !eligible(player, definition)) {
                continue;
            }
            QuestInstance growth = liveGrowthInstance(player.getUniqueId(), "battlepass", pass);
            if (growth == null) {
                continue;
            }
            String cycle = challengeCycle(growth, definition, Instant.now());
            long expires = challengeExpires(growth, definition, Instant.now());
            List<String> stale = quests.entrySet().stream().filter(entry -> {
                QuestInstance quest = entry.getValue();
                return quest.source().equals("challenge") && quest.sourceId().equals(pass)
                        && quest.definition().id().equals(definition.id()) && !quest.cycle().equals(cycle)
                        && !Set.of("CLAIMING", "REVIEW").contains(quest.state());
            }).map(Map.Entry::getKey).toList();
            stale.forEach(quests::remove);
            QuestInstance challenge = new QuestInstance(definition, "challenge", pass,
                    cycle, expires);
            boolean added = !quests.containsKey(challenge.key())
                    && withinLimits(player.getUniqueId(), "challenge", List.of(definition), Set.of());
            if (added) {
                quests.put(challenge.key(), challenge);
            }
            if (!stale.isEmpty() || added) {
                cacheManager.changed(player.getUniqueId());
                rebuildIndex(player.getUniqueId());
            }
        }
    }

    private String challengeCycle(QuestInstance pass, QuestDefinition task, Instant now) {
        String reset = task.getReset();
        if (reset.equals("season")) {
            return pass.season();
        }
        ZonedDateTime date = now.atZone(ZoneId.systemDefault());
        long cycle = reset.equals("daily") ? date.toLocalDate().toEpochDay()
                : date.with(DayOfWeek.MONDAY).toLocalDate().toEpochDay();
        return pass.season() + "--" + reset + "_" + cycle;
    }

    private String challengeSeason(String cycle) {
        int separator = cycle.indexOf("--");
        return separator < 0 ? cycle : cycle.substring(0, separator);
    }

    private long challengeExpires(QuestInstance pass, QuestDefinition task, Instant now) {
        String reset = task.getReset();
        Instant resetEnd = null;
        if (!reset.equals("season")) {
            ZonedDateTime date = now.atZone(ZoneId.systemDefault());
            resetEnd = reset.equals("daily") ? date.truncatedTo(ChronoUnit.DAYS).plusDays(1).toInstant()
                    : date.with(DayOfWeek.MONDAY).truncatedTo(ChronoUnit.DAYS).plusWeeks(1).toInstant();
        }
        Instant passEnd = pass.endsAt();
        Instant end = resetEnd == null || passEnd != null && passEnd.isBefore(resetEnd) ? passEnd : resetEnd;
        return end == null ? 0L : end.toEpochMilli();
    }

    public boolean canAddExperience(Player player, String id) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return false;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return false;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = liveGrowthInstance(player.getUniqueId(), "battlepass", id);
            return quest != null && currentGrowth(quest);
        }
    }

    public boolean addExperience(Player player, String id, long amount) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return false;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return false;
            }
            if (amount <= 0L || !canAddExperience(player, id)) {
                return false;
            }
            QuestInstance quest = liveGrowthInstance(player.getUniqueId(), "battlepass", id);
            int previous = quest.level();
            BigDecimal previousProgress = quest.totalAmount();
            if (!quest.addTotal(progression.boostedAmount(player, quest, amount))) {
                return false;
            }
            cacheManager.changed(player.getUniqueId());
            if (quest.level() > previous) {
                progression.levelFeedback(player, quest);
            }
            sendProgressFeedback(player, quest, previousProgress);
            return true;
        }
    }

    public void addExperienceAndSave(Player player, String id, long amount) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            if (!addExperience(player, id, amount)) {
                LanguageManager.languageManager.sendStringText(player, "progression.unavailable");
                return;
            }
            QuestInstance quest = liveGrowthInstance(player.getUniqueId(), "battlepass", id);
            cacheManager.persist(player.getUniqueId(), () -> notifyPlayer(player, "progression.xp-added", quest),
                    () -> notifyPlayer(player, "progression.xp-save-failed", quest));
        }
    }

    public boolean meetsRequirements(Player player, QuestDefinition definition) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return false;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return false;
            }
            return definition.playerAcceptable() && eligible(player, definition);
        }
    }

    private boolean eligible(Player player, QuestDefinition definition) {
        Map<String, QuestInstance> quests = cacheManager.questsOrEmpty(player.getUniqueId());
        return definition.active(Instant.now()) && challengeUnlocked(player, definition)
                && definition.meetsConditions(player) && definition.prerequisites().stream().allMatch(id ->
                quests.containsKey(id) && quests.get(id).state().equals("CLAIMED"));
    }

    private boolean assignmentActive(UUID id, QuestInstance quest, Instant now) {
        ObjectCache cache = cacheManager.getObjectCache(id);
        return cache != null && Thread.holdsLock(cache) && assignmentActive(quest, now, cache.carriedScrolls());
    }

    private boolean assignmentActive(QuestInstance quest, Instant now, Set<UUID> carriedScrolls) {
        QuestDefinition live = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        if (quest.expired(now) || !quest.claimable(now)
                || live != null && (SeasonManager.seasonManager.paused(live)
                || !quest.definition().growth() && !live.claimable(now))) {
            return false;
        }
        if (quest.source().equals("scroll")) {
            return carriedScrolls.contains(UUID.fromString(quest.sourceId()));
        }
        if (!quest.source().equals("period")) {
            return true;
        }
        YamlConfiguration period = ConfigManager.configManager.getPeriodConfigurations().get(quest.sourceId());
        return period != null && SeasonManager.seasonManager.periodWindow(quest.sourceId(), period).claimable(now)
                && (quest.cycle().equals(QuestPeriodManager.cycle(quest.sourceId(), period, now)) || quest.state().equals("COMPLETED"));
    }

    public List<QuestInstance> assignedInstances(UUID id, String source) {
        ObjectCache cache = cacheManager.getObjectCache(id);
        if (cache == null) {
            return List.of();
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return List.of();
            }
            return cacheManager.questsOrEmpty(id).values().stream()
                    .filter(quest -> quest.source().equals(source) && assignmentActive(id, quest, Instant.now()))
                    .map(QuestInstance::new).toList();
        }
    }

    public List<QuestInstance> assignedInstances(ObjectCache snapshot, String source) {
        if (snapshot == null) {
            return List.of();
        }
        Instant now = Instant.now();
        return snapshot.quests().values().stream()
                .filter(quest -> quest.source().equals(source) && assignmentActive(quest, now, snapshot.carriedScrolls()))
                .toList();
    }

    public void detachScroll(UUID playerId, UUID token) {
        ObjectCache cache = cacheManager.getObjectCache(playerId);
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            Map<String, QuestInstance> quests = cacheManager.quests(playerId);
            if (quests == null || cacheManager.isPending(playerId)) {
                return;
            }
            List<String> removed = quests.entrySet().stream().filter(entry -> entry.getValue().source().equals("scroll")
                    && entry.getValue().sourceId().equals(token.toString())).map(Map.Entry::getKey).toList();
            if (removed.isEmpty()) {
                return;
            }
            removed.forEach(quests::remove);
            cacheManager.changed(playerId);
            rebuildIndex(playerId);
        }
    }

    public void refreshPeriods(Player player) {
        refreshPeriods(player, Instant.now());
    }

    public void refreshPeriods(Player player, Instant now) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            cacheManager.available(player);
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            UUID id = player.getUniqueId();
            if (!cacheManager.isReady(id) || cacheManager.isPending(id) || !cacheManager.isOnline(id)) {
                return;
            }
            Map<String, QuestInstance> quests = cacheManager.quests(id);
            Map<String, String> rolls = cacheManager.getObjectCache(id).periodRolls();
            Map<String, String> previousRolls = new HashMap<>(rolls);
            Map<String, QuestInstance> removed = new LinkedHashMap<>();
            List<String> added = new ArrayList<>();
            for (Map.Entry<String, YamlConfiguration> entry : ConfigManager.configManager.getPeriodConfigurations().entrySet()) {
                String group = entry.getKey();
                YamlConfiguration config = entry.getValue();
                if (!SeasonManager.seasonManager.periodWindow(group, config).active(now)) {
                    continue;
                }
                String cycle = QuestPeriodManager.cycle(group, config, now);
                if (cycle.equals(rolls.get(group))) {
                    continue;
                }
                String previousCycle = rolls.get(group);
                if (previousCycle != null && Long.parseLong(previousCycle.substring(previousCycle.indexOf('_') + 1))
                        >= QuestPeriodManager.start(group, config, now).toEpochSecond()) {
                    continue;
                }
                List<QuestDefinition> drawn;
                try {
                    long seed = id.getMostSignificantBits() ^ id.getLeastSignificantBits() ^ ((long) group.hashCode() << 32) ^ cycle.hashCode();
                    ConfigurationSection source = config.getConfigurationSection("source");
                    drawn = new ArrayList<>();
                    if (source.contains("pool")) {
                        List<String> selected = QuestPoolManager.questPoolManager.draw(source, new Random(seed));
                        for (String objective : selected) {
                            drawn.add(ConfigManager.configManager.getPeriod(group).createDefinition(objective, ConfigManager.configManager.getQuestConfigurations()));
                        }
                    } else {
                        drawn.add(ConfigManager.configManager.getPeriod(group).createDefinition(null, ConfigManager.configManager.getQuestConfigurations()));
                    }
                } catch (IllegalArgumentException exception) {
                    continue;
                }
                List<QuestInstance> replaceable = quests.values().stream().filter(quest ->
                        quest.source().equals("period") && quest.sourceId().equals(group)
                            && !Set.of("CLAIMING", "REVIEW").contains(quest.state())
                            && !(quest.state().equals("COMPLETED") && !quest.expired(now))).toList();
                Set<String> replacing = replaceable.stream().map(QuestInstance::key).collect(Collectors.toSet());
                if (!withinLimits(id, "period", drawn, replacing)) {
                    continue;
                }
                for (QuestInstance quest : replaceable) {
                    removed.put(quest.key(), quest);
                    quests.remove(quest.key());
                }
                long expires = QuestPeriodManager.end(group, config, now).plusSeconds(
                        Math.max(0L, config.getLong("claim-grace-seconds", 0L))).toEpochMilli();
                for (QuestDefinition definition : drawn) {
                    QuestInstance quest = new QuestInstance(definition, "period", group, cycle, expires);
                    quests.put(quest.key(), quest);
                    added.add(quest.key());
                }
                rolls.put(group, cycle);
            }
            if (added.isEmpty()) {
                return;
            }
            String taskNames = added.stream().map(quests::get).map(quest -> quest.definition().name(player))
                    .collect(Collectors.joining(", "));
            String periodNames = added.stream().map(quests::get).map(QuestInstance::sourceId).distinct()
                    .map(group -> CommonUtil.parseLang(player, ConfigManager.configManager.getPeriodConfigurations()
                            .get(group).getString("name", group)))
                    .collect(Collectors.joining(", "));
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "period.ready", "quest", taskNames, "task", taskNames,
                        "name", taskNames, "period", periodNames);
            }, () -> {
                added.forEach(quests::remove);
                quests.putAll(removed);
                rolls.clear();
                rolls.putAll(previousRolls);
                cacheManager.changed(id);
                rebuildIndex(id);
                notifyPlayer(player, "quests.save-failed");
            });
        }
    }

    public boolean canInstallScroll(Player player, List<QuestDefinition> drawn) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return false;
        }
        synchronized (cache) {
            UUID id = player.getUniqueId();
            return cacheManager.isSession(cache, player) && !drawn.isEmpty() && drawn.size() <= 45 && cacheManager.isReady(id) && cacheManager.isOnline(id) && cacheManager.isPending(id)
                    && withinLimits(id, "scroll", drawn, Set.of())
                    && drawn.stream().allMatch(definition -> !definition.growth() && eligible(player, definition));
        }
    }

    private boolean withinLimits(UUID id, String source, List<QuestDefinition> incoming, Set<String> replacing) {
        Instant now = Instant.now();
        List<QuestInstance> active = cacheManager.questsOrEmpty(id).values().stream()
                .filter(quest -> !quest.definition().growth() && !quest.state().equals("CLAIMED")
                        && !replacing.contains(quest.key()) && assignmentActive(id, quest, now)).toList();
        return ConfigManager.configManager.getTaskLimits().allows(active, source, incoming);
    }

    public CompletableFuture<Boolean> installScroll(Player player, UUID token,
            List<QuestDefinition> drawn, QuestInstance restored, long expiresAt) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return CompletableFuture.completedFuture(false);
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return CompletableFuture.completedFuture(false);
            }
            if (!canInstallScroll(player, drawn)) {
                return CompletableFuture.completedFuture(false);
            }
            UUID id = player.getUniqueId();
            QuestInstance quest = restored == null
                    ? new QuestInstance(drawn.get(0), "scroll", token.toString(), "", expiresAt) : restored;
            if (!quest.source().equals("scroll") || !quest.sourceId().equals(token.toString())) {
                return CompletableFuture.completedFuture(false);
            }
            QuestInstance previous = cacheManager.scroll(id, token);
            if (previous != null) {
                cacheManager.quests(id).remove(previous.key());
            }
            cacheManager.quests(id).put(quest.key(), quest);
            cache.carriedScrolls().add(token);
            cacheManager.changed(id);
            UUID operation = cache.operationToken();
            return cacheManager.saveCritical(id).handleAsync((ignored, failure) -> {
                synchronized (cache) {
                    if (!cacheManager.isCurrent(cache) || !cache.operation(operation)) {
                        return false;
                    }
                    if (failure != null) {
                        quest.state("REVIEW");
                        cache.changed();
                        ErrorManager.errorManager.sendErrorMessage("§cError: Quest scroll activation failed for " + id
                                + ": " + failure.getMessage());
                    }
                    rebuildIndex(id);
                }
                return failure == null;
            });
        }
    }

    private void rebuildIndex(UUID id) {
        Map<String, List<String>> index = new HashMap<>();
        for (QuestInstance quest : cacheManager.questsOrEmpty(id).values()) {
            if (!quest.state().equals("ACTIVE")) {
                continue;
            }
            for (String trigger : quest.definition().objectives().stream().map(QuestObjective::trigger).distinct().toList()) {
                index.computeIfAbsent(trigger, ignored -> new ArrayList<>()).add(quest.key());
            }
        }
        cacheManager.index(id, index);
    }

    private void notifyPlayer(Player player, String key, String... args) {
        notifyPlayer(player, key, null, args);
    }

    private void notifyPlayer(Player player, String key, QuestInstance quest, String... args) {
        if (cacheManager.isStopping()) {
            return;
        }
        String[] messageArgs = new String[args.length + 1];
        messageArgs[0] = key;
        System.arraycopy(args, 0, messageArgs, 1, args.length);
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        QuestInstance notification;
        synchronized (cache) {
            notification = quest == null ? null : new QuestInstance(quest);
        }
        SchedulerUtil.runSync(player, () -> {
            if (cacheManager.isSession(cache, player)) {
                if (notification == null) {
                    LanguageManager.languageManager.sendStringText(player, messageArgs);
                } else {
                    sendQuestMessage(player, notification, key, args);
                }
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof ConfiguredMenuGUI gui) {
                    gui.refresh();
                }
                claimAutomatically(player);
            }
        });
    }

    private void claimAutomatically(Player player) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            if (!cacheManager.isSession(cache, player)) {
                return;
            }
            UUID id = player.getUniqueId();
            if (!cacheManager.isOnline(id) || cacheManager.isPending(id)) {
                return;
            }
            for (QuestInstance quest : cacheManager.questsOrEmpty(id).values()) {
                String requested = cacheManager.bulkClaim(id);
                if (quest.definition().growth() && claimableGrowth(quest)
                        && ((requested != null && requested.equals(quest.key()))
                        || quest.definition().settings().getBoolean("auto-claim", false))) {
                    for (String stage : quest.stageStates().keySet().stream().sorted(Comparator
                            .comparingInt((String key) -> Integer.parseInt(key.split("/")[1]))
                            .thenComparing(key -> key)).toList()) {
                        if (quest.stageState(stage).equals("AVAILABLE")
                                && (!stage.startsWith("premium/") || quest.premium(player))) {
                            reserveClaim(player, quest, stage);
                            return;
                        }
                    }
                }
                if (quest.state().equals("COMPLETED") && assignmentActive(id, quest, Instant.now()) && quest.definition().automaticClaim()) {
                    claim(player, quest.key());
                    return;
                }
            }
            cacheManager.bulkClaim(id, null);
        }
    }

    @Override
    public void onPluginDisable() {
        progressFeedbackTimes.clear();
        progression.close();
    }
}
