package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import java.math.BigDecimal;
import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.gui.inv.ConfiguredMenuGUI;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.File;
import java.time.Instant;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class QuestManager extends AbstractManager {

    private static final Set<String> TASK_LIMIT_TYPES = Set.of("normal", "scroll", "period", "battlepass-task");

    public static QuestManager questManager;

    private final Map<UUID, Map<String, QuestInstance>> players = new HashMap<>();

    private final Map<UUID, Map<String, List<String>>> indexes = new HashMap<>();

    private final Map<UUID, String> names = new HashMap<>();

    private final Map<UUID, Long> revisions = new HashMap<>();

    private final Map<UUID, UUID> loading = new HashMap<>();

    private final Set<UUID> online = new HashSet<>();

    private final Set<UUID> pending = new HashSet<>();

    private final Set<UUID> dirty = new HashSet<>();

    private final Map<UUID, Map<String, String>> periodRolls = new HashMap<>();

    private final Map<UUID, String> bulkClaims = new HashMap<>();

    private final SeasonManager seasons = new SeasonManager();

    private final ProgressionManager progression = new ProgressionManager(this);

    private SchedulerUtil autosave;

    public QuestManager() {
        questManager = this;
        try {
            reloadDefinitions();
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
        TriggerManager.triggerManager.subscribe(this::handleTrigger);
    }

    public synchronized void reloadDefinitions() throws Exception {
        ConfigManager.configManager.reloadQuestConfigs(seasons);
    }

    public synchronized Map<String, YamlConfiguration> categories() {
        return ConfigManager.configManager.getCategoryConfigurations();
    }

    public synchronized List<String> displayCategories(QuestDefinition definition, QuestInstance instance) {
        if (instance != null && Set.of("period", "scroll").contains(instance.source())) {
            return definition.categories();
        }
        return ConfigManager.configManager.getDefinitions().getOrDefault(definition.catalogKey(), definition).categories();
    }

    private void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    public synchronized Collection<QuestDefinition> definitions() {
        return ConfigManager.configManager.getDefinitions().values().stream().filter(definition -> !definition.growth() && definition.listed()
                && !definition.isBattlePassTask()).toList();
    }

    public synchronized QuestDefinition definition(String id) {
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().get(id.toLowerCase(Locale.ROOT));
        return definition != null && !definition.growth() ? definition : null;
    }

    public synchronized QuestDefinition resolveDefinition(String key, ConfigurationSection section) {
        if (section == null) {
            throw new IllegalArgumentException("Missing quest instance: " + key);
        }
        String templateId = section.getString("template-id", "");
        String source = section.getString("source", "normal");
        if (source.equals("period")) {
            String group = section.getString("source-id", "");
            ConfigurationSection period = QuestPeriodManager.questPeriodManager.definitions().get(group);
            if (period == null) {
                return null;
            }
            ConfigurationSection configured = period.getConfigurationSection("source");
            if (!templateId.startsWith(group + "_") || configured == null) {
                throw new IllegalArgumentException("Invalid periodic quest reference: " + key);
            }
            String objective = configured.contains("pool") ? templateId.substring(group.length() + 1) : null;
            if (objective != null && !objectiveTemplates().containsKey(objective)) {
                return null;
            }
            return periodDefinition(group, objective, period);
        }
        if (source.equals("scroll")) {
            if (!QuestScrollManager.questScrollManager.definitions().containsKey(templateId)) {
                return null;
            }
            return QuestScrollManager.questScrollManager.definition(templateId, section.getStringList("objective-ids"));
        }
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().get(templateId);
        if (definition != null && source.equals("normal") && !definition.storageKey().equals(key)) {
            return null;
        }
        return definition;
    }

    public synchronized QuestInstance instance(UUID playerId, String id) {
        return players.getOrDefault(playerId, Map.of()).get(id.toLowerCase(Locale.ROOT));
    }

    public synchronized Collection<QuestInstance> instances(UUID playerId) {
        return players.getOrDefault(playerId, Map.of()).values().stream().filter(quest -> !quest.definition().growth() && quest.source().equals("normal")).toList();
    }

    public synchronized Collection<QuestDefinition> growthDefinitions(String type) {
        return ConfigManager.configManager.getDefinitions().values().stream().filter(definition -> definition.type().equals(type)).toList();
    }

    public synchronized QuestDefinition growthDefinition(String type, String id) {
        return ConfigManager.configManager.getDefinitions().get(type + "/" + id.toLowerCase(Locale.ROOT));
    }

    public synchronized QuestInstance growthInstance(UUID playerId, String type, String id) {
        if (id.contains("@") && Set.of("battlepass", "milestone").contains(type)) {
            String[] parts = id.split("@", 2);
            return instance(playerId, type + "/" + parts[0] + "/" + parts[1]);
        }
        QuestDefinition definition = growthDefinition(type, id);
        return definition == null ? null : instance(playerId, definition.storageKey());
    }

    public synchronized boolean ready(UUID playerId) {
        return players.containsKey(playerId);
    }

    @Override
    public void onInit() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runSync(player, () -> join(player));
        }
        configureAutoSave();
    }

    public synchronized void configureAutoSave() {
        if (autosave != null) {
            autosave.cancel();
            autosave = null;
        }
        if (ConfigManager.configManager.getBoolean("auto-save.enabled", true)) {
            long ticks = ConfigManager.configManager.config.getLong("auto-save.period-tick", 6000L);
            autosave = SchedulerUtil.runTaskTimerAsynchronously(() -> {
                if (!ConfigManager.configManager.getBoolean("auto-save.hide-message", false)) {
                    TextUtil.sendMessage(null, CommonUtil.parseLang(null,
                            ConfigManager.configManager.getString("auto-save.message", "")));
                }
                flush();
            }, 180L, ticks);
        }
    }

    public synchronized boolean isLoaded(Player player) {
        UUID id = player.getUniqueId();
        return online.contains(id) && players.containsKey(id) && !loading.containsKey(id);
    }

    public synchronized boolean hasTrigger(Player player, String id) {
        return !indexes.getOrDefault(player.getUniqueId(), Map.of())
                .getOrDefault("ultimatequest:" + id, List.of()).isEmpty();
    }

    public synchronized void join(Player player) {
        UUID id = player.getUniqueId();
        if (pending.contains(id) && !players.containsKey(id)) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return;
        }
        online.add(id);
        names.put(id, player.getName());
        if (players.containsKey(id)) {
            ensureGrowth(id);
            refreshPeriods(player);
            return;
        }
        if (loading.containsKey(id)) {
            return;
        }
        UUID token = UUID.randomUUID();
        loading.put(id, token);
        QuestStorage.questStorage.load(id).whenComplete((snapshot, failure) -> {
            SchedulerUtil.runSync(player, () -> finishLoad(player, token, snapshot, failure));
        });
    }

    private synchronized void finishLoad(Player player, UUID token, QuestPlayerData snapshot, Throwable failure) {
        UUID id = player.getUniqueId();
        if (!online.contains(id) || !token.equals(loading.get(id))) {
            return;
        }
        loading.remove(id);
        try {
            if (failure != null) {
                throw new IllegalStateException("Database load failed", failure);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(snapshot.payload());
            if (config.getInt("schema-version") != 4) {
                throw new IllegalStateException("Unsupported player data version");
            }
            Map<String, QuestInstance> quests = new LinkedHashMap<>();
            ConfigurationSection section = config.getConfigurationSection("quests");
            if (section != null) {
                for (String key : section.getKeys(false)) {
                    ConfigurationSection saved = section.getConfigurationSection(key);
                    QuestDefinition definition = resolveDefinition(key, saved);
                    if (definition != null) {
                        quests.put(key, new QuestInstance(key, saved, definition));
                    }
                }
            }
            Map<String, String> rolls = new HashMap<>();
            ConfigurationSection savedRolls = config.getConfigurationSection("period-rolls");
            if (savedRolls != null) {
                for (String group : savedRolls.getKeys(false)) {
                    String cycle = savedRolls.getString(group, "");
                    if (!group.matches("[a-z0-9_-]+") || !cycle.matches("(?:daily|weekly|monthly|scheduled)_-?[0-9]+")) {
                        throw new IllegalArgumentException("Invalid saved period cycle");
                    }
                    Long.parseLong(cycle.substring(cycle.indexOf('_') + 1));
                    rolls.put(group, cycle);
                }
            }
            players.put(id, quests);
            periodRolls.put(id, rolls);
            revisions.put(id, snapshot.revision());
            ensureGrowth(id);
            rebuildIndex(id);
            if (quests.values().stream().anyMatch(quest -> Set.of("CLAIMING", "REVIEW").contains(quest.state())
                    || quest.stageStates().values().stream().anyMatch(value -> Set.of("CLAIMING", "REVIEW").contains(value)))) {
                LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
            }
            refreshPeriods(player);
            QuestScrollManager.questScrollManager.synchronize(player);
            claimAutomatically(player);
            if (TriggerManager.triggerManager != null) {
                TriggerManager.triggerManager.runtime().playerReady(player);
            }
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Quest load failed for " + id + ": " + exception.getMessage());
            LanguageManager.languageManager.sendStringText(player, "quests.load-failed");
        }
    }

    public synchronized void quit(Player player) {
        UUID id = player.getUniqueId();
        QuestScrollManager.questScrollManager.leave(id);
        online.remove(id);
        loading.remove(id);
        if (players.containsKey(id) && !pending.contains(id)) {
            persist(id, () -> discardOffline(id), () -> {
            });
        }
    }

    private synchronized void discardOffline(UUID id) {
        if (online.contains(id) || pending.contains(id)) {
            return;
        }
        players.remove(id);
        indexes.remove(id);
        names.remove(id);
        revisions.remove(id);
        dirty.remove(id);
        periodRolls.remove(id);
        bulkClaims.remove(id);
    }

    public synchronized void accept(Player player, String rawId) {
        accept(player, rawId, false, false);
    }

    public synchronized void accept(Player player, String rawId, boolean administrative, boolean bypass) {
        UUID id = player.getUniqueId();
        if (!available(player)) {
            return;
        }
        QuestDefinition definition = definition(rawId);
        if (definition == null || !definition.listed() && !administrative || definition.isBattlePassTask()) {
            LanguageManager.languageManager.sendStringText(player, "quests.not-found");
            return;
        }
        Map<String, QuestInstance> quests = players.get(id);
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
        changed(id);
        persist(id, () -> {
            rebuildIndex(id);
            notifyPlayer(player, "quests.accepted", "quest", definition.name(player));
        }, () -> {
            quests.remove(definition.id());
            changed(id);
            notifyPlayer(player, "quests.save-failed");
        });
    }

    public synchronized boolean canAbandon(QuestInstance quest) {
        if (quest == null || quest.definition().growth() || !quest.source().equals("normal")
                || !Set.of("ACTIVE", "COMPLETED").contains(quest.state())) {
            return false;
        }
        QuestDefinition definition = ConfigManager.configManager.getDefinitions().getOrDefault(quest.definition().catalogKey(), quest.definition());
        return definition.settings().getBoolean("allow-abandon", true);
    }

    public synchronized boolean visibleInMenu(Player player, QuestDefinition definition, QuestInstance quest) {
        if (definition.growth() || quest != null && !quest.source().equals("normal")) {
            return true;
        }
        QuestDefinition current = ConfigManager.configManager.getDefinitions().getOrDefault(definition.catalogKey(), definition);
        return !current.settings().getBoolean("hide-in-menu", false)
                && (quest != null || !current.settings().getBoolean("auto-hide", false)
                || meetsRequirements(player, current));
    }

    public synchronized void abandon(Player player, String rawId) {
        UUID id = player.getUniqueId();
        if (!available(player)) {
            return;
        }
        QuestInstance quest = instance(id, rawId);
        if (!canAbandon(quest)) {
            LanguageManager.languageManager.sendStringText(player, "quests.cannot-abandon");
            return;
        }
        String questId = quest.key();
        players.get(id).remove(questId);
        changed(id);
        persist(id, () -> {
            rebuildIndex(id);
            notifyPlayer(player, "quests.abandoned", "quest", quest.definition().name(player));
        }, () -> {
            players.get(id).put(questId, quest);
            changed(id);
            notifyPlayer(player, "quests.save-failed");
        });
    }

    public synchronized void claim(Player player, String rawId) {
        UUID id = player.getUniqueId();
        if (!available(player)) {
            return;
        }
        QuestInstance quest = instance(id, rawId);
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

    public synchronized void claimStage(Player player, String type, String rawId, int level, String track) {
        if (!available(player)) {
            return;
        }
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
        changed(id);
        pending.add(id);
        QuestStorage.questStorage.saveCritical(snapshot(id)).whenComplete((ignored, failure) -> {
            if (failure != null) {
                synchronized (this) {
                    claimState(quest, key, "REVIEW");
                    pending.remove(id);
                    changed(id);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim reservation failed for " + id
                            + ": " + failure.getMessage());
                    bulkClaims.remove(id);
                }
                notifyPlayer(player, "quests.review-needed");
                return;
            }
            SchedulerUtil.runSync(player, () -> grant(player, quest, key), () -> {
                synchronized (this) {
                    claimState(quest, key, "REVIEW");
                    pending.remove(id);
                    bulkClaims.remove(id);
                    changed(id);
                    QuestStorage.questStorage.save(snapshot(id));
                }
            });
        });
    }

    private synchronized void grant(Player player, QuestInstance quest, String key) {
        UUID id = player.getUniqueId();
        ensureGrowth(id);
        if (!online.contains(id) || !assignmentActive(quest, Instant.now()) || (key != null && (!claimableGrowth(quest)
                || (key.startsWith("premium/") && !quest.premium(player))))) {
            claimState(quest, key, "REVIEW");
            changed(id);
            pending.remove(id);
            bulkClaims.remove(id);
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
            synchronized (this) {
                claimState(quest, key, failure == null ? "CLAIMED" : "REVIEW");
                changed(id);
                if (failure != null) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest reward delivery failed for " + id
                            + ": " + failure.getMessage());
                }
                QuestStorage.questStorage.saveCritical(snapshot(id)).whenComplete((saved, saveFailure) -> {
                    synchronized (this) {
                        pending.remove(id);
                        if (saveFailure != null) {
                            claimState(quest, key, "REVIEW");
                            changed(id);
                            ErrorManager.errorManager.sendErrorMessage("§cError: Quest claim result failed for"
                        + " " + id
                                    + ": " + saveFailure.getMessage());
                            dirty.add(id);
                        }
                        rebuildIndex(id);
                        if (saveFailure != null || failure != null) {
                            bulkClaims.remove(id);
                        }
                        if (saveFailure == null) {
                            if (failure == null && key == null && quest.source().equals("scroll")) {
                                SchedulerUtil.runSync(player,
                                        () -> QuestScrollManager.questScrollManager.complete(player, quest));
                            }
                            discardOffline(id);
                        }
                    }
                    notifyPlayer(player, failure == null && saveFailure == null ? "quests.claimed" : "quests.review-needed");
                });
            }
        });
    }

    private synchronized void handleTrigger(QuestContext context) {
        UUID id = context.player().getUniqueId();
        if (!players.containsKey(id)) {
            return;
        }
        ensureGrowth(id);
        refreshPeriods(context.player());
        List<String> candidates = List.copyOf(indexes.getOrDefault(id, Map.of()).getOrDefault(context.trigger().key().toString(), List.of()));
        for (String questId : candidates) {
            QuestInstance quest = instance(id, questId);
            if (quest == null || !quest.state().equals("ACTIVE") || !assignmentActive(quest, Instant.now())) {
                continue;
            }
            if (!challengeUnlocked(context.player(), quest.definition())) {
                continue;
            }
            if (!quest.definition().active(Instant.now())) {
                continue;
            }
            if (quest.source().equals("period")) {
                ConfigurationSection period = QuestPeriodManager.questPeriodManager.definitions().get(quest.sourceId());
                if (period == null || !TimeWindow.read(period).active(Instant.now())) {
                    continue;
                }
            }
            if (quest.source().equals("scroll") && !QuestScrollManager.questScrollManager.hasToken(
                    context.player(), UUID.fromString(quest.sourceId()))) {
                continue;
            }
            QuestDefinition liveDefinition = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
            if (liveDefinition != null && !liveDefinition.active(Instant.now())) {
                continue;
            }
            if (quest.source().equals("challenge")) {
                QuestInstance pass = growthInstance(id, "battlepass", quest.sourceId());
                if (pass == null || !pass.definition().season().equals(challengeSeason(quest.cycle()))) {
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
                            objective.contribution(context, quest.definition().storageKey() + "/" + objective.getId())))) {
                        changed(id);
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
                    changed(id);
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
                    changed(id);
                    LanguageManager.languageManager.sendStringText(context.player(), "battlepass.task-completed");
                    continue;
                }
                LanguageManager.languageManager.sendStringText(context.player(), "quests.completed");
                if (quest.definition().automaticClaim() && !pending.contains(id)) {
                    claim(context.player(), questId);
                }
            }
        }
    }

    private void ensureGrowth(UUID id) {
        Map<String, QuestInstance> quests = players.get(id);
        if (quests == null) {
            return;
        }
        boolean modified = false;
        for (QuestDefinition definition : ConfigManager.configManager.getDefinitions().values()) {
            if (!definition.growth()) {
                continue;
            }
            QuestInstance quest = quests.get(definition.storageKey());
            if (quest == null) {
                if (!definition.active(Instant.now())) {
                    continue;
                }
                quests.put(definition.storageKey(), new QuestInstance(definition));
                modified = true;
            } else if (quest.refreshDefinition(definition)) {
                modified = true;
            }
        }
        if (modified) {
            changed(id);
            rebuildIndex(id);
        }
    }

    private boolean currentGrowth(QuestInstance quest) {
        QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        return current != null && current.storageKey().equals(quest.definition().storageKey()) && current.active(Instant.now());
    }

    public synchronized void refreshGrowth(Player player) {
        ensureGrowth(player.getUniqueId());
        refreshChallenges(player);
    }

    public ProgressionManager progression() {
        return progression;
    }

    public synchronized void editSeason(CommandSender sender, String type, String pass, String operation, List<String> values) {
        seasons.editSeason(this, sender, type, pass, operation, values);
    }

    public synchronized void refreshSeasons() {
        seasons.refresh(this);
    }

    public synchronized boolean claimableGrowth(QuestInstance quest) {
        QuestDefinition current = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        return quest.definition().claimable(Instant.now())
                && current != null && !current.settings().getBoolean("paused", false);
    }

    public synchronized void manageProgression(CommandSender sender, UUID id, String type, String pass, String operation, List<String> values) {
        if (pending.contains(id) || loading.containsKey(id)) {
            LanguageManager.languageManager.sendStringText(sender, "quests.busy");
            return;
        }
        QuestDefinition definition = growthDefinition(type, pass.split("@", 2)[0]);
        if (definition == null) {
            LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
            return;
        }
        if (players.containsKey(id)) {
            QuestInstance quest = growthInstance(id, type, pass);
            if (quest == null) {
                LanguageManager.languageManager.sendStringText(sender, "quests.not-found");
                return;
            }
            try {
                progression.applyChange(quest, operation, values);
                changed(id);
                persist(id, () -> LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-saved"),
                        () -> LanguageManager.languageManager.sendStringText(sender, "quests.save-failed"));
            } catch (RuntimeException exception) {
                LanguageManager.languageManager.sendStringText(sender, "battlepass.admin-failed");
            }
            return;
        }
        pending.add(id);
        QuestStorage.questStorage.load(id).whenComplete((snapshot, failure) -> {
            if (failure != null) {
                finishAdmin(sender, id, failure);
                return;
            }
            try {
                YamlConfiguration data = new YamlConfiguration();
                data.loadFromString(snapshot.payload());
                if (data.getInt("schema-version") != 4) {
                    throw new IllegalStateException("Unsupported player data version");
                }
                String key = pass.contains("@") ? type + "/" + pass.replace('@', '/') : definition.storageKey();
                ConfigurationSection section = data.getConfigurationSection("quests." + key);
                QuestInstance quest = section == null ? new QuestInstance(definition) : new QuestInstance(key, section);
                if (!quest.key().equals(key)) {
                    throw new IllegalArgumentException("Archived season not found");
                }
                progression.applyChange(quest, operation, values);
                data.set("quests." + key, null);
                quest.write(data.createSection("quests." + key));
                QuestStorage.questStorage.saveCritical(new QuestPlayerData(id, snapshot.playerName(),
                        Math.addExact(snapshot.revision(), 1L), data.saveToString()))
                        .whenComplete((ignored, saveFailure) -> finishAdmin(sender, id, saveFailure));
            } catch (Exception exception) {
                finishAdmin(sender, id, exception);
            }
        });
    }

    private synchronized void finishAdmin(CommandSender sender, UUID id, Throwable failure) {
        pending.remove(id);
        if (failure != null) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Quest admin edit failed for " + id
                    + ": " + failure.getMessage());
        }
        SchedulerUtil.runSync(() -> LanguageManager.languageManager.sendStringText(sender,
                failure == null ? "battlepass.admin-saved" : "battlepass.admin-failed"));
    }

    public synchronized List<String> seasons(UUID playerId, String type, String pass) {
        return players.getOrDefault(playerId, Map.of()).values().stream()
                .filter(quest -> quest.definition().type().equals(type) && quest.definition().id().equals(pass))
                .map(quest -> quest.definition().season().isEmpty() ? pass : pass + "@" + quest.definition().season()).toList();
    }

    public synchronized void claimAll(Player player, String type, String pass) {
        if (!available(player)) {
            return;
        }
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
        bulkClaims.put(player.getUniqueId(), quest.key());
        claimAutomatically(player);
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
        Instant starts = growth.definition().startsAt();
        long elapsed = starts == null ? 0L : Math.max(0L, Instant.now().getEpochSecond() - starts.getEpochSecond());
        long week = elapsed / 604800L + 1L;
        int unlock = Math.max(1, definition.getUnlockWeek());
        return week >= unlock && (definition.allowsCatchUp() || week == unlock);
    }

    private void refreshChallenges(Player player) {
        Map<String, QuestInstance> quests = players.get(player.getUniqueId());
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
            String cycle = challengeCycle(growth.definition(), definition, Instant.now());
            long expires = challengeExpires(growth.definition(), definition, Instant.now());
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
                changed(player.getUniqueId());
                rebuildIndex(player.getUniqueId());
            }
        }
    }

    private String challengeCycle(QuestDefinition pass, QuestDefinition task, Instant now) {
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

    private long challengeExpires(QuestDefinition pass, QuestDefinition task, Instant now) {
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

    public synchronized boolean canAddExperience(Player player, String id) {
        ensureGrowth(player.getUniqueId());
        QuestInstance quest = growthInstance(player.getUniqueId(), "battlepass", id);
        return quest != null && currentGrowth(quest);
    }

    public synchronized boolean addExperience(Player player, String id, long amount) {
        if (amount <= 0L || !canAddExperience(player, id)) {
            return false;
        }
        QuestInstance quest = growthInstance(player.getUniqueId(), "battlepass", id);
        int previous = quest.level();
        if (!quest.addTotal(progression.boostedAmount(player, quest, amount))) {
            return false;
        }
        changed(player.getUniqueId());
        if (quest.level() > previous) {
            progression.levelFeedback(player, quest);
        }
        return true;
    }

    public synchronized void addExperienceAndSave(Player player, String id, long amount) {
        if (!available(player)) {
            return;
        }
        if (!addExperience(player, id, amount)) {
            LanguageManager.languageManager.sendStringText(player, "progression.unavailable");
            return;
        }
        persist(player.getUniqueId(), () -> notifyPlayer(player, "progression.xp-added"),
                () -> notifyPlayer(player, "progression.xp-save-failed"));
    }

    public synchronized Map<String, QuestDefinition> templates() {
        return Map.copyOf(ConfigManager.configManager.getDefinitions());
    }

    public synchronized Map<String, YamlConfiguration> objectiveTemplates() {
        return Map.copyOf(ConfigManager.configManager.getQuestConfigurations());
    }

    public synchronized boolean meetsRequirements(Player player, QuestDefinition definition) {
        return definition.playerAcceptable() && eligible(player, definition);
    }

    private boolean eligible(Player player, QuestDefinition definition) {
        Map<String, QuestInstance> quests = players.getOrDefault(player.getUniqueId(), Map.of());
        return definition.active(Instant.now()) && challengeUnlocked(player, definition)
                && definition.meetsConditions(player) && definition.prerequisites().stream().allMatch(id ->
                quests.containsKey(id) && quests.get(id).state().equals("CLAIMED"));
    }

    private boolean assignmentActive(QuestInstance quest, Instant now) {
        QuestDefinition live = ConfigManager.configManager.getDefinitions().get(quest.definition().catalogKey());
        if (quest.expired(now) || !quest.definition().claimable(now)
                || live != null && (live.settings().getBoolean("paused", false)
                || !quest.definition().growth() && !live.claimable(now))) {
            return false;
        }
        if (quest.source().equals("scroll")) {
            return scrollCarried(quest);
        }
        if (!quest.source().equals("period")) {
            return true;
        }
        YamlConfiguration period = QuestPeriodManager.questPeriodManager.definitions().get(quest.sourceId());
        return period != null && TimeWindow.read(period).claimable(now)
                && (quest.cycle().equals(QuestPeriodManager.cycle(period, now)) || quest.state().equals("COMPLETED"));
    }

    public synchronized List<QuestInstance> assignedInstances(UUID id, String source) {
        return players.getOrDefault(id, Map.of()).values().stream()
                .filter(quest -> quest.source().equals(source) && assignmentActive(quest, Instant.now())).toList();
    }

    public synchronized boolean hasScroll(UUID playerId, UUID token) {
        return players.getOrDefault(playerId, Map.of()).values().stream().anyMatch(quest ->
                quest.source().equals("scroll") && quest.sourceId().equals(token.toString()));
    }

    public synchronized QuestInstance scroll(UUID playerId, UUID token) {
        return players.getOrDefault(playerId, Map.of()).values().stream().filter(quest ->
                quest.source().equals("scroll") && quest.sourceId().equals(token.toString())).findFirst().orElse(null);
    }

    public synchronized Set<UUID> scrollTokens(UUID playerId) {
        return players.getOrDefault(playerId, Map.of()).values().stream()
                .filter(quest -> quest.source().equals("scroll"))
                .map(quest -> UUID.fromString(quest.sourceId())).collect(Collectors.toUnmodifiableSet());
    }

    public synchronized void detachScroll(UUID playerId, UUID token) {
        Map<String, QuestInstance> quests = players.get(playerId);
        if (quests == null || pending.contains(playerId)) {
            return;
        }
        List<String> removed = quests.entrySet().stream().filter(entry -> entry.getValue().source().equals("scroll")
                && entry.getValue().sourceId().equals(token.toString())).map(Map.Entry::getKey).toList();
        if (removed.isEmpty()) {
            return;
        }
        removed.forEach(quests::remove);
        changed(playerId);
        rebuildIndex(playerId);
    }

    private boolean scrollCarried(QuestInstance quest) {
        for (Map.Entry<UUID, Map<String, QuestInstance>> entry : players.entrySet()) {
            if (!entry.getValue().containsValue(quest)) {
                continue;
            }
            Player player = Bukkit.getPlayer(entry.getKey());
            return player != null && QuestScrollManager.questScrollManager.hasToken(
                    player, UUID.fromString(quest.sourceId()));
        }
        return false;
    }

    public synchronized void refreshPeriods(Player player) {
        refreshPeriods(player, Instant.now());
    }

    public synchronized void refreshPeriods(Player player, Instant now) {
        UUID id = player.getUniqueId();
        if (!players.containsKey(id) || pending.contains(id) || !online.contains(id)) {
            return;
        }
        Map<String, QuestInstance> quests = players.get(id);
        Map<String, String> rolls = periodRolls.computeIfAbsent(id, ignored -> new HashMap<>());
        Map<String, String> previousRolls = new HashMap<>(rolls);
        Map<String, QuestInstance> removed = new LinkedHashMap<>();
        List<String> added = new ArrayList<>();
        for (Map.Entry<String, YamlConfiguration> entry : QuestPeriodManager.questPeriodManager.definitions().entrySet()) {
            String group = entry.getKey();
            YamlConfiguration config = entry.getValue();
            if (!TimeWindow.read(config).active(now)) {
                continue;
            }
            String cycle = QuestPeriodManager.cycle(config, now);
            if (cycle.equals(rolls.get(group))) {
                continue;
            }
            String previousCycle = rolls.get(group);
            if (previousCycle != null && Long.parseLong(previousCycle.substring(previousCycle.indexOf('_') + 1))
                    >= QuestPeriodManager.start(config, now).toEpochSecond()) {
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
                        drawn.add(periodDefinition(group, objective, config));
                    }
                } else {
                    drawn.add(periodDefinition(group, null, config));
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
            long expires = QuestPeriodManager.end(config, now).plusSeconds(
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
        changed(id);
        persist(id, () -> {
            rebuildIndex(id);
            notifyPlayer(player, "period.ready");
        }, () -> {
            added.forEach(quests::remove);
            quests.putAll(removed);
            rolls.clear();
            rolls.putAll(previousRolls);
            changed(id);
            rebuildIndex(id);
            notifyPlayer(player, "quests.save-failed");
        });
    }

    private QuestDefinition periodDefinition(String group, String objective, ConfigurationSection period) {
    return ConfigManager.configManager.getPeriod(group).createDefinition(objective, objectiveTemplates());
    }

    public synchronized boolean beginScroll(Player player) {
        if (!available(player)) {
            return false;
        }
        pending.add(player.getUniqueId());
        return true;
    }

    public synchronized void endScroll(UUID id) {
        pending.remove(id);
        if (!online.contains(id) && players.containsKey(id)) {
            persist(id, () -> discardOffline(id), () -> {
            });
        }
    }

    public synchronized boolean canInstallScroll(Player player, List<QuestDefinition> drawn) {
        UUID id = player.getUniqueId();
        return !drawn.isEmpty() && drawn.size() <= 45 && players.containsKey(id) && online.contains(id) && pending.contains(id)
                && withinLimits(id, "scroll", drawn, Set.of())
                && drawn.stream().allMatch(definition -> !definition.growth() && eligible(player, definition));
    }

    private boolean withinLimits(UUID id, String source, List<QuestDefinition> incoming, Set<String> replacing) {
        Instant now = Instant.now();
        List<QuestInstance> active = players.getOrDefault(id, Map.of()).values().stream()
                .filter(quest -> !quest.definition().growth() && !quest.state().equals("CLAIMED")
                        && !replacing.contains(quest.key()) && assignmentActive(quest, now)).toList();
        return ConfigManager.configManager.getTaskLimits().allows(active, source, incoming);
    }

    public synchronized CompletableFuture<Boolean> installScroll(Player player, UUID token,
            List<QuestDefinition> drawn, QuestInstance restored, long expiresAt) {
        if (!canInstallScroll(player, drawn)) {
            endScroll(player.getUniqueId());
            return CompletableFuture.completedFuture(false);
        }
        UUID id = player.getUniqueId();
        QuestInstance quest = restored == null
                ? new QuestInstance(drawn.get(0), "scroll", token.toString(), "", expiresAt) : restored;
        if (!quest.source().equals("scroll") || !quest.sourceId().equals(token.toString())) {
            endScroll(id);
            return CompletableFuture.completedFuture(false);
        }
        QuestInstance previous = scroll(id, token);
        if (previous != null) {
            players.get(id).remove(previous.key());
        }
        players.get(id).put(quest.key(), quest);
        changed(id);
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        QuestStorage.questStorage.saveCritical(snapshot(id)).whenComplete((ignored, failure) -> {
            synchronized (this) {
                if (failure != null) {
                    quest.state("REVIEW");
                    changed(id);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest scroll activation failed for " + id
                            + ": " + failure.getMessage());
                }
                pending.remove(id);
                rebuildIndex(id);
                result.complete(failure == null);
                if (failure == null) {
                    discardOffline(id);
                }
            }
        });
        return result;
    }

    private boolean available(Player player) {
        UUID id = player.getUniqueId();
        if (!players.containsKey(id)) {
            join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return false;
        }
        if (pending.contains(id)) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return false;
        }
        ensureGrowth(id);
        return true;
    }

    private void rebuildIndex(UUID id) {
        Map<String, List<String>> index = new HashMap<>();
        for (QuestInstance quest : players.getOrDefault(id, Map.of()).values()) {
            if (!quest.state().equals("ACTIVE")) {
                continue;
            }
            for (String trigger : quest.definition().objectives().stream().map(QuestObjective::trigger).distinct().toList()) {
                index.computeIfAbsent(trigger, ignored -> new ArrayList<>()).add(quest.key());
            }
        }
        indexes.put(id, index);
    }

    private void changed(UUID id) {
        revisions.merge(id, 1L, Long::sum);
        dirty.add(id);
    }

    private QuestPlayerData snapshot(UUID id) {
        YamlConfiguration config = new YamlConfiguration();
        config.set("schema-version", 4);
        periodRolls.getOrDefault(id, Map.of()).forEach((group, cycle) -> config.set("period-rolls." + group, cycle));
        for (QuestInstance quest : players.getOrDefault(id, Map.of()).values()) {
            quest.write(config.createSection("quests." + quest.key()));
        }
        return new QuestPlayerData(id, names.getOrDefault(id, ""), revisions.getOrDefault(id, 0L), config.saveToString());
    }

    private void persist(UUID id, Runnable success, Runnable failureHandler) {
        pending.add(id);
        QuestStorage.questStorage.saveCritical(snapshot(id)).whenComplete((ignored, failure) -> {
            synchronized (this) {
                pending.remove(id);
                if (failure == null) {
                    success.run();
                    discardOffline(id);
                } else {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest save failed for " + id
                            + ": " + failure.getMessage());
                    failureHandler.run();
                }
            }
        });
    }

    private synchronized void flush() {
        for (UUID id : List.copyOf(dirty)) {
            if (!pending.contains(id) && players.containsKey(id)) {
                QuestStorage.questStorage.save(snapshot(id));
            }
        }
    }

    private void notifyPlayer(Player player, String key, String... args) {
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

    private synchronized void claimAutomatically(Player player) {
        UUID id = player.getUniqueId();
        if (!online.contains(id) || pending.contains(id)) {
            return;
        }
        for (QuestInstance quest : players.getOrDefault(id, Map.of()).values()) {
            String requested = bulkClaims.get(id);
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
        bulkClaims.remove(id);
    }

    @Override
    public synchronized void onPluginDisable() {
        if (autosave != null) {
            autosave.cancel();
        }
        flush();
        progression.close();
        questManager = null;
    }
}
