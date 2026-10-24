package cn.superiormc.ultimatequest.managers;

import static cn.superiormc.ultimatequest.managers.CacheManager.cacheManager;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.quests.ObjectScroll;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.objects.quests.QuestInstance;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
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
        synchronized (cacheManager) {
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
    }

    public void give(Player player, String profile, String rarity, String difficulty) {
        synchronized (cacheManager) {
            YamlConfiguration record = ConfigManager.configManager.getScroll(profile).createRecord(rarity, difficulty, RandomGenerator.getDefault());
            ItemStack item = ConfigManager.configManager.getScroll(profile).buildItem(player, record.getString("rarity"), record.getString("difficulty"));
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
                        LanguageManager.languageManager.sendStringText(player, "scroll.given");
                    }
                });
            });
        }
    }

    public void activate(Player player, UUID token) {
        synchronized (cacheManager) {
            if (!cacheManager.canActivateScroll(player.getUniqueId(), token)) {
                return;
            }
            if (!cacheManager.beginScroll(token)) {
                return;
            }
            if (!cacheManager.beginPlayerOperation(player)) {
                cacheManager.endScroll(token);
                return;
            }
            try {
                cacheManager.loadScroll(token).whenComplete((snapshot, failure) -> {
                    if (cacheManager.isStopping()) {
                        return;
                    }
                    try {
                        SchedulerUtil.runSync(player, () -> reserve(player, token, snapshot, failure),
                                () -> release(player.getUniqueId(), token));
                    } catch (RuntimeException exception) {
                        if (cacheManager.deferScroll(player.getUniqueId(), token, false)) {
                            ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be scheduled: "
                                    + exception.getMessage());
                        }
                        release(player.getUniqueId(), token);
                    }
                });
            } catch (RuntimeException exception) {
                if (cacheManager.deferScroll(player.getUniqueId(), token, false)) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " could not be read: "
                            + exception.getMessage());
                    LanguageManager.languageManager.sendStringText(player, "scroll.read-failed");
                }
                release(player.getUniqueId(), token);
            }
        }
    }

    private void reserve(Player player, UUID token, QuestPlayerData snapshot, Throwable failure) {
        synchronized (cacheManager) {
            try {
                if (failure != null) {
                    if (cacheManager.deferScroll(player.getUniqueId(), token, false)) {
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
                List<QuestDefinition> drawn = new ObjectScrollRecord(record).readQuests();
                QuestInstance saved = null;
                if (record.isConfigurationSection("instance")) {
                    saved = new QuestInstance(record.getString("instance-key", ""),
                            record.getConfigurationSection("instance"));
                }
                if (!Set.of("ISSUED", "ACTIVE").contains(record.getString("state"))) {
                    if (cacheManager.deferScroll(player.getUniqueId(), token, true)) {
                        LanguageManager.languageManager.sendStringText(player, "scroll.used");
                    }
                    release(player.getUniqueId(), token);
                    return;
                }
                if (!QuestManager.questManager.canInstallScroll(player, drawn)) {
                    if (cacheManager.deferScroll(player.getUniqueId(), token, false)) {
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
                cacheManager.saveScroll(new QuestPlayerData(token, player.getName(), revision,
                        reserved.saveToString())).whenComplete((ignored, saveFailure) -> {
                            if (cacheManager.isStopping()) {
                                return;
                            }
                    if (saveFailure != null) {
                        if (cacheManager.deferScroll(player.getUniqueId(), token, true)) {
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
                if (cacheManager.deferScroll(player.getUniqueId(), token, true)) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + exception.getMessage());
                    LanguageManager.languageManager.sendStringText(player, "scroll.invalid");
                }
                release(player.getUniqueId(), token);
            }
        }
    }

    private void install(Player player, UUID token, long revision, YamlConfiguration record,
            List<QuestDefinition> drawn, QuestInstance restored) {
        synchronized (cacheManager) {
            if (!player.isOnline() || !hasToken(player, token)) {
                cacheManager.deferScroll(player.getUniqueId(), token, true);
                ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: Reserved scroll was moved or its owner disconnected; manual"
                        + " review required");
                if (player.isOnline()) {
                    LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
                }
                release(player.getUniqueId(), token);
                return;
            }
            rememberRecord(player, token, record);
            long seconds = ObjectScroll.readDuration(record);
            long expiresAt = seconds == 0L ? 0L : Math.addExact(System.currentTimeMillis(), Math.multiplyExact(seconds, 1000L));
            QuestManager.questManager.installScroll(player, token, drawn, restored, expiresAt).whenComplete((success, failure) -> {
                if (cacheManager.isStopping()) {
                    return;
                }
                record.set("state", failure == null && Boolean.TRUE.equals(success) ? "ACTIVE" : "REVIEW");
                cacheManager.saveScroll(new QuestPlayerData(token, player.getName(), revision + 1L,
                        record.saveToString())).whenComplete((ignored, saveFailure) -> {
                            if (cacheManager.isStopping()) {
                                return;
                            }
                    if (saveFailure != null) {
                        ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                                + saveFailure.getMessage());
                    }
                    SchedulerUtil.runSync(player, () -> {
                        if (Boolean.TRUE.equals(success) && failure == null) {
                            QuestInstance quest = cacheManager.scroll(player.getUniqueId(), token);
                            if (quest != null) {
                                saveProgress(player, quest);
                            }
                            LanguageManager.languageManager.sendStringText(player,
                                    saveFailure == null ? "scroll.activated" : "quests.review-needed");
                        } else {
                            if (cacheManager.deferScroll(player.getUniqueId(), token, true)) {
                                if (failure != null) {
                                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token
                                            + " could not be installed: " + failure.getMessage());
                                }
                                LanguageManager.languageManager.sendStringText(player, "quests.review-needed");
                            }
                        }
                        cacheManager.endScroll(token);
                    }, () -> cacheManager.endScroll(token));
                });
            });
        }
    }

    public boolean hasToken(Player player, UUID token) {
        synchronized (cacheManager) {
            return findToken(player, token) != null;
        }
    }

    private ItemStack findToken(Player player, UUID token) {
        synchronized (cacheManager) {
            for (ItemStack item : player.getInventory().getStorageContents()) {
                if (token.equals(token(item))) {
                    return item;
                }
            }
            ItemStack offHand = player.getInventory().getItemInOffHand();
            return token.equals(token(offHand)) ? offHand : null;
        }
    }

    public void synchronize(Player player) {
        synchronized (cacheManager) {
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
            for (UUID removed : cacheManager.scrollTokens(player.getUniqueId())) {
                if (!current.contains(removed)) {
                    QuestManager.questManager.detachScroll(player.getUniqueId(), removed);
                }
            }
            cacheManager.carriedScrolls(player.getUniqueId(), current);
            for (UUID added : current) {
                if (!cacheManager.hasScroll(player.getUniqueId(), added)
                        && !cacheManager.scrollInFlight(added) && cacheManager.canActivateScroll(player.getUniqueId(), added)) {
                    activate(player, added);
                    return;
                }
            }
        }
    }

    public void saveProgress(Player player, QuestInstance quest) {
        synchronized (cacheManager) {
            if (!quest.source().equals("scroll") || cacheManager.isStopping()) {
                return;
            }
            UUID token = UUID.fromString(quest.sourceId());
            YamlConfiguration instance = new YamlConfiguration();
            quest.write(instance);
            String serializedInstance = instance.saveToString();
            String instanceKey = quest.key();
            cacheManager.updateScroll(token, player.getName(), record -> {
                try {
                    YamlConfiguration savedInstance = new YamlConfiguration();
                    savedInstance.loadFromString(serializedInstance);
                    record.set("instance", null);
                    record.set("instance-key", instanceKey);
                    ConfigurationFileUtil.copy(savedInstance, record.createSection("instance"));
                } catch (Exception exception) {
                    throw new IllegalArgumentException("Invalid scroll instance", exception);
                }
            }).whenComplete((ignored, failure) -> {
                if (cacheManager.isStopping()) {
                    return;
                }
                if (failure != null) {
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + failure.getMessage());
                }
            });
        }
    }

    public void reserveReward(Player player, QuestInstance quest, Runnable success) {
        synchronized (cacheManager) {
            UUID token = UUID.fromString(quest.sourceId());
            if (!cacheManager.beginScroll(token)) {
                LanguageManager.languageManager.sendStringText(player, "quests.busy");
                return;
            }
            if (!cacheManager.beginPlayerOperation(player)) {
                cacheManager.endScroll(token);
                return;
            }
            if (!hasToken(player, token)) {
                release(player.getUniqueId(), token);
                return;
            }
            cacheManager.updateScroll(token, player.getName(), record -> {
                if (!record.getString("state", "").equals("ACTIVE")) {
                    throw new IllegalStateException("Scroll is not active");
                }
                record.set("state", "REDEEMING");
            }).whenComplete((ignored, failure) -> {
                if (cacheManager.isStopping()) {
                    return;
                }
                    if (cacheManager.isStopping()) {
                        return;
                    }
                if (failure != null) {
                    release(player.getUniqueId(), token);
                    ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                            + failure.getMessage());
                    SchedulerUtil.runSync(player,
                            () -> LanguageManager.languageManager.sendStringText(player, "scroll.used"));
                    return;
                }
                SchedulerUtil.runSync(player, () -> {
                    synchronized (cacheManager) {
                        if (!player.isOnline() || !hasToken(player, token) || cacheManager.isStopping()) {
                            release(player.getUniqueId(), token);
                            return;
                        }
                        success.run();
                    }
                }, () -> release(player.getUniqueId(), token));
            });
        }
    }

    public void complete(Player player, QuestInstance quest) {
        synchronized (cacheManager) {
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
            cacheManager.removeCarriedScroll(player.getUniqueId(), token);
            cacheManager.updateScroll(token, player.getName(), record -> record.set("state", "REDEEMED"))
                    .whenComplete((ignored, failure) -> {
                        if (cacheManager.isStopping()) {
                            return;
                        }
                        if (failure != null) {
                            ErrorManager.errorManager.sendErrorMessage("§cError: Scroll " + token + " failed: "
                                    + failure.getMessage());
                        }
                    });
            cacheManager.endScroll(token);
        }
    }

    private boolean missingRecord(QuestPlayerData snapshot, YamlConfiguration record) {
        synchronized (cacheManager) {
            return snapshot == null || snapshot.revision() == 0L
                    && (record.getKeys(false).isEmpty() || record.getKeys(false).equals(Set.of("schema-version"))
                    && record.getInt("schema-version") == 4);
        }
    }

    private void rememberRecord(ItemMeta meta, YamlConfiguration record) {
        synchronized (cacheManager) {
            meta.getPersistentDataContainer().set(profileKey, PersistentDataType.STRING, record.getString("profile", ""));
            meta.getPersistentDataContainer().set(rarityKey, PersistentDataType.STRING, record.getString("rarity", "common"));
            meta.getPersistentDataContainer().set(difficultyKey, PersistentDataType.STRING, record.getString("difficulty", "normal"));
        }
    }

    private void rememberRecord(Player player, UUID token, YamlConfiguration record) {
        synchronized (cacheManager) {
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
    }

    private YamlConfiguration recoverRecord(Player player, UUID token) {
        synchronized (cacheManager) {
            ItemStack item = findToken(player, token);
            if (item == null) {
                throw new IllegalArgumentException("Missing scroll item while rebuilding its record");
            }
            ItemMeta meta = item.getItemMeta();
            return recoverRecord(meta);
        }
    }

    private YamlConfiguration recoverRecord(ItemMeta meta) {
        synchronized (cacheManager) {
            String profile = meta.getPersistentDataContainer().get(profileKey, PersistentDataType.STRING);
            if (profile == null) {
                throw new IllegalArgumentException("Scroll item is missing its profile metadata");
            }
            return ConfigManager.configManager.getScroll(profile).createRecord(meta.getPersistentDataContainer().get(rarityKey, PersistentDataType.STRING),
                    meta.getPersistentDataContainer().get(difficultyKey, PersistentDataType.STRING), RandomGenerator.getDefault());
        }
    }

    private void release(UUID player, UUID token) {
        synchronized (cacheManager) {
            cacheManager.endPlayerOperation(player);
            cacheManager.endScroll(token);
        }
    }

}
