package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.MemoryConfiguration;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

public class ConfigManager extends AbstractManager {

    public static ConfigManager configManager;

    public FileConfiguration config;

    private volatile YamlConfiguration menuConfig = new YamlConfiguration();

    private volatile List<String> menuIds = List.of();

    private static final List<String> MENU_FILES = List.of("main", "battlepass", "milestone", "period", "scroll");

    public ConfigManager() {
        configManager = this;
        UltimateQuest.instance.reloadConfig();
        config = UltimateQuest.instance.getConfig();
        config.options().copyDefaults(true);
        loadMenus();
    }

    public boolean getBoolean(String path) {
        return configuration(path).getBoolean(normalizePath(path), false);
    }

    public boolean getBoolean(String path, boolean defaultValue) {
        return configuration(path).getBoolean(normalizePath(path), defaultValue);
    }

    public String getString(String path, String... args) {
        String s = configuration(path).getString(normalizePath(path));
        if (s == null) {
            if (args.length == 0) {
                return null;
            }
            s = args[0];
        }
        for (int i = 1; i < args.length; i += 2) {
            String var = "{" + args[i] + "}";
            if (args[i + 1] == null) {
                s = s.replace(var, "");
            } else {
                s = s.replace(var, args[i + 1]);
            }
        }
        return s.replace("{plugin_folder}", String.valueOf(UltimateQuest.instance.getDataFolder()));
    }

    public int getInt(String path, int defaultValue) {
        return configuration(path).getInt(normalizePath(path), defaultValue);
    }

    public double getDouble(String path, double defaultValue) {
        return configuration(path).getDouble(normalizePath(path), defaultValue);
    }

    public ConfigurationSection getSection(String path) {
        FileConfiguration selected = configuration(path);
        String normalized = normalizePath(path);
        if (selected.getConfigurationSection(normalized) == null) {
            return new MemoryConfiguration();
        }
        return selected.getConfigurationSection(normalized);
    }

    public ConfigurationSection getMenu(String id) {
        if (!hasMenu(id)) {
            return new MemoryConfiguration();
        }
        ConfigurationSection section = menuConfig.getConfigurationSection(id.toLowerCase(Locale.ROOT));
        return section == null ? new MemoryConfiguration() : section;
    }

    public boolean hasMenu(String id) {
        return id != null && menuIds.contains(id.toLowerCase(Locale.ROOT));
    }

    public Collection<String> getMenuIds() {
        return menuIds;
    }

    private void loadMenus() {
        try {
            reloadMenus();
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load menu configurations: " + exception.getMessage());
        }
    }

    public synchronized void reloadMenus() throws Exception {
        File directory = new File(UltimateQuest.instance.getDataFolder(), "menus");
        if (!directory.exists() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create menu configuration directory: " + directory);
        }
        YamlConfiguration candidate = new YamlConfiguration();
        loadMenuFile(directory, "settings", null, candidate);
        Set<String> ids = new LinkedHashSet<>(MENU_FILES);
        File[] files = directory.listFiles((ignored, name) -> name.toLowerCase(Locale.ROOT).endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
                if (!id.equals("settings")) {
                    ids.add(id);
                }
            }
        }
        for (String id : ids) {
            loadMenuFile(directory, id, id, candidate);
            ConfigurationSection menu = candidate.getConfigurationSection(id);
            for (String layout : List.of("layout", "catalog-layout", "detail-layout", "challenge-layout", "category-filter.layout")) {
                if (menu.contains(layout)) {
                    List<String> rows = menu.getStringList(layout);
                    if (rows.isEmpty() || rows.size() > 6 || rows.stream().anyMatch(row -> row.length() != 9)) {
                        throw new IllegalArgumentException("Invalid menu layout: " + id + "." + layout);
                    }
                    if (layout.equals("category-filter.layout") && rows.stream().noneMatch(row -> row.contains("Q"))) {
                        throw new IllegalArgumentException("Category layout requires Q slots: " + id);
                    }
                }
            }
        }
        menuConfig = candidate;
        menuIds = List.copyOf(ids);
    }

    private void loadMenuFile(File directory, String fileName, String sectionName, YamlConfiguration candidate) throws Exception {
        String resource = "menus/" + fileName + ".yml";
        YamlConfiguration loaded = InitManager.loadConfiguration(new File(directory, fileName + ".yml"), resource);
        ConfigurationSection target = sectionName == null ? candidate : candidate.createSection(sectionName);
        copySection(loaded, target);
    }

    private static void copySection(ConfigurationSection source, ConfigurationSection target) {
        for (String key : source.getKeys(false)) {
            ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) {
                target.set(key, source.get(key));
            } else {
                copySection(child, target.createSection(key));
            }
        }
    }

    private FileConfiguration configuration(String path) {
        return path != null && path.startsWith("menu.") ? menuConfig : config;
    }

    private static String normalizePath(String path) {
        return path != null && path.startsWith("menu.") ? path.substring("menu.".length()) : path;
    }
}
