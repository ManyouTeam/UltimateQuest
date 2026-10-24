package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.objects.quests.QuestDefinition;
import cn.superiormc.ultimatequest.utils.ConfigurationFileUtil;
import cn.superiormc.ultimatequest.objects.quests.QuestObjective;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.random.RandomGenerator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class QuestPoolManager extends AbstractManager {

    public static QuestPoolManager questPoolManager;

    public QuestPoolManager() {
        questPoolManager = this;
    }

    public synchronized Map<String, YamlConfiguration> objectives() {
        return ConfigManager.configManager.getQuestConfigurations();
    }

    public static void validateWeights(ConfigurationSection weights) {
        if (weights == null || weights.getKeys(false).isEmpty()) {
            throw new IllegalArgumentException("Weighted selection must not be empty");
        }
        long total = 0L;
        for (String key : weights.getKeys(false)) {
            if (!key.matches("[a-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid weighted selection id: " + key);
            }
            Object value = weights.get(key);
            if (!(value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long)
                    || ((Number) value).longValue() <= 0L) {
                throw new IllegalArgumentException("Expected a positive integer weight for " + key);
            }
            total = Math.addExact(total, ((Number) value).longValue());
        }
    }

    public static String choose(ConfigurationSection weights, RandomGenerator random) {
        validateWeights(weights);
        long total = weights.getKeys(false).stream().mapToLong(weights::getLong).sum();
        long value = random.nextLong(total);
        for (String key : weights.getKeys(false)) {
            value -= weights.getLong(key);
            if (value < 0L) {
                return key;
            }
        }
        throw new IllegalStateException("Weighted selection failed");
    }

    public synchronized List<String> draw(String pool, int count, RandomGenerator random) {
        return ConfigManager.configManager.getQuestPool(pool).draw(count, random);
    }

    public synchronized List<String> draw(ConfigurationSection source, RandomGenerator random) {
        return draw(source, ConfigManager.configManager.getPoolConfigurations(), objectives(), random);
    }

    public static List<String> draw(ConfigurationSection source, Map<String, YamlConfiguration> pools,
            Map<String, YamlConfiguration> objectives, RandomGenerator random) {
        String pool = source.getString("pool", "");
        ConfigurationSection config = pools.get(pool);
        if (config == null) {
            throw new IllegalArgumentException("Unknown objective pool: " + pool);
        }
        ConfigurationSection weights = config.getConfigurationSection("quests");
        validateWeights(weights);
        List<String> candidates = new ArrayList<>();
        for (String id : weights.getKeys(false)) {
            if (objectives.containsKey(id)) {
                candidates.add(id);
            }
        }
        if (!source.contains("categories")) {
            long count = QuestDefinition.positiveLong(source, "count", 1L);
            if (count > 45L) {
                throw new IllegalArgumentException("Too many objectives requested from pool: " + pool);
            }
            return drawCandidates(pool, weights, candidates, (int) count, random);
        }
        ConfigurationSection categories = source.getConfigurationSection("categories");
        if (categories == null || categories.getKeys(false).isEmpty() || source.contains("count")) {
            throw new IllegalArgumentException("Pool categories require a nonempty category/count section without count: " + pool);
        }
        Map<String, List<String>> objectiveCategories = new HashMap<>();
        for (String id : candidates) {
            objectiveCategories.put(id, QuestObjective.resolve(id, objectives.get(id), objectives).getStringList("categories"));
        }
        List<List<String>> slots = new ArrayList<>();
        for (String category : categories.getKeys(false)) {
            long count = QuestDefinition.positiveLong(categories, category, 0L);
            if (!category.matches("[a-z0-9_-]+") || count > 45L || slots.size() + count > 45L) {
                throw new IllegalArgumentException("Invalid category quota in pool " + pool + ": " + category);
            }
            List<String> eligible = candidates.stream().filter(id -> objectiveCategories.get(id).contains(category)).toList();
            if (eligible.size() < count) {
                throw new IllegalArgumentException("Not enough objectives in pool " + pool + " for category: " + category);
            }
            for (int index = 0; index < count; index++) {
                slots.add(drawCandidates(pool, weights, new ArrayList<>(eligible), eligible.size(), random));
            }
        }
        Map<String, Integer> assigned = new HashMap<>();
        for (int slot = 0; slot < slots.size(); slot++) {
            if (!assignCategory(slot, slots, assigned, new HashSet<>())) {
                throw new IllegalArgumentException("Not enough distinct objectives for category quotas in pool: " + pool);
            }
        }
        List<String> result = new ArrayList<>(Collections.nCopies(slots.size(), ""));
        assigned.forEach((id, slot) -> result.set(slot, id));
        return List.copyOf(result);
    }

    private static boolean assignCategory(int slot, List<List<String>> slots,
            Map<String, Integer> assigned, Set<String> visited) {
        for (String id : slots.get(slot)) {
            if (!visited.add(id)) {
                continue;
            }
            Integer previous = assigned.get(id);
            if (previous == null || assignCategory(previous, slots, assigned, visited)) {
                assigned.put(id, slot);
                return true;
            }
        }
        return false;
    }

    public static List<String> drawCandidates(String pool, ConfigurationSection weights,
            List<String> candidates, int count, RandomGenerator random) {
        if (candidates.size() < count) {
            throw new IllegalArgumentException("Not enough eligible quests in pool: " + pool);
        }
        List<String> result = new ArrayList<>();
        while (result.size() < count) {
            long total = candidates.stream().mapToLong(weights::getLong).sum();
            long value = random.nextLong(total);
            for (int index = 0; index < candidates.size(); index++) {
                String id = candidates.get(index);
                value -= weights.getLong(id);
                if (value < 0L) {
                    result.add(id);
                    candidates.remove(index);
                    break;
                }
            }
        }
        return List.copyOf(result);
    }
}
