package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.random.RandomGenerator;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public final class QuestPoolManager extends AbstractManager {

    public static QuestPoolManager questPoolManager;

    private Map<String, YamlConfiguration> pools = Map.of();

    private Map<String, YamlConfiguration> objectives = Map.of();

    public QuestPoolManager() {
        questPoolManager = this;
    }

    public static Map<String, YamlConfiguration> readFolder(String name) throws Exception {
        return readFolder(name, false);
    }

    public static Map<String, YamlConfiguration> readFolder(String name, boolean includeDisabled) throws Exception {
        File directory = new File(UltimateQuest.instance.getDataFolder(), name);
        if (!directory.isDirectory() && !directory.mkdirs()) {
            throw new IllegalStateException("Could not create configuration directory: " + name);
        }
        File[] files = directory.listFiles((ignored, file) -> file.endsWith(".yml"));
        if (files == null) {
            throw new IllegalStateException("Could not list configuration directory: " + name);
        }
        Arrays.sort(files, Comparator.comparing(File::getName));
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        for (File file : files) {
            String id = file.getName().substring(0, file.getName().length() - 4).toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid configuration id: " + id);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            if ((includeDisabled || config.getBoolean("enabled", true)) && result.putIfAbsent(id, config) != null) {
                throw new IllegalArgumentException("Duplicate configuration id: " + id);
            }
        }
        return result;
    }

    public Map<String, YamlConfiguration> load(Map<String, YamlConfiguration> definitions) throws Exception {
        Map<String, YamlConfiguration> loaded = readFolder("pools");
        for (Map.Entry<String, YamlConfiguration> entry : loaded.entrySet()) {
            ConfigurationSection quests = entry.getValue().getConfigurationSection("quests");
            validateWeights(quests);
            for (String id : quests.getKeys(false)) {
                if (!definitions.containsKey(id)) {
                    throw new IllegalArgumentException("Unknown objective in pool " + entry.getKey() + ": " + id);
                }
            }
        }
        return loaded;
    }

    public synchronized void install(Map<String, YamlConfiguration> loaded,
            Map<String, YamlConfiguration> loadedObjectives) {
        pools = Map.copyOf(loaded);
        objectives = Map.copyOf(loadedObjectives);
    }

    public synchronized Map<String, YamlConfiguration> objectives() {
        return objectives;
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
        YamlConfiguration config = pools.get(pool);
        if (config == null || count < 1 || count > 45) {
            throw new IllegalArgumentException("Unknown pool or invalid draw count: " + pool);
        }
        ConfigurationSection weights = config.getConfigurationSection("quests");
        List<String> candidates = new ArrayList<>();
        for (String id : weights.getKeys(false)) {
            if (objectives.containsKey(id)) {
                candidates.add(id);
            }
        }
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
