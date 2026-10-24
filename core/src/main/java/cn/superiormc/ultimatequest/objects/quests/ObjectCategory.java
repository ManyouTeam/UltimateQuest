package cn.superiormc.ultimatequest.objects.quests;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ObjectCategory {

    private final String id;

    private final YamlConfiguration config;

    public ObjectCategory(String id, YamlConfiguration config) {
        this.id = id;
        this.config = config;
        if (Set.of("all", "uncategorized").contains(id)) {
            throw new IllegalArgumentException("Reserved category ID: " + id);
        }
        Object order = config.get("order", 0);
        if (!(order instanceof Integer || order instanceof Long)) {
            throw new IllegalArgumentException("categories/" + id + ".yml: order must be an integer");
        }
    }

    public static void validateCategories(ConfigurationSection section, Map<String, YamlConfiguration> categories, String location) {
        if (section == null || !section.contains("categories")) {
            return;
        }
        Object configured = section.get("categories");
        if (!(configured instanceof List<?> values)) {
            throw new IllegalArgumentException(location + ": categories must be a list");
        }
        for (Object value : values) {
            if (!(value instanceof String id) || !categories.containsKey(id)) {
                throw new IllegalArgumentException(location + ": unknown categories entry: " + value);
            }
        }
    }

    public String getId() {
        return id;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public boolean isEnabled() {
        return config.getBoolean("enabled", true);
    }

}
