package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.database.sql.DatabaseDialect;
import cn.superiormc.ultimatequest.database.sql.H2Dialect;
import cn.superiormc.ultimatequest.database.sql.MySQLDialect;
import cn.superiormc.ultimatequest.database.sql.PostgreSQLDialect;
import cn.superiormc.ultimatequest.database.sql.SQLiteDialect;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.objects.quests.ObjectScrollRecord;
import cn.superiormc.ultimatequest.objects.quests.QuestInstanceData;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.objects.quests.QuestSeasonData;
import cn.superiormc.ultimatequest.objects.quests.QuestStageData;
import cn.superiormc.ultimatequest.objects.time.TimeWindow;
import cn.superiormc.ultimatequest.utils.TextUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class SQLDatabase extends AbstractDatabase {

    private HikariDataSource dataSource;

    private DatabaseDialect dialect;

    @Override
    public void onInit() {
        ConfigManager manager = ConfigManager.configManager;
        String jdbcUrl = manager.getString("database.jdbc-url", "");
        dialect = List.of(new MySQLDialect(), new H2Dialect(), new PostgreSQLDialect(), new SQLiteDialect()).stream().filter(candidate -> candidate.matches(jdbcUrl)).findFirst().orElseThrow(() -> new IllegalArgumentException("Unsupported JDBC URL"));
        try {
            dialect.needExtraDownload(jdbcUrl);
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(jdbcUrl);
            String user = manager.getString("database.properties.user", "");
            if (!user.isBlank()) {
                config.setUsername(user);
                config.setPassword(manager.getString("database.properties.password", ""));
            }
            config.setPoolName("UltimateQuest-Hikari");
            config.setMaximumPoolSize(dialect.maxPoolSize());
            config.setMinimumIdle(dialect.minIdle());
            dataSource = new HikariDataSource(config);
            try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
                for (String sql : dialect.createTables()) {
                    statement.execute(sql);
                }
            }
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fConnected to SQL database.");
        } catch (Exception exception) {
            onClose();
            throw new IllegalStateException("Could not initialize SQL database", exception);
        }
    }

    private Connection transaction() throws SQLException {
        Connection connection = dataSource.getConnection();
        try {
            connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
            connection.setAutoCommit(false);
            return connection;
        } catch (SQLException exception) {
            connection.close();
            throw exception;
        }
    }

    @Override
    public QuestPlayerData checkData(UUID playerId) {
        try (Connection connection = transaction()) {
            QuestPlayerData result = readPlayer(connection, playerId);
            connection.commit();
            return result;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load SQL player: " + playerId, exception);
        }
    }

    private QuestPlayerData readPlayer(Connection connection, UUID id) throws SQLException {
        String name = "";
        long revision = 0L;
        try (PreparedStatement statement = query(connection,
                "SELECT player_name, storage_version FROM ultimatequest_players WHERE player_uuid = ?", id.toString());
                ResultSet rows = statement.executeQuery()) {
            if (rows.next()) {
                name = rows.getString(1);
                revision = rows.getLong(2);
            }
        }
        Map<String, QuestInstanceData> quests = new LinkedHashMap<>();
        try (PreparedStatement statement = query(connection,
                "SELECT * FROM ultimatequest_instances WHERE player_uuid = ?", id.toString());
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                QuestInstanceData data = readInstance(connection, rows);
                quests.put(data.questKey(), data);
            }
        }
        Map<String, String> rolls = new LinkedHashMap<>();
        try (PreparedStatement statement = query(connection,
                "SELECT group_id, cycle FROM ultimatequest_period_rolls WHERE player_uuid = ?", id.toString());
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                rolls.put(rows.getString(1), rows.getString(2));
            }
        }
        return new QuestPlayerData(id, name, revision, quests, rolls);
    }

    @Override
    public void updateData(QuestPlayerData data, QuestPlayerData previous) {
        if (!data.playerUUID().equals(previous.playerUUID()) || data.revision() != previous.revision()) {
            throw new IllegalArgumentException("Mismatched player save baseline");
        }
        try (Connection connection = transaction()) {
            try {
                version(connection, "players", "player_uuid", data.storageId(), data.revision(),
                        fields("player_name", data.playerName()));
                for (QuestInstanceData old : previous.quests().values()) {
                    QuestInstanceData replacement = data.quests().get(old.questKey());
                    if (!old.source().equals("scroll") && replacement != null
                            && !replacement.instanceId().equals(old.instanceId())) {
                        deleteInstance(connection, old.instanceId());
                    }
                }
                for (QuestInstanceData quest : data.quests().values()) {
                    if (!quest.equals(previous.quests().get(quest.questKey()))) {
                        if (quest.source().equals("scroll")) {
                            try (PreparedStatement statement = query(connection,
                                    "UPDATE ultimatequest_scrolls SET storage_version = storage_version + 1, instance_id = ?"
                                            + " WHERE token = ? AND owner_uuid = ? AND state IN ('ACTIVE', 'REDEEMING')",
                                    quest.instanceId().toString(), quest.sourceId(), data.storageId())) {
                                if (statement.executeUpdate() != 1) {
                                    throw new IllegalStateException("Scroll ownership changed: " + quest.sourceId());
                                }
                            }
                        }
                        writeInstance(connection, data.playerUUID(), quest);
                    }
                }
                for (QuestInstanceData removed : previous.quests().values()) {
                    if (!removed.source().equals("scroll") && !data.quests().containsKey(removed.questKey())) {
                        deleteInstance(connection, removed.instanceId());
                    }
                }
                for (String group : previous.periodRolls().keySet()) {
                    if (!data.periodRolls().containsKey(group)) {
                        execute(connection, "DELETE FROM ultimatequest_period_rolls WHERE player_uuid = ? AND group_id = ?",
                                data.storageId(), group);
                    }
                }
                for (Map.Entry<String, String> roll : data.periodRolls().entrySet()) {
                    if (!roll.getValue().equals(previous.periodRolls().get(roll.getKey()))) {
                        execute(connection, "DELETE FROM ultimatequest_period_rolls WHERE player_uuid = ? AND group_id = ?",
                                data.storageId(), roll.getKey());
                        insert(connection, "period_rolls", fields("player_uuid", data.storageId(),
                                "group_id", roll.getKey(), "cycle", roll.getValue()));
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQL player: " + data.storageId(), exception);
        }
    }

    private void version(Connection connection, String table, String key, String id, long expected,
            Map<String, Object> values) throws SQLException {
        Map<String, Object> next = new LinkedHashMap<>(values);
        next.put("storage_version", Math.addExact(expected, 1L));
        List<Object> arguments = new ArrayList<>(next.values());
        arguments.add(id);
        arguments.add(expected);
        String assignments = String.join(", ", next.keySet().stream().map(column -> column + " = ?").toList());
        try (PreparedStatement statement = query(connection, "UPDATE ultimatequest_" + table + " SET " + assignments
                + " WHERE " + key + " = ? AND storage_version = ?", arguments.toArray())) {
            if (statement.executeUpdate() == 1) {
                return;
            }
        }
        if (expected != 0L) {
            throw new IllegalStateException("Stored data changed: " + table + "/" + id);
        }
        next.put(key, id);
        insert(connection, table, next);
    }

    private QuestInstanceData readInstance(Connection connection, ResultSet row) throws SQLException {
        UUID id = UUID.fromString(row.getString("instance_id"));
        Map<String, BigDecimal> progress = new LinkedHashMap<>();
        try (PreparedStatement statement = query(connection,
                "SELECT * FROM ultimatequest_objectives WHERE instance_id = ?", id.toString());
                ResultSet values = statement.executeQuery()) {
            while (values.next()) {
                progress.put(values.getString("objective_id"), decimal(values, "progress"));
            }
        }
        Map<String, QuestStageData> stages = new LinkedHashMap<>();
        try (PreparedStatement statement = query(connection,
                "SELECT * FROM ultimatequest_stages WHERE instance_id = ?", id.toString());
                ResultSet values = statement.executeQuery()) {
            while (values.next()) {
                String track = values.getString("track");
                int level = values.getInt("stage_level");
                QuestStageData stage = new QuestStageData(track, level, values.getString("state"),
                        values.getLong("required"), choices(connection, id, "stage", track, level));
                stages.put(stage.key(), stage);
            }
        }
        String token = row.getString("scroll_token");
        List<String> objectives = token == null ? List.of() : scrollObjectives(connection, UUID.fromString(token));
        return new QuestInstanceData(id, row.getString("quest_key"), row.getString("template_id"),
                row.getLong("accepted_at"), row.getString("source"), row.getString("source_id"), row.getString("cycle"),
                row.getLong("expires_at"), row.getString("season"), window(row), row.getString("state"), progress,
                decimal(row, "total"), row.getInt("premium") != 0, row.getDouble("booster"), row.getLong("booster_until"),
                row.getInt("reward_selected") == 0 ? null : choices(connection, id, "quest", "", 0), stages, objectives);
    }

    private void writeInstance(Connection connection, UUID player, QuestInstanceData data) throws SQLException {
        Map<String, Object> values = fields("player_uuid", player == null ? null : player.toString(),
                "quest_key", data.questKey(), "template_id", data.templateId(), "source", data.source(),
                "source_id", data.sourceId(), "cycle", data.cycle(), "season", data.season(),
                "accepted_at", data.acceptedAt(), "expires_at", data.expiresAt(),
                "window_starts", millis(data.seasonWindow().startsAt()), "window_ends", millis(data.seasonWindow().endsAt()),
                "grace_seconds", data.seasonWindow().claimGraceSeconds(), "window_paused", data.seasonWindow().paused() ? 1 : 0, "state", data.state(),
                "total_whole", data.total().longValue(), "total_fraction", fraction(data.total()),
                "premium", data.premium() ? 1 : 0, "booster", data.booster(), "booster_until", data.boosterUntil(),
                "reward_selected", data.rewardKeys() == null ? 0 : 1,
                "scroll_token", data.source().equals("scroll") ? data.sourceId() : null);
        upsert(connection, "instances", "instance_id", data.instanceId().toString(), values);
        clearInstanceChildren(connection, data.instanceId());
        for (Map.Entry<String, BigDecimal> objective : data.progress().entrySet()) {
            insert(connection, "objectives", fields("instance_id", data.instanceId().toString(),
                    "objective_id", objective.getKey(), "progress_whole", objective.getValue().longValue(),
                    "progress_fraction", fraction(objective.getValue())));
        }
        if (data.rewardKeys() != null) {
            writeChoices(connection, data.instanceId(), "quest", "", 0, data.rewardKeys());
        }
        for (QuestStageData stage : data.stages().values()) {
            insert(connection, "stages", fields("instance_id", data.instanceId().toString(), "track", stage.track(),
                    "stage_level", stage.level(), "state", stage.state(), "required", stage.required()));
            writeChoices(connection, data.instanceId(), "stage", stage.track(), stage.level(), stage.rewardKeys());
        }
    }

    private void clearInstanceChildren(Connection connection, UUID id) throws SQLException {
        for (String table : List.of("objectives", "stages", "reward_choices")) {
            execute(connection, "DELETE FROM ultimatequest_" + table + " WHERE instance_id = ?", id.toString());
        }
    }

    private void deleteInstance(Connection connection, UUID id) throws SQLException {
        clearInstanceChildren(connection, id);
        execute(connection, "DELETE FROM ultimatequest_instances WHERE instance_id = ?", id.toString());
    }

    private List<String> choices(Connection connection, UUID id, String scope, String track, int level) throws SQLException {
        List<String> result = new ArrayList<>();
        try (PreparedStatement statement = query(connection, "SELECT reward_key FROM ultimatequest_reward_choices"
                + " WHERE instance_id = ? AND scope = ? AND track = ? AND stage_level = ? ORDER BY position",
                id.toString(), scope, track, level); ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                result.add(rows.getString(1));
            }
        }
        return result;
    }

    private void writeChoices(Connection connection, UUID id, String scope, String track, int level,
            List<String> keys) throws SQLException {
        for (int index = 0; index < keys.size(); index++) {
            insert(connection, "reward_choices", fields("instance_id", id.toString(), "scope", scope,
                    "track", track, "stage_level", level, "position", index, "reward_key", keys.get(index)));
        }
    }

    @Override
    public ObjectScrollRecord checkScroll(UUID token) {
        try (Connection connection = transaction()) {
            ObjectScrollRecord data = readScroll(connection, token);
            connection.commit();
            return data;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load SQL scroll: " + token, exception);
        }
    }

    private ObjectScrollRecord readScroll(Connection connection, UUID token) throws SQLException {
        try (PreparedStatement statement = query(connection, "SELECT * FROM ultimatequest_scrolls WHERE token = ?", token.toString());
                ResultSet row = statement.executeQuery()) {
            if (!row.next()) {
                return null;
            }
            QuestInstanceData instance = null;
            String id = row.getString("instance_id");
            if (id != null) {
                try (PreparedStatement instanceQuery = query(connection,
                        "SELECT * FROM ultimatequest_instances WHERE instance_id = ?", id);
                        ResultSet instanceRow = instanceQuery.executeQuery()) {
                    if (!instanceRow.next()) {
                        throw new IllegalStateException("Missing scroll instance: " + id);
                    }
                    instance = readInstance(connection, instanceRow);
                }
            }
            String owner = row.getString("owner_uuid");
            return new ObjectScrollRecord(token, row.getString("profile"), row.getString("rarity"), row.getString("difficulty"),
                    row.getLong("duration_seconds"), scrollObjectives(connection, token), row.getLong("storage_version"),
                    owner == null ? null : UUID.fromString(owner), row.getString("state"), instance);
        }
    }

    private List<String> scrollObjectives(Connection connection, UUID token) throws SQLException {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement statement = query(connection,
                "SELECT objective_id FROM ultimatequest_scroll_objectives WHERE token = ? ORDER BY position", token.toString());
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                ids.add(rows.getString(1));
            }
        }
        return ids;
    }

    @Override
    public void updateScroll(ObjectScrollRecord data) {
        try (Connection connection = transaction()) {
            try {
                ObjectScrollRecord previous = readScroll(connection, data.token());
                if (previous != null && (!previous.profile().equals(data.profile())
                        || !previous.objectiveIds().equals(data.objectiveIds()))) {
                    throw new IllegalArgumentException("Cannot replace scroll selection");
                }
                version(connection, "scrolls", "token", data.token().toString(), data.revision(),
                        fields("profile", data.profile(), "rarity", data.rarity(), "difficulty", data.difficulty(),
                                "duration_seconds", data.durationSeconds(), "owner_uuid", data.owner() == null ? null : data.owner().toString(),
                                "state", data.state(), "instance_id", data.instance() == null ? null : data.instance().instanceId().toString()));
                if (data.instance() != null) {
                    writeInstance(connection, data.owner(), data.instance());
                }
                if (previous == null) {
                    for (int index = 0; index < data.objectiveIds().size(); index++) {
                        insert(connection, "scroll_objectives", fields("token", data.token().toString(), "position", index,
                                "objective_id", data.objectiveIds().get(index)));
                    }
                }
                connection.commit();
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQL scroll: " + data.token(), exception);
        }
    }

    @Override
    public Map<String, QuestSeasonData> loadSeasons() {
        try (Connection connection = transaction()) {
            Map<String, QuestSeasonData> result = new LinkedHashMap<>();
            try (PreparedStatement statement = query(connection, "SELECT * FROM ultimatequest_seasons");
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    String key = rows.getString("catalog_key");
                    Map<String, TimeWindow> history = new LinkedHashMap<>();
                    try (PreparedStatement historyQuery = query(connection,
                            "SELECT * FROM ultimatequest_season_history WHERE catalog_key = ?", key);
                            ResultSet windows = historyQuery.executeQuery()) {
                        while (windows.next()) {
                            history.put(windows.getString("season"), window(windows));
                        }
                    }
                    Long paused = nullableLong(rows, "paused");
                    result.put(key, new QuestSeasonData(rows.getLong("storage_version"), rows.getString("season"),
                            nullableLong(rows, "cycle_start"), paused == null ? null : paused != 0,
                            rows.getInt("rotation_disabled") != 0, rows.getInt("window_override") != 0,
                            rows.getInt("window_override") == 0 ? null : window(rows), rows.getDouble("booster"),
                            rows.getLong("booster_until"), history));
                }
            }
            connection.commit();
            return Map.copyOf(result);
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load SQL seasons", exception);
        }
    }

    @Override
    public Map<String, QuestSeasonData> saveSeasons(Map<String, QuestSeasonData> data,
            Map<String, QuestSeasonData> previous) {
        Map<String, QuestSeasonData> saved = new LinkedHashMap<>(data);
        try (Connection connection = transaction()) {
            try {
                for (Map.Entry<String, QuestSeasonData> entry : data.entrySet()) {
                    QuestSeasonData state = entry.getValue();
                    if (state.equals(previous.get(entry.getKey()))) {
                        continue;
                    }
                    TimeWindow window = state.window();
                    version(connection, "seasons", "catalog_key", entry.getKey(), state.revision(),
                            fields("season", state.season(), "cycle_start", state.cycleStart(),
                                    "paused", state.paused() == null ? null : state.paused() ? 1 : 0,
                                    "rotation_disabled", state.rotationDisabled() ? 1 : 0,
                                    "window_override", state.windowOverride() ? 1 : 0,
                                    "window_starts", window == null ? null : millis(window.startsAt()),
                                    "window_ends", window == null ? null : millis(window.endsAt()),
                                    "grace_seconds", window == null ? 0L : window.claimGraceSeconds(),
                                    "window_paused", window != null && window.paused() ? 1 : 0,
                                    "booster", state.booster(), "booster_until", state.boosterUntil()));
                    execute(connection, "DELETE FROM ultimatequest_season_history WHERE catalog_key = ?", entry.getKey());
                    for (Map.Entry<String, TimeWindow> history : state.history().entrySet()) {
                        insert(connection, "season_history", fields("catalog_key", entry.getKey(), "season", history.getKey(),
                                "window_starts", millis(history.getValue().startsAt()), "window_ends", millis(history.getValue().endsAt()),
                                "grace_seconds", history.getValue().claimGraceSeconds(),
                                "window_paused", history.getValue().paused() ? 1 : 0));
                    }
                    saved.put(entry.getKey(), state.withRevision(Math.addExact(state.revision(), 1L)));
                }
                connection.commit();
                return Map.copyOf(saved);
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQL seasons", exception);
        }
    }

    @Override
    public List<QuestPlayerData> allPlayers() {
        try (Connection connection = transaction()) {
            List<UUID> ids = new ArrayList<>();
            try (PreparedStatement statement = query(connection, "SELECT player_uuid FROM ultimatequest_players");
                    ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    ids.add(UUID.fromString(rows.getString(1)));
                }
            }
            List<QuestPlayerData> players = new ArrayList<>();
            for (UUID id : ids) {
                players.add(readPlayer(connection, id));
            }
            connection.commit();
            return players;
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not enumerate SQL players", exception);
        }
    }

    private PreparedStatement query(Connection connection, String sql, Object... arguments) throws SQLException {
        PreparedStatement statement = connection.prepareStatement(sql);
        try {
            for (int index = 0; index < arguments.length; index++) {
                statement.setObject(index + 1, arguments[index]);
            }
            return statement;
        } catch (SQLException exception) {
            statement.close();
            throw exception;
        }
    }

    private void execute(Connection connection, String sql, Object... arguments) throws SQLException {
        try (PreparedStatement statement = query(connection, sql, arguments)) {
            statement.executeUpdate();
        }
    }

    private Map<String, Object> fields(Object... values) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int index = 0; index < values.length; index += 2) {
            result.put((String) values[index], values[index + 1]);
        }
        return result;
    }

    private void insert(Connection connection, String table, Map<String, Object> fields) throws SQLException {
        execute(connection, "INSERT INTO ultimatequest_" + table + " (" + String.join(", ", fields.keySet())
                + ") VALUES (" + String.join(", ", Collections.nCopies(fields.size(), "?")) + ")", fields.values().toArray());
    }

    private void upsert(Connection connection, String table, String key, Object id, Map<String, Object> fields) throws SQLException {
        List<Object> arguments = new ArrayList<>(fields.values());
        arguments.add(id);
        String assignments = String.join(", ", fields.keySet().stream().map(column -> column + " = ?").toList());
        try (PreparedStatement statement = query(connection,
                "UPDATE ultimatequest_" + table + " SET " + assignments + " WHERE " + key + " = ?", arguments.toArray())) {
            if (statement.executeUpdate() == 0) {
                Map<String, Object> inserted = new LinkedHashMap<>(fields);
                inserted.put(key, id);
                insert(connection, table, inserted);
            }
        }
    }

    private int fraction(BigDecimal value) {
        return value.subtract(BigDecimal.valueOf(value.longValue())).movePointRight(6).intValueExact();
    }

    private BigDecimal decimal(ResultSet row, String prefix) throws SQLException {
        int fraction = row.getInt(prefix + "_fraction");
        if (fraction < 0 || fraction >= 1_000_000) {
            throw new IllegalStateException("Invalid stored progress fraction");
        }
        return BigDecimal.valueOf(row.getLong(prefix + "_whole")).add(BigDecimal.valueOf(fraction, 6));
    }

    private Long nullableLong(ResultSet row, String field) throws SQLException {
        long value = row.getLong(field);
        return row.wasNull() ? null : value;
    }

    private Instant instant(ResultSet row, String field) throws SQLException {
        Long value = nullableLong(row, field);
        return value == null ? null : Instant.ofEpochMilli(value);
    }

    private TimeWindow window(ResultSet row) throws SQLException {
        return new TimeWindow(instant(row, "window_starts"), instant(row, "window_ends"), row.getLong("grace_seconds"), row.getInt("window_paused") != 0);
    }

    private Long millis(Instant value) {
        return value == null ? null : value.toEpochMilli();
    }

    @Override
    public void onClose() {
        if (dataSource != null) {
            dataSource.close();
            dataSource = null;
        }
        if (dialect != null) {
            dialect.closeDrivers();
            dialect = null;
        }
    }
}
