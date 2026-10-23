package cn.superiormc.ultimatequest.database.sql;

public final class PostgreSQLDialect extends DatabaseDialect {

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:postgresql:");
    }

    @Override
    public int maxPoolSize() {
        return 4;
    }

    @Override
    public int minIdle() {
        return 1;
    }

    @Override
    public String createPlayerDataTable() {
        return """
            CREATE TABLE IF NOT EXISTS ultimatequest_player_data (
                player_uuid VARCHAR(36) NOT NULL PRIMARY KEY,
                player_name VARCHAR(32) NOT NULL,
                revision BIGINT NOT NULL,
                payload TEXT NOT NULL
            )
            """;
    }

    @Override
    public String upsertPlayerData() {
        return """
            INSERT INTO ultimatequest_player_data (player_uuid, player_name, revision, payload)
            VALUES (?, ?, ?, ?)
            ON CONFLICT(player_uuid) DO UPDATE SET player_name = excluded.player_name,
            revision = excluded.revision, payload = excluded.payload
            WHERE excluded.revision >= ultimatequest_player_data.revision
            """;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("postgresql", "https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar", "org.postgresql.Driver");
    }
}
