package cn.superiormc.ultimatequest.database.sql;

public final class MySQLDialect extends DatabaseDialect {

    @Override
    protected String serverDataPayloadType() {
        return "LONGTEXT";
    }

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:mariadb:");
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
                payload LONGTEXT NOT NULL
            )
            """;
    }

    @Override
    public String upsertPlayerData() {
        return """
            INSERT INTO ultimatequest_player_data (player_uuid, player_name, revision, payload)
            VALUES (?, ?, ?, ?)
            ON DUPLICATE KEY UPDATE
            player_name = IF(VALUES(revision) >= revision, VALUES(player_name), player_name),
            payload = IF(VALUES(revision) >= revision, VALUES(payload), payload),
            revision = GREATEST(revision, VALUES(revision))
            """;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mariadb:")) {
            loadDriver("mariadb-java-client", "https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar", "org.mariadb.jdbc.Driver");
            return;
        }
        loadDriver("mysql-connector-j", "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar", "com.mysql.cj.jdbc.Driver");
    }
}
