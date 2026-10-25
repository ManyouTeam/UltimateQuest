package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class CacheManager extends AbstractManager {

    public static CacheManager cacheManager;

    private final Map<UUID, ObjectCache> playerCacheMap = new ConcurrentHashMap<>();

    private final Map<UUID, UUID> scrollInFlight = new ConcurrentHashMap<>();

    private final Map<UUID, CompletableFuture<Void>> scrollWrites = new ConcurrentHashMap<>();

    private final Set<CompletableFuture<Void>> writes = ConcurrentHashMap.newKeySet();

    private final Object lifecycleLock = new Object();

    private final Object scrollWriteLock = new Object();

    private SchedulerUtil autosave;

    private volatile boolean stopping;

    public CacheManager() {
        cacheManager = this;
    }

    @Override
    public void onInit() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            SchedulerUtil.runSync(player, () -> join(player));
        }
        configureAutoSave();
    }

    private <T> T read(UUID id, T fallback, Function<ObjectCache, T> reader) {
        while (true) {
            ObjectCache cache = playerCacheMap.get(id);
            if (cache == null) {
                return fallback;
            }
            synchronized (cache) {
                if (isCurrent(cache)) {
                    return reader.apply(cache);
                }
            }
        }
    }

    public boolean isCurrent(ObjectCache cache) {
        return cache != null && playerCacheMap.get(cache.playerId()) == cache;
    }

    public boolean isSession(ObjectCache cache, Player player) {
        synchronized (cache) {
            return !stopping && isCurrent(cache) && cache.session() == player && player.isOnline();
        }
    }

    public ObjectCache getObjectCache(UUID id) {
        return playerCacheMap.get(id);
    }

    public ObjectCache snapshot(UUID id) {
        return read(id, null, cache -> cache.ready() ? cache.copy() : null);
    }

    public QuestInstance instance(UUID id, String key) {
        return read(id, null, cache -> {
            QuestInstance quest = cache.ready() ? cache.quests().get(key.toLowerCase(Locale.ROOT)) : null;
            return quest == null ? null : new QuestInstance(quest);
        });
    }

    public QuestInstance liveInstance(UUID id, String key) {
        return questsOrEmpty(id).get(key.toLowerCase(Locale.ROOT));
    }

    public Collection<QuestInstance> instances(UUID id) {
        return read(id, List.of(), cache -> cache.ready() ? cache.quests().values().stream()
                .filter(quest -> !quest.definition().growth() && quest.source().equals("normal"))
                .map(QuestInstance::new).toList() : List.of());
    }

    public boolean hasTrigger(Player player, String id) {
        return read(player.getUniqueId(), false, cache -> cache.ready()
                && !cache.index().getOrDefault("ultimatequest:" + id, List.of()).isEmpty());
    }

    public List<String> seasons(UUID id, String type, String pass) {
        return read(id, List.of(), cache -> cache.ready() ? cache.quests().values().stream()
                .filter(quest -> quest.definition().type().equals(type) && quest.definition().id().equals(pass))
                .map(quest -> quest.season().isEmpty() ? pass : pass + "@" + quest.season()).toList() : List.of());
    }

    public boolean hasScroll(UUID id, UUID token) {
        return read(id, false, cache -> cache.ready() && cache.quests().values().stream().anyMatch(quest ->
                quest.source().equals("scroll") && quest.sourceId().equals(token.toString())));
    }

    public QuestInstance scroll(UUID id, UUID token) {
        return read(id, null, cache -> cache.ready() ? cache.quests().values().stream()
                .filter(quest -> quest.source().equals("scroll") && quest.sourceId().equals(token.toString()))
                .findFirst().map(QuestInstance::new).orElse(null) : null);
    }

    public Set<UUID> scrollTokens(UUID id) {
        return read(id, Set.of(), cache -> cache.ready() ? cache.quests().values().stream()
                .filter(quest -> quest.source().equals("scroll"))
                .map(quest -> UUID.fromString(quest.sourceId())).collect(Collectors.toUnmodifiableSet()) : Set.of());
    }

    public boolean available(Player player) {
        if (stopping || !player.isOnline()) {
            return false;
        }
        if (!isLoaded(player)) {
            join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return false;
        }
        if (isPending(player.getUniqueId())) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return false;
        }
        return true;
    }

    public UUID beginPlayerOperation(Player player) {
        return read(player.getUniqueId(), null, cache -> {
            if (!isSession(cache, player) || !cache.ready() || cache.pending()) {
                return null;
            }
            cache.pending(true);
            return cache.operationToken();
        });
    }

    public void endPlayerOperation(ObjectCache cache, UUID operation) {
        synchronized (cache) {
            if (!isCurrent(cache) || !cache.operation(operation)) {
                return;
            }
            cache.finishOperation(operation);
            if (!cache.online() && cache.ready()) {
                save(cache.playerId());
            }
            discardOffline(cache.playerId());
        }
    }

    public Map<String, QuestInstance> quests(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null) {
            return null;
        }
        if (!Thread.holdsLock(cache)) {
            throw new IllegalStateException("Live quest access requires the player cache lock");
        }
        return cache.ready() ? cache.quests() : null;
    }

    public Map<String, QuestInstance> questsOrEmpty(UUID id) {
        Map<String, QuestInstance> quests = quests(id);
        return quests == null ? Map.of() : quests;
    }

    public boolean isReady(UUID id) {
        return !stopping && read(id, false, ObjectCache::ready);
    }

    public boolean isOnline(UUID id) {
        return !stopping && read(id, false, ObjectCache::online);
    }

    public boolean isLoading(UUID id) {
        return read(id, false, cache -> cache.loadingToken() != null);
    }

    public boolean isPending(UUID id) {
        return read(id, false, ObjectCache::pending);
    }

    public boolean isLoaded(Player player) {
        return read(player.getUniqueId(), false, cache -> isSession(cache, player)
                && cache.ready() && cache.loadingToken() == null);
    }

    public boolean isStopping() {
        return stopping;
    }

    public void pending(UUID id, boolean pending) {
        read(id, null, cache -> {
            cache.pending(pending);
            return null;
        });
    }

    public void changed(UUID id) {
        read(id, null, cache -> {
            if (cache.ready()) {
                cache.changed();
            }
            return null;
        });
    }

    public void markDirty(UUID id) {
        read(id, null, cache -> {
            if (cache.ready()) {
                cache.dirty(true);
            }
            return null;
        });
    }

    public Map<String, List<String>> index(UUID id) {
        return read(id, Map.of(), cache -> cache.ready() ? Map.copyOf(cache.index()) : Map.of());
    }

    public void index(UUID id, Map<String, List<String>> index) {
        read(id, null, cache -> {
            if (cache.ready()) {
                cache.index().clear();
                index.forEach((key, value) -> cache.index().put(key, List.copyOf(value)));
            }
            return null;
        });
    }

    public String bulkClaim(UUID id) {
        return read(id, null, ObjectCache::bulkClaim);
    }

    public void bulkClaim(UUID id, String quest) {
        read(id, null, cache -> {
            cache.bulkClaim(quest);
            return null;
        });
    }

    public void join(Player player) {
        if (stopping || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        while (!stopping) {
            ObjectCache cache = playerCacheMap.computeIfAbsent(id, ObjectCache::new);
            synchronized (cache) {
                if (!isCurrent(cache)) {
                    continue;
                }
                if (stopping) {
                    return;
                }
                cache.session(player);
                cache.playerName(player.getName());
                if (cache.pending() && !cache.ready()) {
                    LanguageManager.languageManager.sendStringText(player, "quests.busy");
                    return;
                }
                if (cache.ready()) {
                    QuestManager.questManager.playerLoaded(player);
                    return;
                }
                if (cache.loadingToken() != null) {
                    return;
                }
                UUID token = UUID.randomUUID();
                cache.loadingToken(token);
                QuestStorage.questStorage.load(id).whenComplete((snapshot, failure) -> {
                    try {
                        if (stopping) {
                            cancelLoad(cache, token);
                        } else {
                            SchedulerUtil.runSync(player, () -> finishLoad(player, cache, token, snapshot, failure),
                                    () -> cancelLoad(cache, token));
                        }
                    } catch (RuntimeException exception) {
                        cancelLoad(cache, token);
                        error("Quest load could not be scheduled for " + id, exception);
                    }
                });
                return;
            }
        }
    }

    private void cancelLoad(ObjectCache cache, UUID token) {
        synchronized (cache) {
            if (isCurrent(cache) && token.equals(cache.loadingToken())) {
                cache.loadingToken(null);
                cache.session(null);
                discardOffline(cache.playerId());
            }
        }
    }

    private void finishLoad(Player player, ObjectCache cache, UUID token, QuestPlayerData snapshot, Throwable failure) {
        synchronized (cache) {
            if (!isSession(cache, player) || !token.equals(cache.loadingToken())) {
                return;
            }
            cache.loadingToken(null);
            try {
                if (failure != null) {
                    throw new IllegalStateException("Database load failed", failure);
                }
                cache.load(snapshot);
                QuestManager.questManager.playerLoaded(player);
            } catch (Exception exception) {
                error("Quest load failed for " + cache.playerId(), exception);
                LanguageManager.languageManager.sendStringText(player, "quests.load-failed");
            }
        }
    }

    public void quit(Player player) {
        ObjectCache cache = playerCacheMap.get(player.getUniqueId());
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            if (!isCurrent(cache) || cache.session() != player) {
                return;
            }
            cache.session(null);
            cache.loadingToken(null);
            cache.carriedScrolls().clear();
            cache.activationRetries().clear();
            if (cache.ready() && !cache.pending()) {
                save(cache.playerId());
            }
            discardOffline(cache.playerId());
        }
    }

    public void discardOffline(UUID id) {
        read(id, null, cache -> {
            if (!cache.online() && cache.loadingToken() == null && !cache.pending()
                    && cache.saveTail().isDone() && !cache.dirty()) {
                playerCacheMap.remove(id, cache);
            }
            return null;
        });
    }

    public CompletableFuture<Void> saveCritical(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player cache is not loaded"));
        }
        ObjectCache copy;
        CompletableFuture<Void> previous;
        CompletableFuture<Void> result = new CompletableFuture<>();
        synchronized (cache) {
            if (!isCurrent(cache) || !cache.ready()) {
                return CompletableFuture.failedFuture(new IllegalStateException("Player cache is not loaded"));
            }
            copy = cache.copy();
            previous = cache.saveTail();
            cache.saveTail(result);
            writes.add(result);
        }
        previous.handle((ignored, failure) -> null).thenComposeAsync(ignored -> {
            QuestPlayerData snapshot = copy.snapshot();
            return QuestStorage.questStorage.saveCritical(snapshot).thenRun(() -> {
                synchronized (cache) {
                    if (isCurrent(cache)) {
                        cache.saved(snapshot.revision());
                    }
                }
            });
        }).whenComplete((ignored, failure) -> {
            finishWrite(result, failure);
            discardOffline(id);
        });
        return result;
    }

    public void save(UUID id) {
        saveCritical(id).whenComplete((ignored, failure) -> {
            if (failure != null) {
                error("Quest save failed for " + id, failure);
            }
        });
    }

    public void persist(UUID id, Runnable success, Runnable failureHandler) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null) {
            failureHandler.run();
            return;
        }
        UUID operation;
        synchronized (cache) {
            if (!isCurrent(cache) || !cache.ready()) {
                failureHandler.run();
                return;
            }
            cache.pending(true);
            operation = cache.operationToken();
        }
        saveCritical(id).whenComplete((ignored, failure) -> {
            synchronized (cache) {
                if (!isCurrent(cache) || !cache.operation(operation)) {
                    return;
                }
                cache.finishOperation(operation);
                if (failure == null) {
                    success.run();
                } else {
                    error("Quest save failed for " + id, failure);
                    failureHandler.run();
                }
            }
            discardOffline(id);
        });
    }

    public CompletableFuture<Void> editOffline(UUID id, Consumer<YamlConfiguration> editor) {
        ObjectCache cache = playerCacheMap.computeIfAbsent(id, ObjectCache::new);
        UUID operation;
        CompletableFuture<Void> result = new CompletableFuture<>();
        synchronized (cache) {
            if (stopping || !isCurrent(cache) || cache.ready() || cache.pending() || cache.loadingToken() != null) {
                return CompletableFuture.failedFuture(new IllegalStateException("Player data is busy"));
            }
            cache.pending(true);
            operation = cache.operationToken();
            cache.saveTail(result);
            writes.add(result);
        }
        QuestStorage.questStorage.load(id).thenComposeAsync(snapshot -> {
            try {
                YamlConfiguration data = new YamlConfiguration();
                data.loadFromString(snapshot.payload());
                editor.accept(data);
                return QuestStorage.questStorage.saveCritical(new QuestPlayerData(id, snapshot.playerName(),
                        Math.addExact(snapshot.revision(), 1L), data.saveToString()));
            } catch (Exception exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }).whenComplete((ignored, failure) -> {
            Player session;
            synchronized (cache) {
                cache.finishOperation(operation);
                session = cache.session();
            }
            try {
                if (session != null && !stopping) {
                    SchedulerUtil.runSync(session, () -> {
                        if (isSession(cache, session)) {
                            join(session);
                        }
                    });
                }
            } catch (RuntimeException exception) {
                error("Quest reload could not be scheduled for " + id, exception);
            } finally {
                finishWrite(result, failure);
                discardOffline(id);
            }
        });
        return result;
    }

    public void configureAutoSave() {
        boolean enabled = ConfigManager.configManager.getBoolean("auto-save.enabled", true);
        long ticks = Math.max(1L, ConfigManager.configManager.config.getLong("auto-save.period-tick", 6000L));
        synchronized (lifecycleLock) {
            if (autosave != null) {
                autosave.cancel();
                autosave = null;
            }
            if (!stopping && enabled) {
                autosave = SchedulerUtil.runTaskTimerAsynchronously(() -> {
                    if (!stopping) {
                        if (!ConfigManager.configManager.getBoolean("auto-save.hide-message", false)) {
                            TextUtil.sendMessage(null, CommonUtil.parseLang(null,
                                    ConfigManager.configManager.getString("auto-save.message", "")));
                        }
                        flush(false);
                    }
                }, 180L, ticks);
            }
        }
    }

    private void flush(boolean shutdown) {
        for (ObjectCache cache : List.copyOf(playerCacheMap.values())) {
            synchronized (cache) {
                if (isCurrent(cache) && cache.ready() && (shutdown || cache.dirty() && !cache.pending())) {
                    save(cache.playerId());
                }
            }
        }
    }

    public boolean beginScroll(UUID token, UUID operation) {
        return !stopping && scrollInFlight.putIfAbsent(token, operation) == null;
    }

    public boolean scrollInFlight(UUID token) {
        return scrollInFlight.containsKey(token);
    }

    public boolean scrollOperation(UUID token, UUID operation) {
        return operation != null && operation.equals(scrollInFlight.get(token));
    }

    public void endScroll(UUID token, UUID operation) {
        scrollInFlight.remove(token, operation);
    }

    public boolean canActivateScroll(UUID player, UUID token) {
        return !stopping && read(player, false, cache -> cache.ready()
                && cache.activationRetries().getOrDefault(token, 0L) <= System.currentTimeMillis());
    }

    public boolean deferScroll(UUID player, UUID token, boolean permanent) {
        return read(player, false, cache -> {
            long retryAt = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 10_000L;
            Long previous = cache.activationRetries().put(token, retryAt);
            return previous == null || permanent && previous != Long.MAX_VALUE;
        });
    }

    public void carriedScrolls(UUID player, Set<UUID> tokens) {
        read(player, null, cache -> {
            cache.carriedScrolls().clear();
            cache.carriedScrolls().addAll(tokens);
            cache.activationRetries().keySet().retainAll(tokens);
            return null;
        });
    }

    public void removeCarriedScroll(UUID player, UUID token) {
        read(player, null, cache -> {
            cache.carriedScrolls().remove(token);
            return null;
        });
    }

    public CompletableFuture<QuestPlayerData> loadScroll(UUID token) {
        return QuestStorage.questStorage.loadScroll(token);
    }

    public CompletableFuture<Void> saveScroll(QuestPlayerData snapshot) {
        return writeScroll(snapshot.playerUUID(), () -> QuestStorage.questStorage.saveScroll(snapshot));
    }

    public CompletableFuture<Void> updateScroll(UUID token, String playerName, Consumer<YamlConfiguration> update) {
        return writeScroll(token, () -> loadScroll(token).thenCompose(snapshot -> {
            try {
                YamlConfiguration record = new YamlConfiguration();
                record.loadFromString(snapshot.payload());
                update.accept(record);
                return QuestStorage.questStorage.saveScroll(new QuestPlayerData(token,
                        playerName.isEmpty() ? snapshot.playerName() : playerName,
                        Math.addExact(snapshot.revision(), 1L), record.saveToString()));
            } catch (Exception exception) {
                return CompletableFuture.failedFuture(exception);
            }
        }));
    }

    public CompletableFuture<QuestPlayerData> editScroll(UUID token, Function<QuestPlayerData, QuestPlayerData> editor) {
        AtomicReference<QuestPlayerData> edited = new AtomicReference<>();
        return writeScroll(token, () -> loadScroll(token).thenCompose(snapshot -> {
            QuestPlayerData next = editor.apply(snapshot);
            edited.set(next);
            return QuestStorage.questStorage.saveScroll(next);
        })).thenApply(ignored -> edited.get());
    }

    private CompletableFuture<Void> writeScroll(UUID token, Supplier<CompletableFuture<Void>> write) {
        CompletableFuture<Void> previous;
        CompletableFuture<Void> result = new CompletableFuture<>();
        synchronized (scrollWriteLock) {
            if (stopping) {
                return CompletableFuture.failedFuture(new IllegalStateException("Player cache is shutting down"));
            }
            previous = scrollWrites.getOrDefault(token, CompletableFuture.completedFuture(null));
            scrollWrites.put(token, result);
            writes.add(result);
        }
        previous.handle((ignored, failure) -> null).thenComposeAsync(ignored -> write.get()).whenComplete((ignored, failure) -> {
            scrollWrites.remove(token, result);
            finishWrite(result, failure);
        });
        return result;
    }

    private void finishWrite(CompletableFuture<Void> result, Throwable failure) {
        try {
            if (failure == null) {
                result.complete(null);
            } else {
                result.completeExceptionally(failure);
            }
        } finally {
            writes.remove(result);
        }
    }

    private void error(String message, Throwable failure) {
        ErrorManager.errorManager.sendErrorMessage("§cError: " + message + ": " + failure.getMessage());
    }

    @Override
    public void onPluginDisable() {
        synchronized (scrollWriteLock) {
            stopping = true;
        }
        synchronized (lifecycleLock) {
            if (autosave != null) {
                autosave.cancel();
                autosave = null;
            }
        }
        for (ObjectCache cache : List.copyOf(playerCacheMap.values())) {
            synchronized (cache) {
                cache.loadingToken(null);
                cache.session(null);
                cache.pending(false);
                if (cache.ready()) {
                    for (QuestInstance quest : cache.quests().values()) {
                        if (quest.state().equals("CLAIMING")) {
                            quest.state("REVIEW");
                            cache.changed();
                        }
                        for (Map.Entry<String, String> stage : quest.stageStates().entrySet()) {
                            if (stage.getValue().equals("CLAIMING")) {
                                quest.stageState(stage.getKey(), "REVIEW");
                                cache.changed();
                            }
                        }
                    }
                }
            }
        }
    }

    private void awaitWrites() {
        while (!writes.isEmpty()) {
            CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new)).handle((ignored, failure) -> null).join();
        }
        DatabaseExecutor.await();
    }

    public void finishShutdown() {
        awaitWrites();
        flush(true);
        awaitWrites();
        playerCacheMap.clear();
        scrollInFlight.clear();
        scrollWrites.clear();
    }
}
