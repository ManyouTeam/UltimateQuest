package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.objects.quests.QuestSeasonData;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public abstract class AbstractDatabase {

    public abstract void onInit();

    public abstract QuestPlayerData checkData(UUID playerId);

    public abstract void updateData(QuestPlayerData data, QuestPlayerData previous);

    public abstract ObjectScrollRecord checkScroll(UUID token);

    public abstract void updateScroll(ObjectScrollRecord data);

    public abstract Map<String, QuestSeasonData> loadSeasons();

    public abstract Map<String, QuestSeasonData> saveSeasons(Map<String, QuestSeasonData> data,
            Map<String, QuestSeasonData> previous);

    public abstract List<QuestPlayerData> allPlayers();

    public abstract void onClose();
}
