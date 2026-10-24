package cn.superiormc.ultimatequest.managers;

import static cn.superiormc.ultimatequest.managers.CacheManager.cacheManager;

import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.math.BigDecimal;
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
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class QuestManager extends AbstractManager {

    public static QuestManager questManager;

    private final ProgressionManager progression = new ProgressionManager(this);

    public QuestManager() {
        questManager = this;
        TriggerManager.triggerManager.subscribe(this::handleTrigger);
    }

    public QuestInstance growthInstance(UUID playerId, String type, String id) {
        synchronized (cacheManager) {
            ensureGrowth(playerId);
            if (id.contains("@") && Set.of("battlepass", "milestone").contains(type)) {
                String[] parts = id.split("@", 2);
                return cacheManager.instance(playerId, type + "/" + parts[0] + "/" + parts[1]);
            }
            QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, id);
            return definition == null ? null : cacheManager.instance(playerId, SeasonManager.seasonManager.storageKey(definition));
        }
    }

    public void playerLoaded(Player player) {
        synchronized (cacheManager) {
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
        synchronized (cacheManager) {
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
            quests.put(definition.id(), new QuestInstance(definition));
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "quests.accepted", "quest", definition.name(player));
            }, () -> {
                quests.remove(definition.id());
                cacheManager.changed(id);
                notifyPlayer(player, "quests.save-failed");
            });
        }
    }

    public boolean canAbandon(QuestInstance quest) {
        synchronized (cacheManager) {
            if (quest == null || quest.definition().growth() || !quest.source().equals("normal")
                    || !Set.of("ACTIVE", "COMPLETED").contains(quest.state())) {
                return false;
            }
            QuestDefinition definition = ConfigManager.configManager.getDefinitions().getOrDefault(quest.definition().catalogKey(), quest.definition());
            return definition.settings().getBoolean("allow-abandon", true);
        }
    }

    public void abandon(Player player, String rawId) {
        synchronized (cacheManager) {
            UUID id = player.getUniqueId();
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = cacheManager.instance(id, rawId);
            if (!canAbandon(quest)) {
                LanguageManager.languageManager.sendStringText(player, "quests.cannot-abandon");
                return;
            }
            String questId = quest.key();
            cacheManager.quests(id).remove(questId);
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "quests.abandoned", "quest", quest.definition().name(player));
            }, () -> {
                cacheManager.quests(id).put(questId, quest);
                cacheManager.changed(id);
                notifyPlayer(player, "quests.save-failed");
            });
        }
    }

    public void claim(Player player, String rawId) {
        synchronized (cacheManager) {
            UUID id = player.getUniqueId();
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = cacheManager.instance(id, rawId);
            if (quest == null || quest.definition().growth() || !assignmentActive(quest, Instant.now()) || !quest.state().equals("COMPLETED")) {
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
        synchronized (cacheManager) {
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = growthInstance(player.getUniqueId(), type, rawId);
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
        claimState(quest, key, "CLAIMING");
        cacheManager.changed(id);
        cacheManager.pending(id, true);
        cacheManager.saveCritical(id).whenComplete((ignored, failure) -> {
            synchronized (cacheManager) {
                if (failure != null) {
                    reviewClaim(player, quest, key);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim reservation failed for " + id
                            + ": " + failure.getMessage());
                    notifyPlayer(player, "quests.review-needed");
                    return;
                }
                if (cacheManager.isStopping()) {
                    return;
                }
                try {
                    SchedulerUtil.runSync(player, () -> grant(player, quest, key),
                            () -> reviewClaim(player, quest, key));
                } catch (RuntimeException exception) {
                    reviewClaim(player, quest, key);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest reward could not be scheduled for " + id
                            + ": " + exception.getMessage());
                }
            }
        });
    }

    private void reviewClaim(Player player, QuestInstance quest, String key) {
        synchronized (cacheManager) {
            UUID id = player.getUniqueId();
            claimState(quest, key, "REVIEW");
            cacheManager.pending(id, false);
            cacheManager.bulkClaim(id, null);
            cacheManager.changed(id);
            cacheManager.save(id);
        }
    }

    private void grant(Player player, QuestInstance quest, String key) {
        synchronized (cacheManager) {
            UUID id = player.getUniqueId();
            ensureGrowth(id);
            if (!player.isOnline() || !cacheManager.isOnline(id) || !assignmentActive(quest, Instant.now()) || (key != null && (!claimableGrowth(quest)
                    || (key.startsWith("premium/") && !quest.premium(player))))) {
                reviewClaim(player, quest, key);
                return;
            }
            CompletableFuture<Void> delivery;
            try {
                delivery = RewardManager.rewardManager.grant(player,
                        key == null ? quest.rewards() : quest.stageReward(key), quest.definition().id());
            } catch (RuntimeException exception) {
                delivery = CompletableFuture.failedFuture(exception);
            }
            delivery.whenComplete((ignored, failure) -> {
                synchronized (cacheManager) {
                    claimState(quest, key, failure == null ? "CLAIMED" : "REVIEW");
                    cacheManager.changed(id);
                    if (failure != null) {
                        ErrorManager.errorManager.sendErrorMessage("§cError: Quest reward delivery failed for " + id
                                + ": " + failure.getMessage());
                    }
                    cacheManager.saveCritical(id).whenComplete((saved, saveFailure) -> {
                        synchronized (cacheManager) {
                            cacheManager.pending(id, false);
                            if (saveFailure != null) {
                                claimState(quest, key, "REVIEW");
                                cacheManager.changed(id);
                                ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim result failed for " + id
                                        + ": " + saveFailure.getMessage());
                                cacheManager.markDirty(id);
                            }
                            rebuildIndex(id);
                            if (saveFailure != null || failure != null) {
                                cacheManager.bulkClaim(id, null);
                            }
                            if (saveFailure == null) {
                                if (!cacheManager.isStopping() && failure == null && key == null && quest.source().equals("scroll")) {
                                    SchedulerUtil.runSync(player,
                                            () -> QuestScrollManager.questScrollManager.complete(player, quest));
                                }
                                cacheManager.discardOffline(id);
                            }
                        }
                        notifyPlayer(player, failure == null && saveFailure == null ? "quests.claimed" : "quests.review-needed");
                    });
                }
            });
        }
    }

    private void handleTrigger(QuestContext context) {
        synchronized (cacheManager) {
            UUID id = context.player().getUniqueId();
            if (!cacheManager.isReady(id)) {
                return;
            }
            ensureGrowth(id);
            refreshPeriods(context.player());
            List<String> candidates = List.copyOf(cacheManager.index(id).getOrDefault(context.trigger().key().toString(), List.of()));
            for (String questId : candidates) {
                QuestInstance quest = cacheManager.instance(id, questId);
                if (quest == null || !quest.state().equals("ACTIVE") || !assignmentActive(quest, Instant.now())) {
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
                    QuestInstance pass = growthInstance(id, "battlepass", quest.sourceId());
                    if (pass == null || !pass.season().equals(challengeSeason(quest.cycle()))) {
                        continue;
                    }
                }
                if (quest.definition().growth()) {
                    if (!currentGrowth(quest) || !quest.definition().meetsConditions(context.player())) {
                        continue;
                    }
                    int previous = quest.level();
                    for (QuestObjective objective : quest.definition().objectives()) {
                        if (quest.addTotal(progression.boostedAmount(context.player(), quest,
                                objective.contribution(context, quest.key() + "/" + objective.getId())))) {
                            cacheManager.changed(id);
                        }
                    }
                    if (quest.level() > previous) {
                        progression.levelFeedback(context.player(), quest);
                    }
                    claimAutomatically(context.player());
                    continue;
                }
                for (QuestObjective objective : quest.definition().objectives()) {
                    if (quest.progress(objective) >= objective.required()) {
                        continue;
                    }
                    BigDecimal amount = objective.contribution(context, quest.instanceId() + "/" + objective.getId());
                    if (quest.advance(objective, amount)) {
                        cacheManager.changed(id);
                        if (quest.source().equals("scroll")) {
                            QuestScrollManager.questScrollManager.saveProgress(context.player(), quest);
                        }
                    }
                }
                if (quest.state().equals("COMPLETED")) {
                    if (quest.source().equals("challenge")) {
                        long points = quest.definition().getPoints();
                        addExperience(context.player(), quest.sourceId(), points);
                        quest.state("CLAIMED");
                        cacheManager.changed(id);
                        LanguageManager.languageManager.sendStringText(context.player(), "battlepass.task-completed");
                        continue;
                    }
                    LanguageManager.languageManager.sendStringText(context.player(), "quests.completed");
                    if (quest.definition().automaticClaim() && !cacheManager.isPending(id)) {
                        claim(context.player(), questId);
                    }
                }
            }
        }
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
        synchronized (cacheManager) {
            ensureGrowth(player.getUniqueId());
            refreshChallenges(player);
        }
    }

    public boolean claimableGrowth(QuestInstance quest) {
        synchronized (cacheManager) {
            QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
            return quest.claimable(Instant.now())
                    && current != null && !SeasonManager.seasonManager.paused(current);
        }
    }

    public void manageProgression(CommandSender sender, UUID id, String type, String pass, String operation, List<String> values) {
        synchronized (cacheManager) {
            if (cacheManager.isPending(id) || cacheManager.isLoading(id)) {
                LanguageManager.languageManager.sendStringText(sender, "quests.busy");
                return;
            }
            QuestDefinition definition = ConfigManager.configManager.getGrowthDefinition(type, pass.split("@", 2)[0]);
            if (definition == null) {
                LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
                return;
            }
            if (cacheManager.isReady(id)) {
                QuestInstance quest = growthInstance(id, type, pass);
                if (quest == null) {
                    LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
                    return;
                }
                try {
                    progression.applyChange(quest, operation, values);
                    cacheManager.changed(id);
                    cacheManager.persist(id, () -> LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-saved"),
                            () -> LanguageManager.languageManager.sendStringText(sender, "quests.save-failed"));
                } catch (RuntimeException exception) {
                    LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
                }
                return;
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
    }

    private void finishAdmin(CommandSender sender, UUID id, Throwable failure) {
        synchronized (cacheManager) {
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Quest admin edit failed for " + id
                        + ": " + failure.getMessage());
            }
            if (cacheManager.isStopping()) {
                return;
            }
            SchedulerUtil.runSync(() -> LanguageManager.languageManager.sendStringText(sender,
                    failure == null ? "battlepass.admin-saved" : "battlepass.admin-failed"));
        }
    }

    public void claimAll(Player player, String type, String pass) {
        synchronized (cacheManager) {
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = growthInstance(player.getUniqueId(), type, pass);
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
        QuestInstance growth = growthInstance(player.getUniqueId(), "battlepass", pass);
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
            QuestInstance growth = growthInstance(player.getUniqueId(), "battlepass", pass);
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
        synchronized (cacheManager) {
            ensureGrowth(player.getUniqueId());
            QuestInstance quest = growthInstance(player.getUniqueId(), "battlepass", id);
            return quest != null && currentGrowth(quest);
        }
    }

    public boolean addExperience(Player player, String id, long amount) {
        synchronized (cacheManager) {
            if (amount <= 0L || !canAddExperience(player, id)) {
                return false;
            }
            QuestInstance quest = growthInstance(player.getUniqueId(), "battlepass", id);
            int previous = quest.level();
            if (!quest.addTotal(progression.boostedAmount(player, quest, amount))) {
                return false;
            }
            cacheManager.changed(player.getUniqueId());
            if (quest.level() > previous) {
                progression.levelFeedback(player, quest);
            }
            return true;
        }
    }

    public void addExperienceAndSave(Player player, String id, long amount) {
        synchronized (cacheManager) {
            if (!cacheManager.available(player)) {
                return;
            }
            ensureGrowth(player.getUniqueId());
            if (!addExperience(player, id, amount)) {
                LanguageManager.languageManager.sendStringText(player, "progression.unavailable");
                return;
            }
            cacheManager.persist(player.getUniqueId(), () -> notifyPlayer(player, "progression.xp-added"),
                    () -> notifyPlayer(player, "progression.xp-save-failed"));
        }
    }

    public boolean meetsRequirements(Player player, QuestDefinition definition) {
        synchronized (cacheManager) {
            return definition.playerAcceptable() && eligible(player, definition);
        }
    }

    private boolean eligible(Player player, QuestDefinition definition) {
        Map<String, QuestInstance> quests = cacheManager.questsOrEmpty(player.getUniqueId());
        return definition.active(Instant.now()) && challengeUnlocked(player, definition)
                && definition.meetsConditions(player) && definition.prerequisites().stream().allMatch(id ->
                quests.containsKey(id) && quests.get(id).state().equals("CLAIMED"));
    }

    private boolean assignmentActive(QuestInstance quest, Instant now) {
        QuestDefinition live = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        if (quest.expired(now) || !quest.claimable(now)
                || live != null && (SeasonManager.seasonManager.paused(live)
                || !quest.definition().growth() && !live.claimable(now))) {
            return false;
        }
        if (quest.source().equals("scroll")) {
            return scrollCarried(quest);
        }
        if (!quest.source().equals("period")) {
            return true;
        }
        YamlConfiguration period = ConfigManager.configManager.getPeriodConfigurations().get(quest.sourceId());
        return period != null && SeasonManager.seasonManager.periodWindow(quest.sourceId(), period).claimable(now)
                && (quest.cycle().equals(QuestPeriodManager.cycle(quest.sourceId(), period, now)) || quest.state().equals("COMPLETED"));
    }

    public List<QuestInstance> assignedInstances(UUID id, String source) {
        synchronized (cacheManager) {
            return cacheManager.questsOrEmpty(id).values().stream()
                    .filter(quest -> quest.source().equals(source) && assignmentActive(quest, Instant.now())).toList();
        }
    }

    public void detachScroll(UUID playerId, UUID token) {
        synchronized (cacheManager) {
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

    private boolean scrollCarried(QuestInstance quest) {
        for (ObjectCache cache : cacheManager.loadedCaches()) {
            if (!cache.quests().containsValue(quest)) {
                continue;
            }
            Player player = Bukkit.getPlayer(cache.playerId());
            return player != null && QuestScrollManager.questScrollManager.hasToken(
                    player, UUID.fromString(quest.sourceId()));
        }
        return false;
    }

    public void refreshPeriods(Player player) {
        refreshPeriods(player, Instant.now());
    }

    public void refreshPeriods(Player player, Instant now) {
        synchronized (cacheManager) {
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
            cacheManager.changed(id);
            cacheManager.persist(id, () -> {
                rebuildIndex(id);
                notifyPlayer(player, "period.ready");
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
        synchronized (cacheManager) {
            UUID id = player.getUniqueId();
            return !drawn.isEmpty() && drawn.size() <= 45 && cacheManager.isReady(id) && cacheManager.isOnline(id) && cacheManager.isPending(id)
                    && withinLimits(id, "scroll", drawn, Set.of())
                    && drawn.stream().allMatch(definition -> !definition.growth() && eligible(player, definition));
        }
    }

    private boolean withinLimits(UUID id, String source, List<QuestDefinition> incoming, Set<String> replacing) {
        Instant now = Instant.now();
        List<QuestInstance> active = cacheManager.questsOrEmpty(id).values().stream()
                .filter(quest -> !quest.definition().growth() && !quest.state().equals("CLAIMED")
                        && !replacing.contains(quest.key()) && assignmentActive(quest, now)).toList();
        return ConfigManager.configManager.getTaskLimits().allows(active, source, incoming);
    }

    public CompletableFuture<Boolean> installScroll(Player player, UUID token,
            List<QuestDefinition> drawn, QuestInstance restored, long expiresAt) {
        synchronized (cacheManager) {
            if (!canInstallScroll(player, drawn)) {
                cacheManager.endPlayerOperation(player.getUniqueId());
                return CompletableFuture.completedFuture(false);
            }
            UUID id = player.getUniqueId();
            QuestInstance quest = restored == null
                    ? new QuestInstance(drawn.get(0), "scroll", token.toString(), "", expiresAt) : restored;
            if (!quest.source().equals("scroll") || !quest.sourceId().equals(token.toString())) {
                cacheManager.endPlayerOperation(id);
                return CompletableFuture.completedFuture(false);
            }
            QuestInstance previous = cacheManager.scroll(id, token);
            if (previous != null) {
                cacheManager.quests(id).remove(previous.key());
            }
            cacheManager.quests(id).put(quest.key(), quest);
            cacheManager.changed(id);
            CompletableFuture<Boolean> result = new CompletableFuture<>();
            cacheManager.saveCritical(id).whenComplete((ignored, failure) -> {
                synchronized (cacheManager) {
                    if (failure != null) {
                        quest.state("REVIEW");
                        cacheManager.changed(id);
                        ErrorManager.errorManager.sendErrorMessage("§cError: Quest scroll activation failed for " + id
                                + ": " + failure.getMessage());
                    }
                    cacheManager.pending(id, false);
                    rebuildIndex(id);
                    result.complete(failure == null);
                    if (failure == null) {
                        cacheManager.discardOffline(id);
                    }
                }
            });
            return result;
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
        if (cacheManager.isStopping()) {
            return;
        }
        String[] messageArgs = new String[args.length + 1];
        messageArgs[0] = key;
        System.arraycopy(args, 0, messageArgs, 1, args.length);
        SchedulerUtil.runSync(player, () -> {
            if (player.isOnline()) {
                LanguageManager.languageManager.sendStringText(player, messageArgs);
                if (player.getOpenInventory().getTopInventory().getHolder() instanceof ConfiguredMenuGUI gui) {
                    gui.refresh();
                }
                claimAutomatically(player);
            }
        });
    }

    private void claimAutomatically(Player player) {
        synchronized (cacheManager) {
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
                if (quest.state().equals("COMPLETED") && assignmentActive(quest, Instant.now()) && quest.definition().automaticClaim()) {
                    claim(player, quest.key());
                    return;
                }
            }
            cacheManager.bulkClaim(id, null);
        }
    }

    @Override
    public void onPluginDisable() {
        progression.close();
    }
}
