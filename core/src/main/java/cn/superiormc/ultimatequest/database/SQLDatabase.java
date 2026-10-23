package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.database.sql.DatabaseDialect;
import cn.superiormc.ultimatequest.database.sql.H2Dialect;
import cn.superiormc.ultimatequest.database.sql.MySQLDialect;
import cn.superiormc.ultimatequest.database.sql.PostgreSQLDialect;
import cn.superiormc.ultimatequest.database.sql.SQLiteDialect;
import cn.superiormc.ultimatequest.managers.ConfigManager;
import cn.superiormc.ultimatequest.objects.quests.QuestPlayerData;
import cn.superiormc.ultimatequest.utils.TextUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.ArrayList;
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
                statement.execute(dialect.createPlayerDataTable());
                statement.execute(dialect.createScrollDataTable());
            }
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fConnected to SQL database.");
        } catch (Exception exception) {
            onClose();
            throw new IllegalStateException("Could not initialize SQL database", exception);
        }
    }

    @Override
    public QuestPlayerData checkData(UUID playerId) {
        return checkData(playerId, false);
    }

    @Override
    public QuestPlayerData checkData(UUID playerId, boolean scroll) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement("SELECT player_name, revision, payload FROM "
                    + (scroll ? "ultimatequest_scroll_data" : "ultimatequest_player_data") + " WHERE player_uuid = ?")) {
            statement.setString(1, playerId.toString());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    return new QuestPlayerData(playerId, "", 0L, "schema-version: 4\n");
                }
                return new QuestPlayerData(playerId, result.getString(1), result.getLong(2), result.getString(3));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not load SQL player data: " + playerId, exception);
        }
    }

    @Override
    public void updateData(QuestPlayerData snapshot) {
        updateData(snapshot, false);
    }

    @Override
    public void updateData(QuestPlayerData snapshot, boolean scroll) {
        try (Connection connection = dataSource.getConnection();
            PreparedStatement statement = connection.prepareStatement(scroll ? dialect.upsertScrollData() : dialect.upsertPlayerData())) {
            statement.setString(1, snapshot.storageId());
            statement.setString(2, snapshot.playerName());
            statement.setLong(3, snapshot.revision());
            statement.setString(4, snapshot.payload());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not save SQL player data: " + snapshot.storageId(), exception);
        }
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

    @Override
    public List<QuestPlayerData> allPlayers() {
        List<QuestPlayerData> players = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT player_uuid, player_name, revision, payload FROM ultimatequest_player_data");
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                players.add(new QuestPlayerData(UUID.fromString(result.getString(1)), result.getString(2),
                        result.getLong(3), result.getString(4)));
            }
        } catch (SQLException exception) {
            throw new IllegalStateException("Could not enumerate SQL player data", exception);
        }
        return players;
    }
}
