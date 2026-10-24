package cn.superiormc.ultimatequest.database.sql;

public final class H2Dialect extends DatabaseDialect {

    @Override
    protected String serverDataPayloadType() {
        return "CLOB";
    }

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:h2:");
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
                payload CLOB NOT NULL
            )
            """;
    }

    @Override
    public String upsertPlayerData() {
        return """
            MERGE INTO ultimatequest_player_data AS target
            USING (VALUES (?, ?, ?, ?)) AS source(player_uuid, player_name, revision, payload)
            ON target.player_uuid = source.player_uuid
            WHEN MATCHED AND source.revision >= target.revision THEN UPDATE SET
            player_name = source.player_name, revision = source.revision, payload = source.payload
            WHEN NOT MATCHED THEN INSERT (player_uuid, player_name, revision, payload)
            VALUES (source.player_uuid, source.player_name, source.revision, source.payload)
            """;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("h2", "https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar", "org.h2.Driver");
    }
}
