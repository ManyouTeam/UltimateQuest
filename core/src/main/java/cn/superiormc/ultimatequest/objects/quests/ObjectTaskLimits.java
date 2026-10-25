package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public final class ObjectTaskLimits {

    private static final Set<String> TASK_LIMIT_TYPES = Set.of("normal", "scroll", "period", "battlepass-task");

    private final ConfigurationSection config;

    public ObjectTaskLimits(ConfigurationSection config, Set<String> categories) {
        this.config = config;
        validateTaskLimits(config, categories);
    }

    public boolean allows(Collection<QuestInstance> active, String source, List<QuestDefinition> incoming) {
        return allowsTasks(config, active, source, incoming);
    }

    public boolean allows(Player player, Collection<QuestInstance> active, String source, List<QuestDefinition> incoming) {
        return allowsTasks(player, config, active, source, incoming);
    }

    public static void validateTaskLimits(ConfigurationSection config, Set<String> categories) {
        validateLimitSettings(config, categories, false);
        ConfigurationSection permissions = config.getConfigurationSection("permission-limits");
        if (permissions == null) {
            return;
        }
        for (String id : permissions.getKeys(false)) {
            ConfigurationSection limits = permissions.getConfigurationSection(id);
            if (limits == null) {
                throw new IllegalArgumentException("task-limits.permission-limits." + id + " must be a section");
            }
            Object permission = limits.get("permission");
            if (!(permission instanceof String node) || node.isBlank()) {
                throw new IllegalArgumentException(limits.getCurrentPath() + ".permission must be a non-empty string");
            }
            validateLimitSettings(limits, categories, true);
        }
    }

    private static void validateLimitSettings(ConfigurationSection config, Set<String> categories, boolean permissionProfile) {
        String path = config.getCurrentPath();
        for (String key : config.getKeys(false)) {
            if (permissionProfile && key.equals("permission")) {
                continue;
            }
            if (!TASK_LIMIT_TYPES.contains(key) && !key.equals("categories")
                    && !(key.equals("permission-limits") && !permissionProfile)) {
                throw new IllegalArgumentException("Unknown " + path + " section: " + key);
            }
            if (!config.isConfigurationSection(key)) {
                throw new IllegalArgumentException(path + "." + key + " must be a section");
            }
        }
        for (String type : TASK_LIMIT_TYPES) {
            ConfigurationSection scoped = config.getConfigurationSection(type);
            if (scoped != null) {
                for (String key : scoped.getKeys(false)) {
                    if (!Set.of("total", "categories").contains(key)) {
                        throw new IllegalArgumentException("Unknown " + path + "." + type + " option: " + key);
                    }
                }
            }
            if (config.contains(type + ".categories") && !config.isConfigurationSection(type + ".categories")) {
                throw new IllegalArgumentException(path + "." + type + ".categories must be a section");
            }
            if (config.contains(type + ".total")) {
                taskLimit(config, type + ".total");
            }
            validateLimitCategories(config.getConfigurationSection(type + ".categories"), categories, path + "." + type + ".categories");
        }
        validateLimitCategories(config.getConfigurationSection("categories"), categories, path + ".categories");
    }

    public boolean allowsTasks(ConfigurationSection config, Collection<QuestInstance> active, String source, List<QuestDefinition> incoming) {
        return allowsTasks(null, config, active, source, incoming);
    }

    private boolean allowsTasks(Player player, ConfigurationSection config, Collection<QuestInstance> active, String source, List<QuestDefinition> incoming) {
        String type = source.equals("challenge") ? "battlepass-task" : source;
        if (!TASK_LIMIT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown task type: " + type);
        }
        List<ConfigurationSection> permissionLimits = new ArrayList<>();
        ConfigurationSection permissions = config.getConfigurationSection("permission-limits");
        if (player != null && permissions != null) {
            for (String id : permissions.getKeys(false)) {
                ConfigurationSection limits = permissions.getConfigurationSection(id);
                if (player.hasPermission(limits.getString("permission").strip())) {
                    permissionLimits.add(limits);
                }
            }
        }
        long total = active.stream().filter(quest -> quest.source().equals(source)).count() + incoming.size();
        long maximum = taskLimit(config, type + ".total", permissionLimits);
        if (maximum >= 0L && total > maximum) {
            return false;
        }
        Map<String, Long> globalCounts = new HashMap<>();
        Map<String, Long> typeCounts = new HashMap<>();
        for (QuestInstance quest : active) {
            for (String id : limitCategories(ConfigManager.configManager.getDisplayCategories(quest.definition(), quest))) {
                globalCounts.merge(id, 1L, Long::sum);
                if (quest.source().equals(source)) {
                    typeCounts.merge(id, 1L, Long::sum);
                }
            }
        }
        Set<String> affected = new HashSet<>();
        for (QuestDefinition definition : incoming) {
            for (String id : limitCategories(definition.categories())) {
                globalCounts.merge(id, 1L, Long::sum);
                typeCounts.merge(id, 1L, Long::sum);
                affected.add(id);
            }
        }
        for (String id : affected) {
            long global = taskLimit(config, "categories." + id, permissionLimits);
            long scoped = taskLimit(config, type + ".categories." + id, permissionLimits);
            if (global >= 0L && globalCounts.getOrDefault(id, 0L) > global || scoped >= 0L && typeCounts.getOrDefault(id, 0L) > scoped) {
                return false;
            }
        }
        return true;
    }

    private static void validateLimitCategories(ConfigurationSection section, Set<String> categories, String path) {
        if (section == null) {
            return;
        }
        for (String id : section.getKeys(false)) {
            if (!categories.contains(id) && !id.equals("uncategorized")) {
                throw new IllegalArgumentException(path + ": unknown category " + id);
            }
            taskLimit(section, id);
        }
    }

    private static long taskLimit(ConfigurationSection section, String path) {
        Object value = section.get(path, -1);
        if (!(value instanceof Integer || value instanceof Long) || ((Number) value).longValue() < -1L) {
            throw new IllegalArgumentException(section.getCurrentPath() + "." + path + " must be an integer >= -1");
        }
        return ((Number) value).longValue();
    }

    private static long taskLimit(ConfigurationSection section, String path, List<ConfigurationSection> permissionLimits) {
        long maximum = taskLimit(section, path);
        for (ConfigurationSection limits : permissionLimits) {
            if (maximum == -1L) {
                return -1L;
            }
            if (limits.contains(path)) {
                long permitted = taskLimit(limits, path);
                if (permitted == -1L) {
                    return -1L;
                }
                maximum = Math.max(maximum, permitted);
            }
        }
        return maximum;
    }

    private static Set<String> limitCategories(List<String> categories) {
        return categories.isEmpty() ? Set.of("uncategorized") : Set.copyOf(categories);
    }

}
