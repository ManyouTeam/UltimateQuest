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
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("postgresql", "https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.6.0/postgresql-42.6.0.jar", "org.postgresql.Driver");
    }
}
