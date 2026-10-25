package cn.superiormc.ultimatequest.managers;

import static cn.superiormc.ultimatequest.managers.CacheManager.cacheManager;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.ObjectScroll;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.bukkit.NamespacedKey;
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

    public QuestScrollManager() {
        questScrollManager = this;
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
        ObjectScroll scroll = ConfigManager.configManager.getScroll(profile);
        YamlConfiguration record = scroll.createRecord(rarity, difficulty, RandomGenerator.getDefault());
        ItemStack item = scroll.buildItem(player, record.getString("rarity"), record.getString("difficulty"));
        UUID token = UUID.randomUUID();
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, token.toString());
        rememberRecord(meta, record);
        item.setItemMeta(meta);
        cacheManager.saveScroll(new QuestPlayerData(token, "", 1L, record.saveToString())).whenComplete((ignored, failure) -> {
            if (cacheManager.isStopping()) {
                return;
            }
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: " + failure.getMessage());
                SchedulerUtil.runSync(player,
                        () -> LanguageManager.languageManager.sendStringText(player, "quests.save-failed"));
                return;
            }
            SchedulerUtil.runSync(player, () -> {
                if (player.isOnline()) {
                    CommonUtil.giveOrDrop(player, item);
                    String name = CommonUtil.parseLang(player, scroll.getConfig().getString("name", profile));
                    QuestManager.questManager.sendConfiguredMessage(player, scroll.getConfig(), "scroll.given",
                            "quest", name, "task", name, "name", name, "scroll", name,
                            "id", profile, "player", player.getName());
                }
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
        return ConfigManager.configManager.getScroll(profile).createRecord(meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING),
                meta.getPersistentDataContainer().get(difficultyKey, PersistentDataType.STRING), RandomGenerator.getDefault());
    }

    public void activate(Player player, UUID token) {
        if (!cacheManager.available(player) || !cacheManager.canActivateScroll(player.getUniqueId(), token)) {
            return;
        }
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        UUID operation = cacheManager.beginPlayerOperation(player);
        if (operation == null) {
            return;
        }
        if (!cacheManager.beginScroll(token, operation)) {
            cacheManager.endPlayerOperation(cache, operation);
            return;
        }
        cacheManager.loadScroll(token).whenComplete((snapshot, failure) -> schedule(player, cache, operation, token,
                () -> reserve(player, cache, operation, token, snapshot, failure)));
    }

    private boolean valid(Player player, ObjectCache cache, UUID operation, UUID token) {
        synchronized (cache) {
            return cacheManager.isSession(cache, player) && cache.operation(operation)
                    && cacheManager.scrollOperation(token, operation);
        }
    }

    private void schedule(Player player, ObjectCache cache, UUID operation, UUID token, Runnable task) {
        if (cacheManager.isStopping()) {
            release(cache, operation, token);
            return;
        }
        try {
            SchedulerUtil.runSync(player, () -> {
                if (valid(player, cache, operation, token)) {
                    task.run();
                } else {
                    release(cache, operation, token);
                }
            }, () -> release(cache, operation, token));
        } catch (RuntimeException exception) {
            fail(cache, operation, token, exception);
        }
    }

    private void reserve(Player player, ObjectCache cache, UUID operation, UUID token,
            QuestPlayerData snapshot, Throwable failure) {
        if (failure != null) {
            fail(cache, operation, token, failure);
            LanguageManager.languageManager.sendStringText(player, "scroll.read-failed");
            return;
        }
        try {
            ItemStack item = findToken(player, token);
            if (item == null) {
                release(cache, operation, token);
                return;
            }
            ItemMeta metadata = item.getItemMeta();
            YamlConfiguration record = readRecord(snapshot, metadata);
            List<QuestDefinition> drawn = new ObjectScrollRecord(record).readQuests();
            if (!Set.of("ISSUED", "ACTIVE").contains(record.getString("state", ""))) {
                cacheManager.deferScroll(cache.playerId(), token, true);
                LanguageManager.languageManager.sendStringText(player, "scroll.used");
                release(cache, operation, token);
                return;
            }
            if (!QuestManager.questManager.canInstallScroll(player, drawn)) {
                cacheManager.deferScroll(cache.playerId(), token, false);
                LanguageManager.languageManager.sendStringText(player, "scroll.requirements");
                release(cache, operation, token);
                return;
            }
            String playerName = player.getName();
            cacheManager.editScroll(token, current -> {
                try {
                    YamlConfiguration latest = readRecord(current, metadata);
                    new ObjectScrollRecord(latest).readQuests();
                    if (!Set.of("ISSUED", "ACTIVE").contains(latest.getString("state", ""))) {
                        throw new IllegalStateException("Scroll is no longer available");
                    }
                    latest.set("state", "ACTIVE");
                    latest.set("owner", cache.playerId().toString());
                    return new QuestPlayerData(token, playerName, Math.addExact(current.revision(), 1L), latest.saveToString());
                } catch (Exception exception) {
                    throw new IllegalArgumentException("Invalid scroll record", exception);
                }
            }).whenComplete((reserved, saveFailure) -> {
                if (saveFailure != null) {
                    fail(cache, operation, token, saveFailure);
                } else {
                    schedule(player, cache, operation, token, () -> install(player, cache, operation, token, reserved));
                }
            });
        } catch (Exception exception) {
            fail(cache, operation, token, exception);
            LanguageManager.languageManager.sendStringText(player, "scroll.invalid");
        }
    }

    private YamlConfiguration readRecord(QuestPlayerData snapshot, ItemMeta metadata) throws Exception {
        YamlConfiguration record = new YamlConfiguration();
        record.loadFromString(snapshot.payload());
        return missingRecord(snapshot, record) ? recoverRecord(metadata) : record;
    }

    private void install(Player player, ObjectCache cache, UUID operation, UUID token, QuestPlayerData reserved) {
        try {
            if (!hasToken(player, token)) {
                throw new IllegalStateException("Reserved scroll was moved before installation");
            }
            YamlConfiguration record = new YamlConfiguration();
            record.loadFromString(reserved.payload());
            List<QuestDefinition> drawn = new ObjectScrollRecord(record).readQuests();
            QuestInstance restored = record.isConfigurationSection("instance")
                    ? new QuestInstance(record.getString("instance-key", ""), record.getConfigurationSection("instance")) : null;
            rememberRecord(player, token, record);
            long seconds = ObjectScroll.readDuration(record);
            long expiresAt = seconds == 0L ? 0L : Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
            String playerName = player.getName();
            QuestManager.questManager.installScroll(player, token, drawn, restored, expiresAt).whenComplete((success, failure) -> {
                boolean installed = failure == null && Boolean.TRUE.equals(success);
                cacheManager.updateScroll(token, playerName, latest -> {
                    if (!latest.getString("owner", "").equals(cache.playerId().toString())) {
                        throw new IllegalStateException("Scroll owner changed during installation");
                    }
                    latest.set("state", installed ? "ACTIVE" : "REVIEW");
                }).whenComplete((ignored, saveFailure) -> {
                    if (failure != null || saveFailure != null) {
                        fail(cache, operation, token, failure == null ? saveFailure : failure);
                    } else {
                        schedule(player, cache, operation, token, () -> {
                            QuestInstance quest = cacheManager.scroll(cache.playerId(), token);
                            if (installed && quest != null) {
                                saveProgress(player, quest);
                                QuestManager.questManager.sendQuestMessage(player, quest, "scroll.activated");
                            } else {
                                LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
                            }
                            release(cache, operation, token);
                        });
                    }
                });
            });
        } catch (Exception exception) {
            fail(cache, operation, token, exception);
            LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
        }
    }

    public void synchronize(Player player) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null || !cacheManager.isSession(cache, player)) {
            return;
        }
        Set<UUID> current = new HashSet<>();
        for (ItemStack item : player.getInventory().getStorageContents()) {
            UUID token = token(item);
            if (token != null) {
                current.add(token);
            }
        }
        UUID offHand = token(player.getInventory().getItemInOffHand());
        if (offHand != null) {
            current.add(offHand);
        }
        synchronized (cache) {
            if (!cacheManager.isCurrent(cache)) {
                return;
            }
            for (UUID removed : cacheManager.scrollTokens(cache.playerId())) {
                if (!current.contains(removed)) {
                    QuestManager.questManager.detachScroll(cache.playerId(), removed);
                }
            }
            cacheManager.carriedScrolls(cache.playerId(), current);
        }
        for (UUID added : current) {
            if (!cacheManager.hasScroll(cache.playerId(), added) && !cacheManager.scrollInFlight(added)
                    && cacheManager.canActivateScroll(cache.playerId(), added)) {
                activate(player, added);
                return;
            }
        }
    }

    public void saveProgress(Player player, QuestInstance quest) {
        if (!quest.source().equals("scroll") || cacheManager.isStopping()) {
            return;
        }
        QuestInstance snapshot;
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        synchronized (cache) {
            snapshot = new QuestInstance(quest);
        }
        UUID token = UUID.fromString(snapshot.sourceId());
        String playerName = player.getName();
        cacheManager.updateScroll(token, playerName, record -> {
            if (!record.getString("owner", "").equals(cache.playerId().toString())
                    || !record.getString("state", "").equals("ACTIVE")) {
                return;
            }
            record.set("instance", null);
            record.set("instance-key", snapshot.key());
            snapshot.write(record.createSection("instance"));
        }).whenComplete((ignored, failure) -> {
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: " + failure.getMessage());
            }
        });
    }

    public void reserveReward(Player player, QuestInstance quest, Runnable success) {
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        if (cache == null) {
            return;
        }
        UUID token = UUID.fromString(quest.sourceId());
        UUID operation = cacheManager.beginPlayerOperation(player);
        if (operation == null) {
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return;
        }
        if (!cacheManager.beginScroll(token, operation)) {
            cacheManager.endPlayerOperation(cache, operation);
            LanguageManager.languageManager.sendStringText(player, "quests.busy");
            return;
        }
        if (!hasToken(player, token)) {
            release(cache, operation, token);
            return;
        }
        String playerName = player.getName();
        cacheManager.updateScroll(token, playerName, record -> {
            if (!record.getString("state", "").equals("ACTIVE")
                    || !record.getString("owner", "").equals(cache.playerId().toString())) {
                throw new IllegalStateException("Scroll is not active for this player");
            }
            record.set("state", "REDEEMING");
        }).whenComplete((ignored, failure) -> {
            if (failure != null) {
                fail(cache, operation, token, failure);
            } else {
                schedule(player, cache, operation, token, () -> {
                    if (!hasToken(player, token)) {
                        release(cache, operation, token);
                        return;
                    }
                    synchronized (cache) {
                        if (valid(player, cache, operation, token)) {
                            success.run();
                        }
                    }
                });
            }
        });
    }

    public void complete(Player player, QuestInstance quest, UUID operation) {
        UUID token = UUID.fromString(quest.sourceId());
        if (!cacheManager.scrollOperation(token, operation)) {
            return;
        }
        ObjectCache cache = cacheManager.getObjectCache(player.getUniqueId());
        cacheManager.updateScroll(token, "", record -> {
            if (!record.getString("state", "").equals("REDEEMING")
                    || !record.getString("owner", "").equals(player.getUniqueId().toString())) {
                throw new IllegalStateException("Scroll reward reservation changed");
            }
            record.set("state", "REDEEMED");
        }).whenComplete((ignored, failure) -> {
            cacheManager.endScroll(token, operation);
            if (failure != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: " + failure.getMessage());
            } else if (cache != null && !cacheManager.isStopping()) {
                try {
                    SchedulerUtil.runSync(player, () -> {
                        if (!cacheManager.isSession(cache, player)) {
                            return;
                        }
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
                        cacheManager.removeCarriedScroll(player.getUniqueId(), token);
                    });
                } catch (RuntimeException exception) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Redeemed scroll item cleanup failed: "
                            + exception.getMessage());
                }
            }
        });
    }

    private void fail(ObjectCache cache, UUID operation, UUID token, Throwable failure) {
        Player session = null;
        synchronized (cache) {
            if (cacheManager.isCurrent(cache) && cache.operation(operation)) {
                cacheManager.deferScroll(cache.playerId(), token, true);
                session = cache.session();
            }
        }
        ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: " + failure.getMessage());
        release(cache, operation, token);
        Player recipient = session;
        if (recipient != null && !cacheManager.isStopping()) {
            try {
                SchedulerUtil.runSync(recipient, () -> {
                    if (cacheManager.isSession(cache, recipient)) {
                        LanguageManager.languageManager.sendStringText(recipient, "quests.review-needed");
                    }
                });
            } catch (RuntimeException exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll failure notification could not be scheduled: "
                        + exception.getMessage());
            }
        }
    }

    private void release(ObjectCache cache, UUID operation, UUID token) {
        cacheManager.endPlayerOperation(cache, operation);
        cacheManager.endScroll(token, operation);
    }
}
