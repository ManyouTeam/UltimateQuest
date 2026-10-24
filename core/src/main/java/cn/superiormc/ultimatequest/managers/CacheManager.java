package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;

public final class CacheManager extends AbstractManager {

    public static CacheManager cacheManager;

    private final Map<UUID, ObjectCache> playerCacheMap = new HashMap<>();

    private final Set<UUID> scrollInFlight = new HashSet<>();

    private final Map<UUID, CompletableFuture<Void>> scrollWrites = new HashMap<>();

    private SchedulerUtil autosave;

    private boolean stopping;

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

    public synchronized QuestInstance instance(UUID playerId, String id) {
        return questsOrEmpty(playerId).get(id.toLowerCase(Locale.ROOT));
    }

    public synchronized Collection<QuestInstance> instances(UUID playerId) {
        return questsOrEmpty(playerId).values().stream().filter(quest -> !quest.definition().growth() && quest.source().equals("normal")).toList();
    }

    public synchronized boolean hasTrigger(Player player, String id) {
        return !index(player.getUniqueId())
                .getOrDefault("ultimatequest:" + id, List.of()).isEmpty();
    }

    public synchronized List<String> seasons(UUID playerId, String type, String pass) {
        return questsOrEmpty(playerId).values().stream()
                .filter(quest -> quest.definition().type().equals(type) && quest.definition().id().equals(pass))
                .map(quest -> quest.season().isEmpty() ? pass : pass + "@" + quest.season()).toList();
    }

    public synchronized boolean hasScroll(UUID playerId, UUID token) {
        return questsOrEmpty(playerId).values().stream().anyMatch(quest ->
                quest.source().equals("scroll") && quest.sourceId().equals(token.toString()));
    }

    public synchronized QuestInstance scroll(UUID playerId, UUID token) {
        return questsOrEmpty(playerId).values().stream().filter(quest ->
                quest.source().equals("scroll") && quest.sourceId().equals(token.toString())).findFirst().orElse(null);
    }

    public synchronized Set<UUID> scrollTokens(UUID playerId) {
        return questsOrEmpty(playerId).values().stream()
                .filter(quest -> quest.source().equals("scroll"))
                .map(quest -> UUID.fromString(quest.sourceId())).collect(Collectors.toUnmodifiableSet());
    }

