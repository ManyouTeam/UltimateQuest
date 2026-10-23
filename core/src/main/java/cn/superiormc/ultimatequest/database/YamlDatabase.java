package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlDatabase extends AbstractDatabase {

    private final Path dataDir;

    public YamlDatabase(Path dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public void onInit() {
        try {
            Files.createDirectories(dataDir);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not create player data directory", exception);
        }
    }

    @Override
    public synchronized QuestPlayerData checkData(UUID playerId) {
        return checkData(playerId, false);
    }

    @Override
    public synchronized QuestPlayerData checkData(UUID playerId, boolean scroll) {
        Path file = (scroll ? dataDir.resolve("scrolls") : dataDir).resolve(playerId + ".yml");
        if (!Files.exists(file)) {
            return new QuestPlayerData(playerId, "", 0L, "schema-version: 4\n");
        }
        try {
            YamlConfiguration config = new YamlConfiguration();
            config.load(file.toFile());
            if (config.getInt("storage-version") != 1 || !config.isString("payload")) {
                throw new IllegalStateException("Unsupported or incomplete player data: " + playerId);
            }
            return new QuestPlayerData(playerId, config.getString("playerName", ""), config.getLong("revision"), config.getString("payload"));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not load player data: " + playerId, exception);
        }
    }

    @Override
    public synchronized void updateData(QuestPlayerData snapshot) {
        updateData(snapshot, false);
    }

    @Override
    public synchronized void updateData(QuestPlayerData snapshot, boolean scroll) {
        QuestPlayerData previous = checkData(snapshot.playerUUID(), scroll);
        if (previous.revision() > snapshot.revision()) {
            return;
        }
        Path directory = scroll ? dataDir.resolve("scrolls") : dataDir;
        Path file = directory.resolve(snapshot.dataFileName());
        Path temporary = null;
        try {
            Files.createDirectories(directory);
            YamlConfiguration config = new YamlConfiguration();
            config.set("storage-version", 1);
            config.set("playerName", snapshot.playerName());
            config.set("revision", snapshot.revision());
            config.set("payload", snapshot.payload());
            temporary = Files.createTempFile(directory, snapshot.storageId(), ".tmp");
            Files.writeString(temporary, config.saveToString(), StandardCharsets.UTF_8);
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not save player data: " + snapshot.storageId(), exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // A leftover temporary file never replaces the last committed player data.
                }
            }
        }
    }

    @Override
    public void onClose() {
        // No open handles are retained by the YAML backend.
    }

    @Override
    public synchronized List<QuestPlayerData> allPlayers() {
        List<QuestPlayerData> players = new ArrayList<>();
        try (Stream<Path> files = Files.list(dataDir)) {
            for (Path file : files.filter(path -> path.getFileName().toString()
                    .matches("[0-9a-fA-F-]{36}\\.yml")).toList()) {
                players.add(checkData(UUID.fromString(file.getFileName().toString().replace(".yml", ""))));
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not enumerate player data", exception);
        }
        return players;
    }
}
