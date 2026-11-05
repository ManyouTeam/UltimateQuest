package cn.superiormc.ultimatequest.objects.caches;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestInstanceData;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.entity.Player;

public final class ObjectCache {

    private final UUID playerId;

    private String playerName = "";

    private final Map<String, QuestInstance> quests = new LinkedHashMap<>();

    private final Map<String, List<String>> index = new HashMap<>();

    private final Map<String, String> periodRolls = new HashMap<>();

    private final Set<UUID> carriedScrolls = new HashSet<>();

    private final Map<UUID, Long> activationRetries = new HashMap<>();

    private long revision;

    private QuestPlayerData persistedData;

    private final Map<String, QuestInstanceData> unresolved = new LinkedHashMap<>();

    private UUID loadingToken;

    private boolean ready;

    private UUID operationToken;

    private Player session;

    private CompletableFuture<Void> saveTail = CompletableFuture.completedFuture(null);

    private boolean dirty;

    private String bulkClaim;

    private boolean rebindPending;

    public ObjectCache(UUID playerId) {
        this.playerId = playerId;
        persistedData = QuestPlayerData.empty(playerId);
    }

    public ObjectCache copy() {
        ObjectCache copy = new ObjectCache(playerId);
        copy.playerName = playerName;
        quests.forEach((key, quest) -> copy.quests.put(key, new QuestInstance(quest)));
        index.forEach((key, value) -> copy.index.put(key, List.copyOf(value)));
        copy.periodRolls.putAll(periodRolls);
        copy.carriedScrolls.addAll(carriedScrolls);
        copy.revision = revision;
        copy.persistedData = persistedData;
        copy.unresolved.putAll(unresolved);
        copy.ready = ready;
        return copy;
    }

    public Player session() {
        return session;
    }

    public void session(Player player) {
        session = player;
    }

    public UUID operationToken() {
        return operationToken;
    }

    public boolean operation(UUID token) {
        return token != null && token.equals(operationToken);
    }

    public void finishOperation(UUID token) {
        if (operation(token)) {
            operationToken = null;
            if (rebindPending) {
                rebindDefinitions();
            }
        }
    }

    public void rebindDefinitions() {
        if (pending()) {
            rebindPending = true;
            return;
        }
        rebindPending = false;
        Map<String, QuestInstanceData> saved = new LinkedHashMap<>(unresolved);
        quests.forEach((key, quest) -> saved.put(key, quest.data()));
        quests.clear();
        unresolved.clear();
        saved.forEach((key, entry) -> {
            try {
                QuestDefinition definition = ConfigManager.configManager.resolveDefinition(entry);
                if (definition == null || entry.progress().keySet().stream().anyMatch(objective ->
                        definition.objectives().stream().noneMatch(candidate -> candidate.getId().equals(objective)))) {
                    throw new IllegalArgumentException("Missing quest template or objective: " + entry.templateId());
                }
                quests.put(key, new QuestInstance(entry, definition));
            } catch (RuntimeException exception) {
                unresolved.put(key, entry);
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not bind quest " + key + " for " + playerId
                        + "; its progress has been retained: " + exception.getMessage());
            }
        });
        if (QuestManager.questManager != null) {
            QuestManager.questManager.rebuildIndex(playerId);
        }
    }

    public CompletableFuture<Void> saveTail() {
        return saveTail;
    }

    public void saveTail(CompletableFuture<Void> future) {
        saveTail = future;
    }

    public void load(QuestPlayerData snapshot) {
        if (!playerId.equals(snapshot.playerUUID())) {
            throw new IllegalArgumentException("Player data belongs to another player");
        }
        Map<String, QuestInstance> loaded = new LinkedHashMap<>();
        unresolved.clear();
        snapshot.quests().forEach((key, data) -> {
            try {
                QuestDefinition definition = ConfigManager.configManager.resolveDefinition(data);
                if (definition == null || data.progress().keySet().stream().anyMatch(objective ->
                        definition.objectives().stream().noneMatch(candidate -> candidate.getId().equals(objective)))) {
                    throw new IllegalArgumentException("Missing quest template or objective: " + data.templateId());
                }
                QuestInstance quest = new QuestInstance(data, definition);
                if (quest.state().equals("CLAIMING")) {
                    quest.state("REVIEW");
                }
                quest.stageStates().forEach((stage, state) -> {
                    if (state.equals("CLAIMING")) {
                        quest.stageState(stage, "REVIEW");
                    }
                });
                loaded.put(key, quest);
            } catch (RuntimeException exception) {
                unresolved.put(key, data);
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not load quest " + key + " for " + playerId
                        + "; its progress has been retained: " + exception.getMessage());
            }
        });
        quests.clear();
        quests.putAll(loaded);
        periodRolls.clear();
        periodRolls.putAll(snapshot.periodRolls());
        persistedData = snapshot;
        revision = 0L;
        ready = true;
        dirty = !snapshot().quests().equals(snapshot.quests());
    }

    public boolean unresolved(String key) {
        return unresolved.containsKey(key);
    }

    public QuestPlayerData snapshot() {
        if (!ready) {
            throw new IllegalStateException("Player cache is not loaded");
        }
        Map<String, QuestInstanceData> data = new LinkedHashMap<>(unresolved);
        quests.forEach((key, quest) -> {
            if (unresolved.containsKey(key)) {
                throw new IllegalStateException("Cannot overwrite unresolved quest: " + key);
            }
            data.put(key, quest.data());
        });
        return new QuestPlayerData(playerId, playerName, persistedData.revision(), data, periodRolls);
    }

    public QuestPlayerData persistedData() {
        return persistedData;
    }

    public long changeSequence() {
        return revision;
    }

    public void saved(QuestPlayerData snapshot, long sequence) {
        persistedData = snapshot.withRevision(Math.addExact(snapshot.revision(), 1L));
        if (revision == sequence) {
            dirty = false;
        }
    }

    public void changed() {
        revision = Math.addExact(revision, 1L);
        dirty = true;
    }

    public UUID playerId() {
        return playerId;
    }

    public void playerName(String playerName) {
        this.playerName = playerName;
    }

    public Map<String, QuestInstance> quests() {
        return quests;
    }

    public Map<String, List<String>> index() {
        return index;
    }

    public Map<String, String> periodRolls() {
        return periodRolls;
    }

    public Set<UUID> carriedScrolls() {
        return carriedScrolls;
    }

    public Map<UUID, Long> activationRetries() {
        return activationRetries;
    }

    public UUID loadingToken() {
        return loadingToken;
    }

    public void loadingToken(UUID loadingToken) {
        this.loadingToken = loadingToken;
    }

    public boolean ready() {
        return ready;
    }

    public boolean online() {
        return session != null;
    }

    public boolean pending() {
        return operationToken != null;
    }

    public void pending(boolean pending) {
        if (pending) {
            if (operationToken == null) {
                operationToken = UUID.randomUUID();
            }
        } else {
            operationToken = null;
        }
    }

    public boolean dirty() {
        return dirty;
    }

    public void dirty(boolean dirty) {
        this.dirty = dirty;
    }

    public String bulkClaim() {
        return bulkClaim;
    }

    public void bulkClaim(String bulkClaim) {
        this.bulkClaim = bulkClaim;
    }
}
