package cn.superiormc.ultimatequest.database.sql;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.database.DriverShim;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Driver;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public abstract class DatabaseDialect {

    private final List<Driver> registeredDrivers = new ArrayList<>();

    private final List<URLClassLoader> driverClassLoaders = new ArrayList<>();

    public abstract boolean matches(String jdbcUrl);

    public abstract int maxPoolSize();

    public abstract int minIdle();

    public abstract String createPlayerDataTable();

    public abstract String upsertPlayerData();

    public String createServerDataTable() {
        return "CREATE TABLE IF NOT EXISTS ultimatequest_server_data (data_key VARCHAR(64) NOT NULL PRIMARY KEY, "
                + "revision BIGINT NOT NULL, payload " + serverDataPayloadType() + " NOT NULL)";
    }

    protected String serverDataPayloadType() {
        return "TEXT";
    }

    public String createScrollDataTable() {
        return createPlayerDataTable().replace("ultimatequest_player_data", "ultimatequest_scroll_data");
    }

    public String upsertScrollData() {
        return upsertPlayerData().replace("ultimatequest_player_data", "ultimatequest_scroll_data");
    }

    public abstract void needExtraDownload(String jdbcUrl);

    public void loadDriver(String driverName, String mavenUrl, String driverClassName) {
        try {
            try {
                Class.forName(driverClassName);
                return;
            } catch (ClassNotFoundException ignored) {
                // Load the driver through the same isolated loader used by UltimateShop.
            }
            Path libPath = Paths.get(UltimateQuest.instance.getDataFolder().getPath(), "libs");
            if (!Files.exists(libPath)) {
                Files.createDirectories(libPath);
            }
            String jarName = driverName + ".jar";
            Path jarPath = libPath.resolve(jarName);
            // 如果本地不存在，则下载
            if (!Files.exists(jarPath)) {
                TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §fDownloading " + jarName + " ...");
                try (InputStream in = new URL(mavenUrl).openStream()) {
                    Files.copy(in, jarPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            URL jarUrl = jarPath.toUri().toURL();
            URLClassLoader loader = new URLClassLoader(new URL[] { jarUrl }, UltimateQuest.instance.getClass().getClassLoader());
            driverClassLoaders.add(loader);
            // Register the isolated JDBC driver.
            Class<?> driverClass = Class.forName(driverClassName, true, loader);
            Driver driverInstance = (Driver) driverClass.getDeclaredConstructor().newInstance();
            Driver registeredDriver = new DriverShim(driverInstance);
            DriverManager.registerDriver(registeredDriver);
            registeredDrivers.add(registeredDriver);
            TextUtil.sendMessage(null, TextUtil.pluginPrefix() + " §f" + driverName + " loaded!");
        } catch (Throwable e) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load JDBC driver " + driverName + ": " + e.getMessage());
            throw new IllegalStateException("Could not load JDBC driver " + driverName, e);
        }
    }

    public void closeDrivers() {
        for (Driver driver : registeredDrivers) {
            try {
                DriverManager.deregisterDriver(driver);
            } catch (Throwable ignored) {
            }

        }
        registeredDrivers.clear();
        for (URLClassLoader loader : driverClassLoaders) {
            try {
                loader.close();
            } catch (Throwable ignored) {
            }

        }
        driverClassLoaders.clear();
    }
}
