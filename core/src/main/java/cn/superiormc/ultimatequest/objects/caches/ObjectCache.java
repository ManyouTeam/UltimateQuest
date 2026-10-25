package cn.superiormc.ultimatequest.objects.caches;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
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

    private UUID loadingToken;

    private boolean ready;

    private UUID operationToken;

    private Player session;

    private CompletableFuture<Void> saveTail = CompletableFuture.completedFuture(null);

    private boolean dirty;

    private String bulkClaim;

    public ObjectCache(UUID playerId) {
        this.playerId = playerId;
    }

    public ObjectCache copy() {
        ObjectCache copy = new ObjectCache(playerId);
        copy.playerName = playerName;
        quests.forEach((key, quest) -> copy.quests.put(key, new QuestInstance(quest)));
        index.forEach((key, value) -> copy.index.put(key, List.copyOf(value)));
        copy.periodRolls.putAll(periodRolls);
        copy.carriedScrolls.addAll(carriedScrolls);
        copy.revision = revision;
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
        }
    }

    public CompletableFuture<Void> saveTail() {
        return saveTail;
    }

    public void saveTail(CompletableFuture<Void> future) {
        saveTail = future;
    }

    public void load(QuestPlayerData snapshot) throws Exception {
        if (!playerId.equals(snapshot.playerUUID())) {
            throw new IllegalArgumentException("Player snapshot belongs to another player");
        }
        YamlConfiguration config = new YamlConfiguration();
        config.loadFromString(snapshot.payload());
        Map<String, QuestInstance> loadedQuests = new LinkedHashMap<>();
        ConfigurationSection section = config.getConfigurationSection("quests");
        if (section != null) {
            for (String key : section.getKeys(false)) {
                ConfigurationSection saved = section.getConfigurationSection(key);
                QuestDefinition definition = ConfigManager.configManager.resolveDefinition(key, saved);
                if (definition != null) {
                    loadedQuests.put(key, new QuestInstance(key, saved, definition));
                }
            }
        }
        Map<String, String> loadedRolls = new HashMap<>();
        ConfigurationSection savedRolls = config.getConfigurationSection("period-rolls");
        if (savedRolls != null) {
            for (String group : savedRolls.getKeys(false)) {
                String cycle = savedRolls.getString(group, "");
                if (!group.matches("[a-z0-9_-]+") || !cycle.matches("(?:daily|weekly|monthly|scheduled)_-?[0-9]+")) {
                    throw new IllegalArgumentException("Invalid saved period cycle");
                }
                Long.parseLong(cycle.substring(cycle.indexOf('_') + 1));
                loadedRolls.put(group, cycle);
            }
        }
        quests.clear();
        quests.putAll(loadedQuests);
        periodRolls.clear();
        periodRolls.putAll(loadedRolls);
        revision = snapshot.revision();
        ready = true;
        dirty = false;
    }

    public QuestPlayerData snapshot() {
        if (!ready) {
            throw new IllegalStateException("Player cache is not loaded");
        }
        YamlConfiguration config = new YamlConfiguration();
        config.set("schema-version", 4);
        periodRolls.forEach((group, cycle) -> config.set("period-rolls." + group, cycle));
        for (QuestInstance quest : quests.values()) {
            quest.write(config.createSection("quests." + quest.key()));
        }
        return new QuestPlayerData(playerId, playerName, revision, config.saveToString());
    }

    public void changed() {
        revision = Math.addExact(revision, 1L);
        dirty = true;
    }

    public void saved(long savedRevision) {
        if (revision == savedRevision) {
            dirty = false;
        }
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
