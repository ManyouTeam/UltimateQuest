package cn.superiormc.ultimatequest.database.sql;

import java.util.List;

public final class MySQLDialect extends DatabaseDialect {

    @Override
    public List<String> createTables() {
        return super.createTables().stream().map(sql -> sql + " ENGINE=InnoDB").toList();
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
    public void needExtraDownload(String jdbcUrl) {
        if (jdbcUrl.startsWith("jdbc:mariadb:")) {
            loadDriver("mariadb-java-client", "https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.1.4/mariadb-java-client-3.1.4.jar", "org.mariadb.jdbc.Driver");
            return;
        }
        loadDriver("mysql-connector-j", "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/8.4.0/mysql-connector-j-8.4.0.jar", "com.mysql.cj.jdbc.Driver");
    }
}
