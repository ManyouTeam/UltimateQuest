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

    public List<String> createTables() {
        return List.of(
                "CREATE TABLE IF NOT EXISTS ultimatequest_players (player_uuid VARCHAR(36) NOT NULL PRIMARY KEY, player_name VARCHAR(32) NOT NULL, storage_version BIGINT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS ultimatequest_instances (instance_id VARCHAR(36) NOT NULL PRIMARY KEY, player_uuid VARCHAR(36), quest_key VARCHAR(512) NOT NULL, template_id VARCHAR(256) NOT NULL, source VARCHAR(16) NOT NULL, source_id VARCHAR(128) NOT NULL, cycle VARCHAR(128) NOT NULL, season VARCHAR(128) NOT NULL, accepted_at BIGINT NOT NULL, expires_at BIGINT NOT NULL, window_starts BIGINT, window_ends BIGINT, grace_seconds BIGINT NOT NULL, window_paused INTEGER NOT NULL, state VARCHAR(16) NOT NULL, total_whole BIGINT NOT NULL, total_fraction INTEGER NOT NULL, premium INTEGER NOT NULL, booster DOUBLE PRECISION NOT NULL, booster_until BIGINT NOT NULL, reward_selected INTEGER NOT NULL, scroll_token VARCHAR(36) UNIQUE, UNIQUE(player_uuid, quest_key))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_objectives (instance_id VARCHAR(36) NOT NULL, objective_id VARCHAR(128) NOT NULL, progress_whole BIGINT NOT NULL, progress_fraction INTEGER NOT NULL, PRIMARY KEY(instance_id, objective_id))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_stages (instance_id VARCHAR(36) NOT NULL, track VARCHAR(16) NOT NULL, stage_level INTEGER NOT NULL, state VARCHAR(16) NOT NULL, required BIGINT NOT NULL, PRIMARY KEY(instance_id, track, stage_level))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_reward_choices (instance_id VARCHAR(36) NOT NULL, scope VARCHAR(16) NOT NULL, track VARCHAR(16) NOT NULL, stage_level INTEGER NOT NULL, position INTEGER NOT NULL, reward_key VARCHAR(256) NOT NULL, PRIMARY KEY(instance_id, scope, track, stage_level, position))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_period_rolls (player_uuid VARCHAR(36) NOT NULL, group_id VARCHAR(128) NOT NULL, cycle VARCHAR(128) NOT NULL, PRIMARY KEY(player_uuid, group_id))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_scrolls (token VARCHAR(36) NOT NULL PRIMARY KEY, profile VARCHAR(128) NOT NULL, rarity VARCHAR(128) NOT NULL, difficulty VARCHAR(128) NOT NULL, duration_seconds BIGINT NOT NULL, owner_uuid VARCHAR(36), state VARCHAR(16) NOT NULL, storage_version BIGINT NOT NULL, instance_id VARCHAR(36) UNIQUE)",
                "CREATE TABLE IF NOT EXISTS ultimatequest_scroll_objectives (token VARCHAR(36) NOT NULL, position INTEGER NOT NULL, objective_id VARCHAR(128) NOT NULL, PRIMARY KEY(token, position))",
                "CREATE TABLE IF NOT EXISTS ultimatequest_seasons (catalog_key VARCHAR(256) NOT NULL PRIMARY KEY, storage_version BIGINT NOT NULL, season VARCHAR(128) NOT NULL, cycle_start BIGINT, paused INTEGER, rotation_disabled INTEGER NOT NULL, window_override INTEGER NOT NULL, window_starts BIGINT, window_ends BIGINT, grace_seconds BIGINT NOT NULL, window_paused INTEGER NOT NULL, booster DOUBLE PRECISION NOT NULL, booster_until BIGINT NOT NULL)",
                "CREATE TABLE IF NOT EXISTS ultimatequest_season_history (catalog_key VARCHAR(256) NOT NULL, season VARCHAR(128) NOT NULL, window_starts BIGINT, window_ends BIGINT, grace_seconds BIGINT NOT NULL, window_paused INTEGER NOT NULL, PRIMARY KEY(catalog_key, season))");
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
