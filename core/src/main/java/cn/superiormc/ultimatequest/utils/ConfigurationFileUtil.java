package cn.superiormc.ultimatequest.utils;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.nio.file.LinkOption;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.bukkit.configuration.file.YamlConfiguration;

public final class ConfigurationFileUtil {

    private ConfigurationFileUtil() {
    }

    public static Map<String, YamlConfiguration> readFolder(String name) throws Exception {
        return readFolder(name, false);
    }

    public static Map<String, YamlConfiguration> readFolder(String name, boolean includeDisabled) throws Exception {
        Map<String, YamlConfiguration> result = new LinkedHashMap<>();
        Map<String, File> locations = new HashMap<>();
        Path directory = new File(UltimateQuest.instance.getDataFolder(), name).toPath();
        Files.createDirectories(directory);
        for (Path path : findDefinitionFiles(directory)) {
            if (!name.equals("quests") && !path.getParent().equals(directory)) {
                continue;
            }
            File file = path.toFile();
            String id = file.getName().substring(0, file.getName().lastIndexOf('.')).toLowerCase(Locale.ROOT);
            if (!id.matches("[a-z0-9_-]+")) {
                throw new IllegalArgumentException("Invalid configuration id: " + id);
            }
            YamlConfiguration config = new YamlConfiguration();
            config.load(file);
            if (includeDisabled || config.getBoolean("enabled", true)) {
                if (result.putIfAbsent(id, config) != null) {
                    throw new IllegalArgumentException("Duplicate configuration id " + id + ": "
                            + locations.get(id) + " and " + file);
                }
                locations.put(id, file);
            }
        }
        return result;
    }

    public static List<Path> findDefinitionFiles(Path sourceDirectory) throws IOException {
        try (Stream<Path> paths = Files.walk(sourceDirectory)) {
            return paths.filter(path -> Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS))
                    .filter(path -> {
                        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
                        return name.endsWith(".yml") || name.endsWith(".yaml");
                    })
                    .sorted(Comparator.comparing(path -> sourceDirectory.relativize(path).toString()))
                    .toList();
        }
    }

    public static void save(Path target, YamlConfiguration config, boolean backup) throws IOException {
        write(target, config.saveToString().getBytes(StandardCharsets.UTF_8), backup);
    }

    public static void write(Path target, byte[] content, boolean backup) throws IOException {
        Path absolute = target.toAbsolutePath().normalize();
        Files.createDirectories(absolute.getParent());
        Path temporary = Files.createTempFile(absolute.getParent(), absolute.getFileName().toString(), ".tmp");
        try {
            Files.write(temporary, content);
            if (backup && Files.isRegularFile(absolute)) {
                Files.copy(absolute, absolute.resolveSibling(absolute.getFileName() + ".bak"), StandardCopyOption.REPLACE_EXISTING);
            }
            try {
                Files.move(temporary, absolute, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException exception) {
                Files.move(temporary, absolute, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public static void copy(org.bukkit.configuration.ConfigurationSection source, org.bukkit.configuration.ConfigurationSection target) {
        if (source == null) {
            return;
        }
        for (String key : source.getKeys(false)) {
            org.bukkit.configuration.ConfigurationSection child = source.getConfigurationSection(key);
            if (child == null) {
                target.set(key, source.get(key));
            } else {
                copy(child, target.createSection(key));
            }
        }
    }
}
