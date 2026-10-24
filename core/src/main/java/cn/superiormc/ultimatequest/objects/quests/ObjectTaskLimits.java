package cn.superiormc.ultimatequest.objects.quests;

import cn.superiormc.ultimatequest.managers.ConfigManager;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.bukkit.configuration.ConfigurationSection;

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

    public static void validateTaskLimits(ConfigurationSection config, Set<String> categories) {
        for (String key : config.getKeys(false)) {
            if (!TASK_LIMIT_TYPES.contains(key) && !key.equals("categories")) {
                throw new IllegalArgumentException("Unknown task-limits section: " + key);
            }
            if (!config.isConfigurationSection(key)) {
                throw new IllegalArgumentException("task-limits." + key + " must be a section");
            }
        }
        for (String type : TASK_LIMIT_TYPES) {
            ConfigurationSection scoped = config.getConfigurationSection(type);
            if (scoped != null) {
                for (String key : scoped.getKeys(false)) {
                    if (!Set.of("total", "categories").contains(key)) {
                        throw new IllegalArgumentException("Unknown task-limits." + type + " option: " + key);
                    }
                }
            }
            if (config.contains(type + ".categories") && !config.isConfigurationSection(type + ".categories")) {
                throw new IllegalArgumentException("task-limits." + type + ".categories must be a section");
            }
            if (config.contains(type + ".total")) {
                taskLimit(config, type + ".total");
            }
            validateLimitCategories(config.getConfigurationSection(type + ".categories"), categories, "task-limits." + type + ".categories");
        }
        validateLimitCategories(config.getConfigurationSection("categories"), categories, "task-limits.categories");
    }

    public boolean allowsTasks(ConfigurationSection config, Collection<QuestInstance> active, String source, List<QuestDefinition> incoming) {
        String type = source.equals("challenge") ? "battlepass-task" : source;
        if (!TASK_LIMIT_TYPES.contains(type)) {
            throw new IllegalArgumentException("Unknown task type: " + type);
        }
        long total = active.stream().filter(quest -> quest.source().equals(source)).count() + incoming.size();
        long maximum = taskLimit(config, type + ".total");
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
            long global = taskLimit(config, "categories." + id);
            long scoped = taskLimit(config, type + ".categories." + id);
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

    private static Set<String> limitCategories(List<String> categories) {
        return categories.isEmpty() ? Set.of("uncategorized") : Set.copyOf(categories);
    }

}
