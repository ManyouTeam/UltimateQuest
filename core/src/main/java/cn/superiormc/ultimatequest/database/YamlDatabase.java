package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestInstanceData;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.objects.quests.QuestSeasonData;
import cn.superiormc.ultimatequest.objects.quests.QuestStageData;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class YamlDatabase extends AbstractDatabase {

    private final Path dataDir;
    private FileChannel lockChannel;
    private FileLock lock;

    public YamlDatabase(Path dataDir) {
        this.dataDir = dataDir;
    }

    @Override
    public synchronized void onInit() {
        try {
            Files.createDirectories(dataDir.resolve("players"));
            Files.createDirectories(dataDir.resolve("scrolls"));
            lockChannel = FileChannel.open(dataDir.resolve("storage.lock"), StandardOpenOption.CREATE, StandardOpenOption.WRITE);
            lock = lockChannel.tryLock();
            if (lock == null) {
                throw new IllegalStateException("Quest file storage is already in use");
            }
            recover();
        } catch (Exception exception) {
            onClose();
            throw new IllegalStateException("Could not initialize quest file storage", exception);
        }
    }

    private YamlConfiguration read(Path file) {
        YamlConfiguration data = new YamlConfiguration();
        if (Files.exists(file)) {
            try {
                data.load(file.toFile());
                if (data.getInt("format") != 2) {
                    throw new IllegalStateException("Unsupported quest file: " + file);
                }
            } catch (Exception exception) {
                throw new IllegalStateException("Could not read quest file: " + file, exception);
            }
        }
        return data;
    }

    private void write(Path file, YamlConfiguration data) {
        Path temporary = null;
        try {
            data.set("format", 2);
            temporary = Files.createTempFile(file.getParent(), "quest-", ".tmp");
            byte[] bytes = data.saveToString().getBytes(StandardCharsets.UTF_8);
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                ByteBuffer buffer = ByteBuffer.wrap(bytes);
                while (buffer.hasRemaining()) {
                    channel.write(buffer);
                }
                channel.force(true);
            }
            Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not commit quest file: " + file, exception);
        } finally {
            if (temporary != null) {
                try {
                    Files.deleteIfExists(temporary);
                } catch (IOException ignored) {
                    // An uncommitted temporary file is never read.
                }
            }
        }
    }

    private List<Path> files(String directory) {
        try (Stream<Path> paths = Files.list(dataDir.resolve(directory))) {
            return paths.filter(path -> path.getFileName().toString().matches("[0-9a-fA-F-]{36}\\.yml")).toList();
        } catch (IOException exception) {
            throw new IllegalStateException("Could not enumerate quest files", exception);
        }
    }

    private UUID fileId(Path file) {
        return UUID.fromString(file.getFileName().toString().substring(0, 36));
    }

    @Override
    public synchronized QuestPlayerData checkData(UUID id) {
        recover();
        YamlConfiguration data = read(dataDir.resolve("players").resolve(id + ".yml"));
        Map<String, QuestInstanceData> quests = new LinkedHashMap<>();
        ConfigurationSection instances = data.getConfigurationSection("quests");
        if (instances != null) {
            for (String key : instances.getKeys(false)) {
                QuestInstanceData quest = readInstance(instances.getConfigurationSection(key));
                quests.put(quest.questKey(), quest);
            }
        }
        for (Path file : files("scrolls")) {
            ObjectScrollRecord scroll = readScroll(fileId(file));
            if (id.equals(scroll.owner()) && scroll.instance() != null) {
                quests.put(scroll.instance().questKey(), scroll.instance());
            }
        }
        Map<String, String> rolls = new LinkedHashMap<>();
        ConfigurationSection periods = data.getConfigurationSection("period-rolls");
        if (periods != null) {
            for (String group : periods.getKeys(false)) {
                rolls.put(group, periods.getString(group));
            }
        }
        return new QuestPlayerData(id, data.getString("player-name", ""), data.getLong("revision"), quests, rolls);
    }

    @Override
    public synchronized void updateData(QuestPlayerData data, QuestPlayerData previous) {
        recover();
        if (!data.playerUUID().equals(previous.playerUUID()) || data.revision() != previous.revision()
                || checkData(data.playerUUID()).revision() != previous.revision()) {
            throw new IllegalStateException("Player file version changed");
        }
        YamlConfiguration player = new YamlConfiguration();
        player.set("player-name", data.playerName());
        player.set("revision", Math.addExact(data.revision(), 1L));
        data.periodRolls().forEach((key, value) -> player.set("period-rolls." + key, value));
        YamlConfiguration operation = new YamlConfiguration();
        operation.set("player-id", data.storageId());
        operation.set("player", player);
        List<String> tokens = new ArrayList<>();
        for (QuestInstanceData quest : data.quests().values()) {
            if (quest.source().equals("scroll")) {
                tokens.add(quest.sourceId());
                if (!quest.equals(previous.quests().get(quest.questKey()))) {
                    ObjectScrollRecord scroll = readScroll(UUID.fromString(quest.sourceId()));
                    if (scroll == null || !data.playerUUID().equals(scroll.owner())
                            || !List.of("ACTIVE", "REDEEMING").contains(scroll.state())) {
                        throw new IllegalStateException("Scroll ownership changed: " + quest.sourceId());
                    }
                    scroll.instance(quest);
                    scroll.revision(Math.addExact(scroll.revision(), 1L));
                    writeScroll(operation.createSection("scrolls." + scroll.token()), scroll);
                }
            } else {
                writeInstance(player.createSection("quests." + quest.instanceId()), quest);
            }
        }
        player.set("scroll-tokens", tokens);
        write(dataDir.resolve("pending.yml"), operation);
        recover();
    }

    private void recover() {
        Path journal = dataDir.resolve("pending.yml");
        if (!Files.exists(journal)) {
            return;
        }
        YamlConfiguration operation = read(journal);
        UUID id = UUID.fromString(operation.getString("player-id"));
        ConfigurationSection scrolls = operation.getConfigurationSection("scrolls");
        if (scrolls != null) {
            for (String token : scrolls.getKeys(false)) {
                UUID scrollId = UUID.fromString(token);
                YamlConfiguration target = new YamlConfiguration();
                copy(scrolls.getConfigurationSection(token), target);
                write(dataDir.resolve("scrolls").resolve(scrollId + ".yml"), target);
            }
        }
        YamlConfiguration target = new YamlConfiguration();
        copy(operation.getConfigurationSection("player"), target);
        write(dataDir.resolve("players").resolve(id + ".yml"), target);
        try {
            Files.delete(journal);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not finish quest file operation", exception);
        }
    }

    private void copy(ConfigurationSection source, ConfigurationSection target) {
        if (source == null) {
            throw new IllegalStateException("Incomplete quest file operation");
        }
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) {
                target.set(key, source.get(key));
            } else {
                copy(child, target.createSection(key));
            }
        }
    }

    @Override
    public synchronized ObjectScrollRecord checkScroll(UUID token) {
        recover();
        return readScroll(token);
    }

    private ObjectScrollRecord readScroll(UUID token) {
        Path file = dataDir.resolve("scrolls").resolve(token + ".yml");
        if (!Files.exists(file)) {
            return null;
        }
        YamlConfiguration data = read(file);
        String owner = data.getString("owner");
        return new ObjectScrollRecord(token, data.getString("profile"), data.getString("rarity"),
                data.getString("difficulty"), data.getLong("duration-seconds"), data.getStringList("objective-ids"),
                data.getLong("revision"), owner == null ? null : UUID.fromString(owner), data.getString("state"),
                data.isConfigurationSection("instance") ? readInstance(data.getConfigurationSection("instance")) : null);
    }

    private void writeScroll(ConfigurationSection data, ObjectScrollRecord scroll) {
        data.set("profile", scroll.profile());
        data.set("rarity", scroll.rarity());
        data.set("difficulty", scroll.difficulty());
        data.set("duration-seconds", scroll.durationSeconds());
        data.set("objective-ids", scroll.objectiveIds());
        data.set("revision", scroll.revision());
        data.set("owner", scroll.owner() == null ? null : scroll.owner().toString());
        data.set("state", scroll.state());
        if (scroll.instance() != null) {
            writeInstance(data.createSection("instance"), scroll.instance());
        }
    }

    @Override
    public synchronized void updateScroll(ObjectScrollRecord data) {
        recover();
        ObjectScrollRecord previous = readScroll(data.token());
        if ((previous == null ? 0L : previous.revision()) != data.revision()) {
            throw new IllegalStateException("Scroll file version changed");
        }
        if (previous != null && (!previous.profile().equals(data.profile())
                || !previous.objectiveIds().equals(data.objectiveIds()))) {
            throw new IllegalArgumentException("Cannot replace scroll selection");
        }
        ObjectScrollRecord saved = data.copy();
        saved.revision(Math.addExact(data.revision(), 1L));
        YamlConfiguration target = new YamlConfiguration();
        writeScroll(target, saved);
        write(dataDir.resolve("scrolls").resolve(data.token() + ".yml"), target);
    }

    private void writeInstance(ConfigurationSection data, QuestInstanceData quest) {
        data.set("id", quest.instanceId().toString());
        data.set("key", quest.questKey());
        data.set("template", quest.templateId());
        data.set("accepted-at", quest.acceptedAt());
        data.set("source", quest.source());
        data.set("source-id", quest.sourceId());
        data.set("cycle", quest.cycle());
        data.set("expires-at", quest.expiresAt());
        data.set("season", quest.season());
        writeWindow(data.createSection("window"), quest.seasonWindow());
        data.set("state", quest.state());
        data.set("total", quest.total().toPlainString());
        data.set("premium", quest.premium());
        data.set("booster", quest.booster());
        data.set("booster-until", quest.boosterUntil());
        data.set("reward-keys", quest.rewardKeys());
        data.set("objective-ids", quest.objectiveIds());
        quest.progress().forEach((id, value) -> data.set("progress." + id, value.toPlainString()));
        int index = 0;
        for (QuestStageData stage : quest.stages().values()) {
            ConfigurationSection entry = data.createSection("stages." + index++);
            entry.set("track", stage.track());
            entry.set("level", stage.level());
            entry.set("state", stage.state());
            entry.set("required", stage.required());
            entry.set("reward-keys", stage.rewardKeys());
        }
    }

    private QuestInstanceData readInstance(ConfigurationSection data) {
        Map<String, BigDecimal> progress = new LinkedHashMap<>();
        ConfigurationSection amounts = data.getConfigurationSection("progress");
        if (amounts != null) {
            for (String id : amounts.getKeys(false)) {
                progress.put(id, new BigDecimal(amounts.getString(id)));
            }
        }
        Map<String, QuestStageData> stages = new LinkedHashMap<>();
        ConfigurationSection entries = data.getConfigurationSection("stages");
        if (entries != null) {
            for (String key : entries.getKeys(false)) {
                ConfigurationSection entry = entries.getConfigurationSection(key);
                QuestStageData stage = new QuestStageData(entry.getString("track"), entry.getInt("level"),
                        entry.getString("state"), entry.getLong("required"), entry.getStringList("reward-keys"));
                stages.put(stage.key(), stage);
            }
        }
        return new QuestInstanceData(UUID.fromString(data.getString("id")), data.getString("key"), data.getString("template"),
                data.getLong("accepted-at"), data.getString("source"), data.getString("source-id"), data.getString("cycle"),
                data.getLong("expires-at"), data.getString("season"), readWindow(data.getConfigurationSection("window")),
                data.getString("state"), progress, new BigDecimal(data.getString("total")), data.getBoolean("premium"),
                data.getDouble("booster"), data.getLong("booster-until"),
                data.contains("reward-keys") ? data.getStringList("reward-keys") : null, stages, data.getStringList("objective-ids"));
    }

    private void writeWindow(ConfigurationSection data, TimeWindow window) {
        data.set("starts-at", window.startsAt() == null ? null : window.startsAt().toEpochMilli());
        data.set("ends-at", window.endsAt() == null ? null : window.endsAt().toEpochMilli());
        data.set("grace-seconds", window.claimGraceSeconds());
        data.set("paused", window.paused());
    }

    private TimeWindow readWindow(ConfigurationSection data) {
        return new TimeWindow(data.contains("starts-at") ? Instant.ofEpochMilli(data.getLong("starts-at")) : null,
                data.contains("ends-at") ? Instant.ofEpochMilli(data.getLong("ends-at")) : null,
                data.getLong("grace-seconds"), data.getBoolean("paused"));
    }

    @Override
    public synchronized Map<String, QuestSeasonData> loadSeasons() {
        YamlConfiguration data = read(dataDir.resolve("seasons.yml"));
        Map<String, QuestSeasonData> result = new LinkedHashMap<>();
        ConfigurationSection entries = data.getConfigurationSection("seasons");
        if (entries != null) {
            for (String key : entries.getKeys(false)) {
                ConfigurationSection entry = entries.getConfigurationSection(key);
                Map<String, TimeWindow> history = new LinkedHashMap<>();
                ConfigurationSection windows = entry.getConfigurationSection("history");
                if (windows != null) {
                    for (String season : windows.getKeys(false)) {
                        history.put(season, readWindow(windows.getConfigurationSection(season)));
                    }
                }
                result.put(key, new QuestSeasonData(entry.getLong("revision"), entry.getString("season", ""),
                        entry.contains("cycle-start") ? entry.getLong("cycle-start") : null,
                        entry.contains("paused") ? entry.getBoolean("paused") : null,
                        entry.getBoolean("rotation-disabled"), entry.isConfigurationSection("window"),
                        entry.isConfigurationSection("window") ? readWindow(entry.getConfigurationSection("window")) : null,
                        entry.getDouble("booster", 1D), entry.getLong("booster-until"), history));
            }
        }
        return Map.copyOf(result);
    }

    @Override
    public synchronized Map<String, QuestSeasonData> saveSeasons(Map<String, QuestSeasonData> data,
            Map<String, QuestSeasonData> previous) {
        Map<String, QuestSeasonData> current = loadSeasons();
        Map<String, QuestSeasonData> saved = new LinkedHashMap<>(current);
        for (Map.Entry<String, QuestSeasonData> entry : data.entrySet()) {
            if (!entry.getValue().equals(previous.get(entry.getKey()))) {
                if (!current.getOrDefault(entry.getKey(), QuestSeasonData.empty())
                        .equals(previous.getOrDefault(entry.getKey(), QuestSeasonData.empty()))) {
                    throw new IllegalStateException("Season file version changed: " + entry.getKey());
                }
                saved.put(entry.getKey(), entry.getValue().withRevision(Math.addExact(entry.getValue().revision(), 1L)));
            }
        }
        if (saved.equals(current)) {
            return Map.copyOf(saved);
        }
        YamlConfiguration target = new YamlConfiguration();
        saved.forEach((key, state) -> {
            ConfigurationSection entry = target.createSection("seasons." + key);
            entry.set("revision", state.revision());
            entry.set("season", state.season());
            entry.set("cycle-start", state.cycleStart());
            entry.set("paused", state.paused());
            entry.set("rotation-disabled", state.rotationDisabled());
            entry.set("booster", state.booster());
            entry.set("booster-until", state.boosterUntil());
            if (state.windowOverride()) {
                writeWindow(entry.createSection("window"), state.window());
            }
            state.history().forEach((season, window) -> writeWindow(entry.createSection("history." + season), window));
        });
        write(dataDir.resolve("seasons.yml"), target);
        return Map.copyOf(saved);
    }

    @Override
    public synchronized List<QuestPlayerData> allPlayers() {
        recover();
        return files("players").stream().map(this::fileId).map(this::checkData).toList();
    }

    @Override
    public synchronized void onClose() {
        try {
            if (lock != null) {
                lock.release();
                lock = null;
            }
            if (lockChannel != null) {
                lockChannel.close();
                lockChannel = null;
            }
        } catch (IOException exception) {
            throw new IllegalStateException("Could not close quest file storage", exception);
        }
    }
}
