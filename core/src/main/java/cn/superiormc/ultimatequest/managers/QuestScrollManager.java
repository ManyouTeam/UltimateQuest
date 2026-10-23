package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.random.RandomGenerator;
import org.bukkit.NamespacedKey;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

public final class QuestScrollManager extends AbstractManager {

    public static QuestScrollManager questScrollManager;

    private final NamespacedKey itemKey = new NamespacedKey(UltimateQuest.instance, "quest_scroll");

    private final NamespacedKey profileKey = new NamespacedKey(UltimateQuest.instance, "quest_scroll_profile");

    private final NamespacedKey rarityKey = new NamespacedKey(UltimateQuest.instance, "quest_scroll_rarity");

    private final NamespacedKey difficultyKey = new NamespacedKey(UltimateQuest.instance, "quest_scroll_difficulty");

    private final Set<UUID> inFlight = ConcurrentHashMap.newKeySet();

    private final Map<UUID, Set<UUID>> carried = new ConcurrentHashMap<>();

    private final Map<UUID, CompletableFuture<Void>> writes = new ConcurrentHashMap<>();

    private final Map<UUID, Map<UUID, Long>> activationRetries = new ConcurrentHashMap<>();

    private Map<String, YamlConfiguration> profiles = Map.of();

    public QuestScrollManager() {
        questScrollManager = this;
    }

    public Map<String, YamlConfiguration> load(Map<String, YamlConfiguration> pools,
            Map<String, YamlConfiguration> objectives) throws Exception {
        return load(pools, objectives, RewardManager.rewardManager.loadPools());
    }

    public Map<String, YamlConfiguration> load(Map<String, YamlConfiguration> pools,
            Map<String, YamlConfiguration> objectives, Map<String, YamlConfiguration> rewardPools) throws Exception {
        Map<String, YamlConfiguration> loaded = QuestPoolManager.readFolder("scrolls");
        for (Map.Entry<String, YamlConfiguration> entry : loaded.entrySet()) {
            YamlConfiguration config = entry.getValue();
            RewardManager.rewardManager.resolveReferences(config, rewardPools);
            ConfigurationSection source = config.getConfigurationSection("source");
            YamlConfiguration pool = source == null || !source.contains("pool")
                    ? null : pools.get(source.getString("pool", ""));
            long count = pool == null ? 0L : QuestDefinition.positiveLong(source, "count", 1L);
            if ((source != null && source.contains("pool") && (pool == null || count > 45L))
                    || !config.isConfigurationSection("item")) {
                throw new IllegalArgumentException("Invalid scroll configuration: " + entry.getKey());
            }
            duration(config);
            if (pool != null && pool.getConfigurationSection("quests").getKeys(false).size() < count) {
                throw new IllegalArgumentException("Scroll " + entry.getKey() + " has insufficient objectives");
            }
            ConfigurationSection tasks = config.getConfigurationSection("tasks");
            if (source == null && (tasks == null || tasks.getKeys(false).isEmpty())) {
                throw new IllegalArgumentException("Scroll " + entry.getKey() + " requires tasks or source.pool");
            }
            if (source != null && !source.contains("pool")) {
                YamlConfiguration resolved = QuestObjective.resolve(
                        "main", source, objectives);
                new QuestObjective("main", resolved);
            }
            if (tasks != null) {
                for (String id : tasks.getKeys(false)) {
                    YamlConfiguration resolved = QuestObjective.resolve(
                            id, tasks.getConfigurationSection(id), objectives);
                    new QuestObjective(id, resolved);
                }
            }
            RewardManager.rewardManager.validate(config.getConfigurationSection("rewards"));
        }
        return loaded;
    }

    public synchronized void install(Map<String, YamlConfiguration> loaded) {
        profiles = Map.copyOf(loaded);
        activationRetries.clear();
    }

    public synchronized Map<String, YamlConfiguration> definitions() {
        return profiles;
    }

