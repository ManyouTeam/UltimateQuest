package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.database.AbstractDatabase;
import cn.superiormc.ultimatequest.database.DatabaseExecutor;
import cn.superiormc.ultimatequest.database.SQLDatabase;
import cn.superiormc.ultimatequest.database.YamlDatabase;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.bukkit.configuration.file.YamlConfiguration;

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

    public YamlConfiguration loadSeasons() {
        return database.loadServerData("seasons");
    }

    public void saveSeasons(YamlConfiguration data, long expectedRevision) {
        database.saveServerData("seasons", data, expectedRevision);
    }

    public CompletableFuture<Void> saveCritical(QuestPlayerData snapshot) {
        return DatabaseExecutor.executePlayerTask(snapshot.storageId(), () -> database.updateData(snapshot));
    }

    public CompletableFuture<QuestPlayerData> loadScroll(UUID token) {
        return DatabaseExecutor.supplyPlayerTask("scroll:" + token, () -> database.checkData(token, true));
    }

    public CompletableFuture<Void> saveScroll(QuestPlayerData snapshot) {
        return DatabaseExecutor.executePlayerTask("scroll:" + snapshot.storageId(), () -> database.updateData(snapshot, true));
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
