package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.utils.ResetTimeUtil;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import java.io.File;
import java.time.Instant;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class SeasonManager {

    private final File stateFile = new File(UltimateQuest.instance.getDataFolder(), "season-cycles.yml");

    private YamlConfiguration cycles = new YamlConfiguration();

    private YamlConfiguration working;

    private Instant candidateNext;

    private Instant nextCheck = Instant.MAX;

    private boolean loaded;

    public void beginReload() throws Exception {
        if (!loaded && stateFile.isFile()) {
            cycles.load(stateFile);
        }
        loaded = true;
        working = new YamlConfiguration();
        working.loadFromString(cycles.saveToString());
        candidateNext = Instant.MAX;
    }

    public void prepare(String type, String id, YamlConfiguration config, Instant now) {
        ConfigurationSection rotation = config.getConfigurationSection("rotation");
        if (rotation == null || !rotation.getBoolean("enabled", false)) {
            return;
        }
        Instant start = ResetTimeUtil.previous(rotation, now);
        long saved = working.getLong(type + "/" + id, Long.MIN_VALUE);
        if (saved > start.getEpochSecond()) {
            start = Instant.ofEpochSecond(saved);
        }
        Instant end = ResetTimeUtil.next(rotation, start);
        config.set("starts-at", start.toString());
        config.set("ends-at", end.toString());
        config.set("season", config.getString("season-prefix", "season") + "_" + start.getEpochSecond());
        config.set("rotation", null);
        working.set(type + "/" + id, start.getEpochSecond());
        if (end.isBefore(candidateNext)) {
            candidateNext = end;
        }
    }

    public void commitReload() throws Exception {
        if (!working.getValues(false).equals(cycles.getValues(false))) {
            ConfigurationFileUtil.save(stateFile.toPath(), working, false);
        }
        cycles = working;
        nextCheck = candidateNext;
    }

    public void refresh(QuestManager coordinator) {
        if (Instant.now().isBefore(nextCheck)) {
            return;
        }
        try {
            coordinator.reloadDefinitions();
        } catch (Exception exception) {
            nextCheck = Instant.now().plusSeconds(30L);
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not rotate seasons: " + exception.getMessage());
        }
    }
}