    public synchronized boolean available(Player player) {
        UUID id = player.getUniqueId();
        if (stopping || !player.isOnline()) {
            return false;
        }
        if (!isLoaded(player)) {
            join(player);
            LanguageManager.languageManager.sendStringText(player, "quests.loading");
            return false;
        }
        if (isPending(id)) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return false;
        }
        return true;
    }

    public synchronized boolean beginPlayerOperation(Player player) {
        if (!available(player)) {
            return false;
        }
        pending(player.getUniqueId(), true);
        return true;
    }

    public synchronized void endPlayerOperation(UUID id) {
        pending(id, false);
        if (!isOnline(id) && quests(id) != null) {
            persist(id, () -> discardOffline(id), () -> {
            });
        }
    }

    public synchronized ObjectCache getObjectCache(UUID id) {
        return playerCacheMap.get(id);
    }

    public synchronized Map<String, QuestInstance> quests(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return cache != null && cache.ready() ? cache.quests() : null;
    }

    public synchronized Map<String, QuestInstance> questsOrEmpty(UUID id) {
        Map<String, QuestInstance> quests = quests(id);
        return quests == null ? Map.of() : quests;
    }

    public synchronized List<ObjectCache> loadedCaches() {
        return playerCacheMap.values().stream().filter(ObjectCache::ready).toList();
    }

    public synchronized boolean isReady(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return !stopping && cache != null && cache.ready();
    }

    public synchronized boolean isOnline(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return !stopping && cache != null && cache.online();
    }

    public synchronized boolean isLoading(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return cache != null && cache.loadingToken() != null;
    }

    public synchronized boolean isPending(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return cache != null && cache.pending();
    }

    public synchronized boolean isLoaded(Player player) {
        UUID id = player.getUniqueId();
        return isOnline(id) && isReady(id) && !isLoading(id);
    }

    public synchronized boolean isStopping() {
        return stopping;
    }

    public synchronized void pending(UUID id, boolean pending) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null) {
            cache.pending(pending);
        }
    }

    public synchronized void changed(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null && cache.ready()) {
            cache.changed();
        }
    }

    public synchronized void markDirty(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null && cache.ready()) {
            cache.dirty(true);
        }
    }

    public synchronized Map<String, List<String>> index(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return cache != null && cache.ready() ? cache.index() : Map.of();
    }

    public synchronized void index(UUID id, Map<String, List<String>> index) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null && cache.ready()) {
            cache.index().clear();
            cache.index().putAll(index);
        }
    }

    public synchronized String bulkClaim(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        return cache == null ? null : cache.bulkClaim();
    }

    public synchronized void bulkClaim(UUID id, String quest) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null) {
            cache.bulkClaim(quest);
        }
    }

    public synchronized void join(Player player) {
        if (stopping || !player.isOnline()) {
            return;
        }
        UUID id = player.getUniqueId();
        ObjectCache cache = playerCacheMap.computeIfAbsent(id, ObjectCache::new);
        cache.online(true);
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
            synchronized (this) {
                if (isStopping()) {
                    return;
                }
                try {
                    SchedulerUtil.runSync(player, () -> finishLoad(player, cache, token, snapshot, failure),
                            () -> cancelLoad(cache, token));
                } catch (RuntimeException exception) {
                    cancelLoad(cache, token);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Quest load could not be scheduled for " + id
                            + ": " + exception.getMessage());
                }
            }
        });
    }

    private synchronized void cancelLoad(ObjectCache cache, UUID token) {
        if (playerCacheMap.get(cache.playerId()) == cache && token.equals(cache.loadingToken())) {
            cache.loadingToken(null);
            cache.online(false);
            discardOffline(cache.playerId());
        }
    }

    private synchronized void finishLoad(Player player, ObjectCache cache, UUID token,
            QuestPlayerData snapshot, Throwable failure) {
        UUID id = player.getUniqueId();
        if (stopping || playerCacheMap.get(id) != cache || !cache.online() || !player.isOnline()
                || !token.equals(cache.loadingToken())) {
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
            ErrorManager.errorManager.sendErrorMessage("§cError: Quest load failed for " + id + ": " + exception.getMessage());
            LanguageManager.languageManager.sendStringText(player, "quests.load-failed");
        }
    }

    public synchronized void quit(Player player) {
        UUID id = player.getUniqueId();
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null) {
            return;
        }
        cache.online(false);
        cache.loadingToken(null);
        cache.carriedScrolls().clear();
        cache.activationRetries().clear();
        if (!cache.ready()) {
            discardOffline(id);
        } else if (!cache.pending()) {
            persist(id, () -> discardOffline(id), () -> {
            });
        }
    }

    public synchronized void discardOffline(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null && !cache.online() && !cache.pending() && !cache.dirty()) {
            playerCacheMap.remove(id, cache);
        }
    }

    public synchronized CompletableFuture<Void> saveCritical(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null || !cache.ready()) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player cache is not loaded"));
        }
        QuestPlayerData snapshot = cache.snapshot();
        return QuestStorage.questStorage.saveCritical(snapshot).whenComplete((ignored, failure) -> {
            synchronized (this) {
                if (playerCacheMap.get(id) == cache && failure == null) {
                    cache.saved(snapshot.revision());
                }
            }
        });
    }

    public synchronized void save(UUID id) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache != null && cache.ready()) {
            saveCritical(id).whenComplete((ignored, failure) -> {
                synchronized (this) {
                    if (failure != null) {
                        ErrorManager.errorManager.sendErrorMessage("§cError: Quest save failed for " + id
                                + ": " + failure.getMessage());
                    } else if (playerCacheMap.get(id) == cache) {
                        discardOffline(id);
                    }
                }
            });
        }
    }

    public synchronized void persist(UUID id, Runnable success, Runnable failureHandler) {
        ObjectCache cache = playerCacheMap.get(id);
        if (cache == null || !cache.ready()) {
            failureHandler.run();
            return;
        }
        cache.pending(true);
        saveCritical(id).whenComplete((ignored, failure) -> {
            synchronized (this) {
                if (playerCacheMap.get(id) != cache) {
                    return;
                }
                cache.pending(false);
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

    public synchronized CompletableFuture<Void> editOffline(UUID id, Consumer<YamlConfiguration> editor) {
        if (stopping || isReady(id) || isPending(id) || isLoading(id)) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player data is busy"));
        }
        ObjectCache cache = playerCacheMap.computeIfAbsent(id, ObjectCache::new);
        cache.pending(true);
        return QuestStorage.questStorage.load(id).thenCompose(snapshot -> {
            synchronized (this) {
                try {
                    YamlConfiguration data = new YamlConfiguration();
                    data.loadFromString(snapshot.payload());
                    editor.accept(data);
                    return QuestStorage.questStorage.saveCritical(new QuestPlayerData(id, snapshot.playerName(),
                            Math.addExact(snapshot.revision(), 1L), data.saveToString()));
                } catch (Exception exception) {
                    return CompletableFuture.failedFuture(exception);
                }
            }
        }).whenComplete((ignored, failure) -> {
            synchronized (this) {
                cache.pending(false);
                discardOffline(id);
                if (!stopping && cache.online()) {
                    Player player = Bukkit.getPlayer(id);
                    if (player != null) {
                        SchedulerUtil.runSync(player, () -> join(player));
                    }
                }
            }
        });
    }

    public synchronized void configureAutoSave() {
        if (autosave != null) {
            autosave.cancel();
            autosave = null;
        }
        if (!stopping && ConfigManager.configManager.getBoolean("auto-save.enabled", true)) {
            long ticks = Math.max(1L, ConfigManager.configManager.config.getLong("auto-save.period-tick", 6000L));
            autosave = SchedulerUtil.runTaskTimerAsynchronously(() -> {
                synchronized (this) {
                    if (stopping) {
                        return;
                    }
                    if (!ConfigManager.configManager.getBoolean("auto-save.hide-message", false)) {
                        TextUtil.sendMessage(null, CommonUtil.parseLang(null,
                                ConfigManager.configManager.getString("auto-save.message", "")));
                    }
                    flush(false);
                }
            }, 180L, ticks);
        }
    }

    private synchronized void flush(boolean shutdown) {
        for (ObjectCache cache : loadedCaches()) {
            if (shutdown || cache.dirty() && !cache.pending()) {
                save(cache.playerId());
            }
        }
    }

    public synchronized boolean beginScroll(UUID token) {
        return !stopping && scrollInFlight.add(token);
    }

    public synchronized boolean scrollInFlight(UUID token) {
        return scrollInFlight.contains(token);
    }

    public synchronized void endScroll(UUID token) {
        scrollInFlight.remove(token);
    }

    public synchronized boolean canActivateScroll(UUID player, UUID token) {
        ObjectCache cache = playerCacheMap.get(player);
        return !stopping && cache != null && cache.ready()
                && cache.activationRetries().getOrDefault(token, 0L) <= System.currentTimeMillis();
    }

    public synchronized boolean deferScroll(UUID player, UUID token, boolean permanent) {
        ObjectCache cache = playerCacheMap.get(player);
        if (cache == null) {
            return false;
        }
        long retryAt = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 10_000L;
        Long previous = cache.activationRetries().put(token, retryAt);
        return previous == null || permanent && previous != Long.MAX_VALUE;
    }

    public synchronized void carriedScrolls(UUID player, Set<UUID> tokens) {
        ObjectCache cache = playerCacheMap.get(player);
        if (cache != null) {
            cache.carriedScrolls().clear();
            cache.carriedScrolls().addAll(tokens);
            cache.activationRetries().keySet().retainAll(tokens);
        }
    }

    public synchronized void removeCarriedScroll(UUID player, UUID token) {
        ObjectCache cache = playerCacheMap.get(player);
        if (cache != null) {
            cache.carriedScrolls().remove(token);
        }
    }

    public synchronized CompletableFuture<QuestPlayerData> loadScroll(UUID token) {
        return QuestStorage.questStorage.loadScroll(token);
    }

    public synchronized CompletableFuture<Void> saveScroll(QuestPlayerData snapshot) {
        return QuestStorage.questStorage.saveScroll(snapshot);
    }

    public synchronized CompletableFuture<Void> updateScroll(UUID token, String playerName, Consumer<YamlConfiguration> update) {
        return writeScroll(token, () -> loadScroll(token).thenCompose(snapshot -> {
            synchronized (this) {
                try {
                    YamlConfiguration record = new YamlConfiguration();
                    record.loadFromString(snapshot.payload());
                    update.accept(record);
                    return saveScroll(new QuestPlayerData(token, playerName,
                            Math.addExact(snapshot.revision(), 1L), record.saveToString()));
                } catch (Exception exception) {
                    return CompletableFuture.failedFuture(exception);
                }
            }
        }));
    }

    private synchronized CompletableFuture<Void> writeScroll(UUID token, Supplier<CompletableFuture<Void>> write) {
        if (stopping) {
            return CompletableFuture.failedFuture(new IllegalStateException("Player cache is shutting down"));
        }
        CompletableFuture<Void> previous = scrollWrites.getOrDefault(token, CompletableFuture.completedFuture(null));
        CompletableFuture<Void> result = new CompletableFuture<>();
        scrollWrites.put(token, result);
        previous.handle((ignored, failure) -> null).thenCompose(ignored -> write.get()).whenComplete((ignored, failure) -> {
            synchronized (this) {
                scrollWrites.remove(token, result);
                if (failure == null) {
                    result.complete(null);
                } else {
                    result.completeExceptionally(failure);
                }
            }
        });
        return result;
    }

    @Override
    public synchronized void onPluginDisable() {
        stopping = true;
        if (autosave != null) {
            autosave.cancel();
            autosave = null;
        }
    }

    public void finishShutdown() {
        DatabaseExecutor.await();
        synchronized (this) {
            flush(true);
        }
        DatabaseExecutor.await();
        synchronized (this) {
            playerCacheMap.clear();
            scrollInFlight.clear();
            scrollWrites.clear();
        }
    }
}
