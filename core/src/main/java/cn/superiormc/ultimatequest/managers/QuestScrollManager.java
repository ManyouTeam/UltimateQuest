package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.methods.BuildItem;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
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
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import cn.superiormc.ultimatequest.objects.quests.ObjectScroll;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;

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

    public QuestScrollManager() {
        questScrollManager = this;
    }

    public synchronized Map<String, YamlConfiguration> definitions() {
        return ConfigManager.configManager.getScrollConfigurations();
    }

    public YamlConfiguration createRecord(String profile, String rarity, String difficulty, RandomGenerator random) {
        return ConfigManager.configManager.getScroll(profile).createRecord(rarity, difficulty, random);
    }

    public QuestDefinition definition(String profile, List<String> objectiveIds) {
        return ConfigManager.configManager.getScroll(profile).createDefinition(objectiveIds);
    }

    private static long duration(YamlConfiguration config) {
        return ObjectScroll.readDuration(config);
    }

    public static List<QuestDefinition> readQuests(YamlConfiguration record) throws Exception {
        return new ObjectScrollRecord(record).readQuests();
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
        ItemStack item = ConfigManager.configManager.getScroll(profile).buildItem(player, record.getString("rarity"), record.getString("difficulty"));
        UUID token = UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
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
            ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: Reserved scroll was moved or its owner disconnected; manual"
                    + " review required");
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
                                ErrorManager.errorManager.sendErrorMessage("§cError:"
                                        + " Scroll"
                                        + " " + token + " could"
                                        + " not be"
                                        + " installed:"
                                        + " " + failure.getMessage());
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
                    cn.superiormc.ultimatequest.utils.ConfigurationFileUtil.copy(savedInstance, record.createSection("instance"));
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
        return recoverRecord(meta);
    }

    private YamlConfiguration recoverRecord(ItemMeta meta) {
        String profile = meta.getPersistentDataContainer().get(profileKey, PersistentDataType.STRING);
        if (profile == null) {
            throw new IllegalArgumentException("Scroll item is missing its profile metadata");
        }
        return createRecord(profile, meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING),
                meta.getPersistentDataContainer().get(difficultyKey, PersistentDataType.STRING), RandomGenerator.getDefault());
    }

    private void release(UUID player, UUID token) {
        QuestManager.questManager.endScroll(player);
        inFlight.remove(token);
    }

}
