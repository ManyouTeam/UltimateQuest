package cn.superiormc.ultimatequest.database.sql;

public final class SQLiteDialect extends DatabaseDialect {

    @Override
    public boolean matches(String jdbcUrl) {
        return jdbcUrl.startsWith("jdbc:sqlite:");
    }

    @Override
    public int maxPoolSize() {
        return 1;
    }

    @Override
    public int minIdle() {
        return 1;
    }

    @Override
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("sqlite-jdbc", "https://repo.maven.apache.org/maven2/org/xerial/sqlite-jdbc/3.46.1.3/sqlite-jdbc-3.46.1.3.jar", "org.sqlite.JDBC");
    }
}
