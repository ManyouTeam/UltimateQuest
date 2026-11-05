package cn.superiormc.ultimatequest.database.sql;

public final class H2Dialect extends DatabaseDialect {

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
    public void needExtraDownload(String jdbcUrl) {
        loadDriver("h2", "https://repo.maven.apache.org/maven2/com/h2database/h2/2.2.220/h2-2.2.220.jar", "org.h2.Driver");
    }
}