    private static long duration(YamlConfiguration config) {
        if (!config.contains("duration-seconds")) {
            return 0L;
        }
        Object value = config.get("duration-seconds");
        if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < 0L) {
            throw new IllegalArgumentException("Scroll duration must be a nonnegative integer");
        }
        long seconds = ((Number) value).longValue();
        Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
        return seconds;
    }

    public YamlConfiguration createRecord(String profile, String rarity, String difficulty, RandomGenerator random) {
        YamlConfiguration config = definitions().get(profile);
        if (config == null) {
            throw new IllegalArgumentException("Unknown scroll profile: " + profile);
        }
        ConfigurationSection rarities = config.getConfigurationSection("rarities");
        ConfigurationSection difficulties = config.getConfigurationSection("difficulties");
        rarity = rarities == null ? config.getString("rarity", "common")
                : rarity == null ? QuestPoolManager.choose(rarities, random) : rarity;
        difficulty = difficulties == null ? config.getString("difficulty", "normal")
                : difficulty == null ? QuestPoolManager.choose(difficulties, random) : difficulty;
        if (rarities != null && !rarities.contains(rarity)
                || difficulties != null && !difficulties.contains(difficulty)) {
            throw new IllegalArgumentException("Unknown scroll rarity or difficulty");
        }
        Set<String> objectiveIds = new LinkedHashSet<>();
        ConfigurationSection fixed = config.getConfigurationSection("tasks");
        if (fixed != null) {
            objectiveIds.addAll(fixed.getKeys(false));
        }
        ConfigurationSection source = config.getConfigurationSection("source");
        if (source != null) {
            if (source.contains("pool")) {
                objectiveIds.addAll(QuestPoolManager.questPoolManager.draw(source.getString("pool"),
                        source.getInt("count", 1), random));
            } else {
                objectiveIds.add("main");
            }
        }
        definition(profile, List.copyOf(objectiveIds));
        YamlConfiguration record = new YamlConfiguration();
        record.set("schema-version", 3);
        record.set("kind", "scroll");
        record.set("state", "ISSUED");
        record.set("profile", profile);
        record.set("rarity", rarity);
        record.set("difficulty", difficulty);
        record.set("duration-seconds", duration(config));
        record.set("objective-ids", List.copyOf(objectiveIds));
        return record;
    }

    public QuestDefinition definition(String profile, List<String> objectiveIds) {
        YamlConfiguration config = definitions().get(profile);
        if (config == null) {
            throw new IllegalArgumentException("Unknown scroll profile: " + profile);
        }
        if (objectiveIds.isEmpty() || objectiveIds.size() > 45
                || new HashSet<>(objectiveIds).size() != objectiveIds.size()) {
            throw new IllegalArgumentException("Invalid scroll objective IDs: " + profile);
        }
        YamlConfiguration task = new YamlConfiguration();
        task.set("type", "normal");
        task.set("listed", false);
        task.set("name", config.getString("name", profile));
        task.set("description", config.getStringList("description"));
        task.set("categories", config.getStringList("categories"));
        copy(config.getConfigurationSection("icon"), task.createSection("icon"));
        copy(config.getConfigurationSection("rewards"), task.createSection("rewards"));
        ConfigurationSection fixed = config.getConfigurationSection("tasks");
        if (fixed != null) {
            for (String id : fixed.getKeys(false)) {
                copy(fixed.getConfigurationSection(id), task.createSection("objectives." + id));
            }
        }
        ConfigurationSection source = config.getConfigurationSection("source");
        if (source != null) {
            if (source.contains("pool")) {
                for (String objective : objectiveIds) {
                    if (fixed == null || !fixed.contains(objective)) {
                        task.set("objectives." + objective + ".source.quest", objective);
                    }
                }
            } else {
                copy(source, task.createSection("objectives.main.source"));
            }
        }
        QuestDefinition definition = new QuestDefinition(profile, task, QuestManager.questManager.objectiveTemplates());
        Set<String> resolvedIds = new HashSet<>();
        for (QuestObjective objective : definition.objectives()) {
            resolvedIds.add(objective.getId());
        }
        if (!resolvedIds.equals(new HashSet<>(objectiveIds))) {
            throw new IllegalArgumentException("Scroll objective IDs do not match profile: " + profile);
        }
        return definition;
    }

    public static List<QuestDefinition> readQuests(YamlConfiguration record) throws Exception {
        if (record.getInt("schema-version") != 3) {
            throw new IllegalArgumentException("Unsupported scroll record schema-version: " + record.get("schema-version"));
        }
        if (!record.getString("kind", "").equals("scroll")) {
            throw new IllegalArgumentException("Invalid scroll record kind: " + record.get("kind"));
        }
        if (!Set.of("ISSUED", "ACTIVE", "REDEEMING", "REDEEMED", "REVIEW").contains(record.getString("state", ""))) {
            throw new IllegalArgumentException("Invalid scroll record state: " + record.get("state"));
        }
        if (!record.isList("objective-ids")) {
            throw new IllegalArgumentException("Missing scroll objective IDs");
        }
        duration(record);
        QuestDefinition definition = questScrollManager.definition(record.getString("profile", ""),
                record.getStringList("objective-ids"));
        return List.of(definition);
    }

    public UUID token(ItemStack item) {
        if (item == null || !item.hasItemMeta()) {
            return null;
        }
        String value = item.getItemMeta().getPersistentDataContainer().get(itemKey, PersistentDataType.STRING);
        if (value == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public void give(Player player, String profile, String rarity, String difficulty) {
        YamlConfiguration record = createRecord(profile, rarity, difficulty, RandomGenerator.getDefault());
        YamlConfiguration config = definitions().get(profile);
        ItemStack item = BuildItem.buildItemStack(player, config.getConfigurationSection("item"));
        if (item == null || item.getType().isAir()) {
            throw new IllegalArgumentException("Invalid scroll item");
        }
        item.setAmount(1);
        UUID token = UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        UltimateQuest.methodUtil.setItemName(meta, CommonUtil.modifyString(player, config.getString("name", profile),
                "rarity", record.getString("rarity"), "difficulty", record.getString("difficulty")), player);
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, token.toString());
        rememberRecord(meta, record);
        item.setItemMeta(meta);
        QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, "", 1L, record.saveToString())).whenComplete((ignored, failure) -> {
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: " + failure.getMessage());
                SchedulerUtil.runSync(player,
                        () -> LanguageManager.languageManager.sendStringText(player, "quests.save-failed"));
                return;
            }
            SchedulerUtil.runSync(player, () -> {
                if (player.isOnline()) {
                    CommonUtil.giveOrDrop(player, item);
                    LanguageManager.languageManager.sendStringText(player, "scroll.given");
                }
            });
        });
    }

    public void activate(Player player, UUID token) {
        if (!canActivate(player.getUniqueId(), token)) {
            return;
        }
        if (!inFlight.add(token)) {
            return;
        }
        if (!QuestManager.questManager.beginScroll(player)) {
            inFlight.remove(token);
            return;
        }
        try {
            QuestStorage.questStorage.loadScroll(token).whenComplete((snapshot, failure) -> {
                try {
                    SchedulerUtil.runSync(player, () -> reserve(player, token, snapshot, failure),
                            () -> release(player.getUniqueId(), token));
                } catch (RuntimeException exception) {
                    if (deferActivation(player.getUniqueId(), token, false)) {
                        ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be scheduled: "
                                + exception.getMessage());
                    }
                    release(player.getUniqueId(), token);
                }
            });
        } catch (RuntimeException exception) {
            if (deferActivation(player.getUniqueId(), token, false)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be read: "
                        + exception.getMessage());
                LanguageManager.languageManager.sendStringText(player, "scroll.read-failed");
            }
            release(player.getUniqueId(), token);
        }
    }

    private void reserve(Player player, UUID token, QuestPlayerData snapshot, Throwable failure) {
        try {
            if (failure != null) {
                if (deferActivation(player.getUniqueId(), token, false)) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be read: " + failure.getMessage());
                    LanguageManager.languageManager.sendStringText(player, "scroll.read-failed");
                }
                release(player.getUniqueId(), token);
                return;
            }
            if (!player.isOnline() || !hasToken(player, token)) {
                release(player.getUniqueId(), token);
                return;
            }
            YamlConfiguration record = new YamlConfiguration();
            if (snapshot != null) {
                record.loadFromString(snapshot.payload());
            }
            if (missingRecord(snapshot, record)) {
                record = recoverRecord(player, token);
            }
            List<QuestDefinition> drawn = readQuests(record);
            QuestInstance saved = null;
            if (record.isConfigurationSection("instance")) {
                saved = new QuestInstance(record.getString("instance-key", ""),
                        record.getConfigurationSection("instance"));
            }
            if (!Set.of("ISSUED", "ACTIVE").contains(record.getString("state"))) {
                if (deferActivation(player.getUniqueId(), token, true)) {
                    LanguageManager.languageManager.sendStringText(player, "scroll.used");
                }
                release(player.getUniqueId(), token);
                return;
            }
            if (!QuestManager.questManager.canInstallScroll(player, drawn)) {
                if (deferActivation(player.getUniqueId(), token, false)) {
                    LanguageManager.languageManager.sendStringText(player, "scroll.requirements");
                }
                release(player.getUniqueId(), token);
                return;
            }
            record.set("state", "ACTIVE");
            record.set("owner", player.getUniqueId().toString());
            QuestInstance restored = saved;
            YamlConfiguration reserved = record;
            long revision = snapshot == null ? 1L : Math.addExact(snapshot.revision(), 1L);
            QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, player.getName(), revision,
                    reserved.saveToString())).whenComplete((ignored, saveFailure) -> {
                if (saveFailure != null) {
                    if (deferActivation(player.getUniqueId(), token, true)) {
                        ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                                + saveFailure.getMessage());
                        SchedulerUtil.runSync(player,
                                () -> LanguageManager.languageManager.sendStringText(player, "quests.review-needed"));
                    }
                    release(player.getUniqueId(), token);
                    return;
                }
                SchedulerUtil.runSync(player, () -> install(player, token, revision,
                                reserved, drawn, restored),
                        () -> release(player.getUniqueId(), token));
            });
        } catch (Exception exception) {
            if (deferActivation(player.getUniqueId(), token, true)) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                        + exception.getMessage());
                LanguageManager.languageManager.sendStringText(player, "scroll.invalid");
            }
            release(player.getUniqueId(), token);
        }
    }

    private void install(Player player, UUID token, long revision, YamlConfiguration record,
            List<QuestDefinition> drawn, QuestInstance restored) {
        if (!player.isOnline() || !hasToken(player, token)) {
            deferActivation(player.getUniqueId(), token, true);
            ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token
                    + " failed: Reserved scroll was moved or its owner disconnected; manual review required");
            if (player.isOnline()) {
                LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
            }
            release(player.getUniqueId(), token);
            return;
        }
        rememberRecord(player, token, record);
        long seconds = duration(record);
        long expiresAt = seconds == 0L ? 0L : Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
        QuestManager.questManager.installScroll(player, token, drawn, restored, expiresAt).whenComplete((success, failure) -> {
            record.set("state", failure == null && Boolean.TRUE.equals(success) ? "ACTIVE" : "REVIEW");
            QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, player.getName(), revision + 1L,
                    record.saveToString())).whenComplete((ignored, saveFailure) -> {
                if (saveFailure != null) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + saveFailure.getMessage());
                }
                SchedulerUtil.runSync(player, () -> {
                    if (Boolean.TRUE.equals(success) && failure == null) {
                        QuestInstance quest = QuestManager.questManager.scroll(player.getUniqueId(), token);
                        if (quest != null) {
                            saveProgress(player, quest);
                        }
                        LanguageManager.languageManager.sendStringText(player,
                                saveFailure == null ? "scroll.activated" : "quests.review-needed");
                    } else {
                        if (deferActivation(player.getUniqueId(), token, true)) {
                            if (failure != null) {
                                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be installed: "
                                        + failure.getMessage());
                            }
                            LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
                        }
                    }
                    inFlight.remove(token);
                }, () -> inFlight.remove(token));
            });
        });
    }

    public boolean hasToken(Player player, UUID token) {
        return findToken(player, token) != null;
    }

    private ItemStack findToken(Player player, UUID token) {
        for (ItemStack item : player.getInventory().getStorageContents()) {
            if (token.equals(token(item))) {
                return item;
            }
        }
        ItemStack offHand = player.getInventory().getItemInOffHand();
        return token.equals(token(offHand)) ? offHand : null;
    }

    public void synchronize(Player player) {
        Set<UUID> current = new HashSet<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            UUID token = token(item);
            if (token != null) {
                current.add(token);
            }
        }
        UUID token = token(player.getInventory().getItemInOffHand());
        if (token != null) {
            current.add(token);
        }
        for (UUID removed : QuestManager.questManager.scrollTokens(player.getUniqueId())) {
            if (!current.contains(removed)) {
                QuestManager.questManager.detachScroll(player.getUniqueId(), removed);
            }
        }
        carried.put(player.getUniqueId(), current);
        Map<UUID, Long> retries = activationRetries.get(player.getUniqueId());
        if (retries != null) {
            retries.keySet().retainAll(current);
        }
        for (UUID added : current) {
            if (!QuestManager.questManager.hasScroll(player.getUniqueId(), added)
                    && !inFlight.contains(added) && canActivate(player.getUniqueId(), added)) {
                activate(player, added);
                return;
            }
        }
    }

    public void saveProgress(Player player, QuestInstance quest) {
        if (!quest.source().equals("scroll")) {
            return;
        }
        UUID token = UUID.fromString(quest.sourceId());
        YamlConfiguration instance = new YamlConfiguration();
        quest.write(instance);
        String serializedInstance = instance.saveToString();
        String instanceKey = quest.key();
        String playerName = player.getName();
        writes.compute(token, (ignored, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null) : previous.handle((value, failure) -> null);
            CompletableFuture<Void> next = base.thenCompose(value -> QuestStorage.questStorage.loadScroll(token))
                    .thenCompose(snapshot -> {
                        try {
                            YamlConfiguration record = new YamlConfiguration();
                            record.loadFromString(snapshot.payload());
                            record.set("instance", null);
                            record.set("instance-key", instanceKey);
                            YamlConfiguration savedInstance = new YamlConfiguration();
                            savedInstance.loadFromString(serializedInstance);
                            copy(savedInstance, record.createSection("instance"));
                            return QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, playerName,
                                    snapshot.revision() + 1L, record.saveToString()));
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(exception);
                        }
                    });
            next.whenComplete((value, failure) -> {
                writes.remove(token, next);
                if (failure != null) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + failure.getMessage());
                }
            });
            return next;
        });
    }

    public void reserveReward(Player player, QuestInstance quest, Runnable success) {
        UUID token = UUID.fromString(quest.sourceId());
        if (!inFlight.add(token)) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return;
        }
        writes.compute(token, (ignored, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null) : previous.handle((value, failure) -> null);
            CompletableFuture<Void> next = base.thenCompose(value -> QuestStorage.questStorage.loadScroll(token))
                    .thenCompose(snapshot -> {
                        try {
                            YamlConfiguration record = new YamlConfiguration();
                            record.loadFromString(snapshot.payload());
                            if (!record.getString("state", "").equals("ACTIVE") || !hasToken(player, token)) {
                                throw new IllegalStateException("Scroll is not active or carried");
                            }
                            record.set("state", "REDEEMING");
                            return QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, player.getName(),
                                    snapshot.revision() + 1L, record.saveToString()));
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(exception);
                        }
                    });
            next.whenComplete((value, failure) -> {
                writes.remove(token, next);
                if (failure != null) {
                    inFlight.remove(token);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + failure.getMessage());
                    SchedulerUtil.runSync(player,
                            () -> LanguageManager.languageManager.sendStringText(player, "scroll.used"));
                    return;
                }
                SchedulerUtil.runSync(player, success,
                        () -> inFlight.remove(token));
            });
            return next;
        });
    }

    public void complete(Player player, QuestInstance quest) {
        UUID token = UUID.fromString(quest.sourceId());
        ItemStack[] contents = player.getInventory().getStorageContents();
        for (int slot = 0; slot < contents.length; slot++) {
            if (token.equals(token(contents[slot]))) {
                player.getInventory().setItem(slot, null);
                break;
            }
        }
        if (token.equals(token(player.getInventory().getItemInOffHand()))) {
            player.getInventory().setItemInOffHand(null);
        }
        carried.computeIfPresent(player.getUniqueId(), (id, tokens) -> {
            tokens.remove(token);
            return tokens;
        });
        writes.compute(token, (ignored, previous) -> {
            CompletableFuture<Void> base = previous == null
                    ? CompletableFuture.completedFuture(null) : previous.handle((value, failure) -> null);
            CompletableFuture<Void> next = base.thenCompose(value -> QuestStorage.questStorage.loadScroll(token))
                    .thenCompose(snapshot -> {
                        try {
                            YamlConfiguration record = new YamlConfiguration();
                            record.loadFromString(snapshot.payload());
                            record.set("state", "REDEEMED");
                            return QuestStorage.questStorage.saveScroll(new QuestPlayerData(token, player.getName(),
                                    snapshot.revision() + 1L, record.saveToString()));
                        } catch (Exception exception) {
                            return CompletableFuture.failedFuture(exception);
                        }
                    });
            next.whenComplete((value, failure) -> writes.remove(token, next));
            return next;
        });
        inFlight.remove(token);
    }

    public void leave(UUID playerId) {
        carried.remove(playerId);
        activationRetries.remove(playerId);
    }

    private boolean canActivate(UUID playerId, UUID token) {
        return activationRetries.getOrDefault(playerId, Map.of()).getOrDefault(token, 0L) <= System.currentTimeMillis();
    }

    private boolean deferActivation(UUID playerId, UUID token, boolean permanent) {
        long retryAt = permanent ? Long.MAX_VALUE : System.currentTimeMillis() + 10_000L;
        Long previous = activationRetries.computeIfAbsent(playerId, ignored -> new ConcurrentHashMap<>()).put(token, retryAt);
        return previous == null || permanent && previous != Long.MAX_VALUE;
    }

    private boolean missingRecord(QuestPlayerData snapshot, YamlConfiguration record) {
        return snapshot == null || snapshot.revision() == 0L
                && (record.getKeys(false).isEmpty() || record.getKeys(false).equals(Set.of("schema-version"))
                && record.getInt("schema-version") == 4);
    }

    private void rememberRecord(ItemMeta meta, YamlConfiguration record) {
        meta.getPersistentDataContainer().set(profileKey, PersistentDataType.STRING, record.getString("profile", ""));
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, record.getString("rarity", "common"));
        meta.getPersistentDataContainer().set(difficultyKey, PersistentDataType.STRING, record.getString("difficulty", "normal"));
    }

    private void rememberRecord(Player player, UUID token, YamlConfiguration record) {
        ItemStack[] items = player.getInventory().getStorageContents();
        for (int slot = 0; slot < items.length; slot++) {
            ItemStack item = items[slot];
            if (token.equals(token(item))) {
                ItemMeta meta = item.getItemMeta();
                rememberRecord(meta, record);
                item.setItemMeta(meta);
                player.getInventory().setItem(slot, item);
                return;
            }
        }
        ItemStack item = player.getInventory().getItemInOffHand();
        if (token.equals(token(item))) {
            ItemMeta meta = item.getItemMeta();
            rememberRecord(meta, record);
            item.setItemMeta(meta);
            player.getInventory().setItemInOffHand(item);
        }
    }

    private YamlConfiguration recoverRecord(Player player, UUID token) {
        ItemStack item = findToken(player, token);
        if (item == null) {
            throw new IllegalArgumentException("Missing scroll item while rebuilding its record");
        }
        ItemMeta meta = item.getItemMeta();
        return recoverRecord(player, meta, item.getType());
    }

    private YamlConfiguration recoverRecord(Player player, ItemMeta meta, Material material) {
        String profile = meta.getPersistentDataContainer().get(profileKey, PersistentDataType.STRING);
        if (profile != null) {
            return createRecord(profile, meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING),
                    meta.getPersistentDataContainer().get(difficultyKey, PersistentDataType.STRING), RandomGenerator.getDefault());
        }
        String[] match = matchLegacyProfile(player, material, UltimateQuest.methodUtil.getItemName(meta));
        return createRecord(match[0], match[1], match[2], RandomGenerator.getDefault());
    }

    private String[] matchLegacyProfile(Player player, Material material, String displayName) {
        String[] match = null;
        String actual = plainName(displayName);
        for (Map.Entry<String, YamlConfiguration> entry : definitions().entrySet()) {
            YamlConfiguration config = entry.getValue();
            if (Material.matchMaterial(config.getString("item.material", "")) != material) {
                continue;
            }
            String name = config.getString("name", entry.getKey());
            List<String> names = new ArrayList<>();
            names.add(name);
            if (name.startsWith("{lang:") && name.endsWith("}") && name.indexOf('}') == name.length() - 1) {
                names.addAll(LanguageManager.languageManager.overrideVariants(name.substring(6, name.length() - 1)));
            }
            List<String> rarities = config.isConfigurationSection("rarities")
                    ? List.copyOf(config.getConfigurationSection("rarities").getKeys(false)) : List.of(config.getString("rarity", "common"));
            List<String> difficulties = config.isConfigurationSection("difficulties")
                    ? List.copyOf(config.getConfigurationSection("difficulties").getKeys(false)) : List.of(config.getString("difficulty", "normal"));
            for (String rarity : rarities) {
                for (String difficulty : difficulties) {
                    for (String candidate : names) {
                        if (actual.isBlank() || !actual.equals(plainName(CommonUtil.modifyString(player, candidate,
                                "rarity", rarity, "difficulty", difficulty)))) {
                            continue;
                        }
                        if (match != null && !match[0].equals(entry.getKey())) {
                            throw new IllegalArgumentException("Missing scroll record and its item matches multiple profiles");
                        }
                        if (match == null) {
                            match = new String[] {entry.getKey(), rarity, difficulty};
                        } else {
                            if (!rarity.equals(match[1])) {
                                match[1] = null;
                            }
                            if (!difficulty.equals(match[2])) {
                                match[2] = null;
                            }
                        }
                    }
                }
            }
        }
        if (match == null) {
            throw new IllegalArgumentException("Missing scroll record and its source profile could not be identified");
        }
        return match;
    }

    private String plainName(String name) {
        return name == null ? "" : ChatColor.stripColor(TextUtil.colorize(name.replaceAll("<[^>]+>", "")));
    }

    private static void copy(ConfigurationSection source, ConfigurationSection target) {
        if (source == null) {
            return;
        }
        for (Map.Entry<String, Object> entry : source.getValues(true).entrySet()) {
            if (!(entry.getValue() instanceof ConfigurationSection)) {
                target.set(entry.getKey(), entry.getValue());
            }
        }
    }

    private void release(UUID player, UUID token) {
        QuestManager.questManager.endScroll(player);
        inFlight.remove(token);
    }

}
