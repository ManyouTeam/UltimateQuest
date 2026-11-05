package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.caches.ObjectCache;
import cn.superiormc.ultimatequest.objects.quests.ObjectScroll;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import static cn.superiormc.ultimatequest.managers.CacheManager.cacheManager;

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
        UUID token = UUID.randomUUID();
        ObjectScrollRecord record = scroll.createRecord(token, rarity, difficulty, RandomGenerator.getDefault());
        ItemStack item = scroll.buildItem(player, record.rarity(), record.difficulty());
        ItemMeta meta = item.getItemMeta();
        meta.getPersistentDataContainer().set(itemKey, PersistentDataType.STRING, token.toString());
        rememberRecord(meta, record);
        item.setItemMeta(meta);
        cacheManager.saveScroll(record).whenComplete((ignored, failure) -> {
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

    private void rememberRecord(ItemMeta meta, ObjectScrollRecord record) {
        meta.getPersistentDataContainer().set(profileKey, PersistentDataType.STRING, record.profile());
        meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, record.rarity());
        meta.getPersistentDataContainer().set(difficultyKey, PersistentDataType.STRING, record.difficulty());
    }

    private void rememberRecord(Player player, UUID token, ObjectScrollRecord record) {
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
            ObjectScrollRecord snapshot, Throwable failure) {
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
            ObjectScrollRecord record = readRecord(snapshot);
            List<QuestDefinition> drawn = record.readQuests();
            if (!Set.of("ISSUED", "ACTIVE").contains(record.state())) {
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
            cacheManager.editScroll(token, current -> {
                try {
                    ObjectScrollRecord latest = readRecord(current).copy();
                    if (!Set.of("ISSUED", "ACTIVE").contains(latest.state())) {
                        throw new IllegalStateException("Scroll is no longer available");
                    }
                    latest.state("ACTIVE");
                    latest.owner(cache.playerId());
                    return latest;
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

    private ObjectScrollRecord readRecord(ObjectScrollRecord record) {
        if (record == null) {
            throw new IllegalStateException("Missing scroll record");
        }
        return record;
    }

    private void install(Player player, ObjectCache cache, UUID operation, UUID token, ObjectScrollRecord reserved) {
        try {
            if (!hasToken(player, token)) {
                throw new IllegalStateException("Reserved scroll was moved before installation");
            }
            ObjectScrollRecord record = readRecord(reserved);
            List<QuestDefinition> drawn = record.readQuests();
            QuestInstance restored = record.instance() == null ? null : new QuestInstance(record.instance(), drawn.get(0));
            rememberRecord(player, token, record);
            long seconds = record.durationSeconds();
            long expiresAt = seconds == 0L ? 0L : Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
            String playerName = player.getName();
            QuestManager.questManager.installScroll(player, token, drawn, restored, expiresAt).whenComplete((success, failure) -> {
                boolean installed = failure == null && Boolean.TRUE.equals(success);
                cacheManager.updateScroll(token, playerName, latest -> {
                    if (!cache.playerId().equals(latest.owner())) {
                        throw new IllegalStateException("Scroll owner changed during installation");
                    }
                    latest.state(installed ? "ACTIVE" : "REVIEW");
                }).whenComplete((ignored, saveFailure) -> {
                    if (failure != null || saveFailure != null) {
                        fail(cache, operation, token, failure == null ? saveFailure : failure);
                    } else {
                        schedule(player, cache, operation, token, () -> {
                            QuestInstance quest = cacheManager.scroll(cache.playerId(), token);
                            if (installed && quest != null) {
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
            if (!record.state().equals("ACTIVE")
                    || !cache.playerId().equals(record.owner())) {
                throw new IllegalStateException("Scroll is not active for this player");
            }
            record.state("REDEEMING");
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
            if (!record.state().equals("REDEEMING")
                    || !cache.playerId().equals(record.owner())) {
                throw new IllegalStateException("Scroll reward reservation changed");
            }
            record.state("REDEEMED");
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
