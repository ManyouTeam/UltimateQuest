package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.database.AbstractDatabase;
import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.database.SQLDatabase;
import cn.superiormc.ultimatequest.database.YamlDatabase;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.objects.quests.QuestSeasonData;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public final class QuestStorage extends AbstractManager {

    public static QuestStorage questStorage;

    private final AbstractDatabase database;

    public QuestStorage() {
        questStorage = this;
        database = ConfigManager.configManager.getBoolean("database.enabled") ? new SQLDatabase() : new YamlDatabase(UltimateQuest.instance.getDataFolder().toPath().resolve("data"));
        database.onInit();
    }

    public CompletableFuture<QuestPlayerData> load(UUID playerId) {
        return DatabaseExecutor.supplyPlayerTask(playerId.toString(), () -> database.checkData(playerId));
    }

    public CompletableFuture<List<QuestPlayerData>> allPlayers() {
        return DatabaseExecutor.supplyDatabaseTask(database::allPlayers);
    }

    public Map<String, QuestSeasonData> loadSeasons() {
        return database.loadSeasons();
    }

    public Map<String, QuestSeasonData> saveSeasons(Map<String, QuestSeasonData> data,
            Map<String, QuestSeasonData> previous) {
        return database.saveSeasons(data, previous);
    }

    public CompletableFuture<Void> saveCritical(QuestPlayerData data, QuestPlayerData previous) {
        return DatabaseExecutor.executePlayerTask(data.storageId(), () -> database.updateData(data, previous));
    }

    public CompletableFuture<ObjectScrollRecord> loadScroll(UUID token) {
        return DatabaseExecutor.supplyPlayerTask("scroll:" + token, () -> database.checkScroll(token));
    }

    public CompletableFuture<Void> saveScroll(ObjectScrollRecord data) {
        ObjectScrollRecord copy = data.copy();
        return DatabaseExecutor.executePlayerTask("scroll:" + data.token(), () -> database.updateScroll(copy));
    }

    @Override
    public void onPluginDisable() {
        TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fWaiting for all pending database task finished, this may freeze your server if your database is lost connection.");
        if (CacheManager.cacheManager != null) {
            CacheManager.cacheManager.finishShutdown();
        }
        DatabaseExecutor.quiesce();
        DatabaseExecutor.await();
        DatabaseExecutor.shutdown();
        database.onClose();
        questStorage = null;
    }
}
