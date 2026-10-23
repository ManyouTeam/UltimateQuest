package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import java.util.UUID;
import java.util.List;

public abstract class AbstractDatabase {

    public abstract void onInit();

    public abstract QuestPlayerData checkData(UUID playerId);

    public abstract void updateData(QuestPlayerData snapshot);

    public abstract QuestPlayerData checkData(UUID id, boolean scroll);

    public abstract void updateData(QuestPlayerData snapshot, boolean scroll);

    public abstract void onClose();

    public abstract List<QuestPlayerData> allPlayers();
}
