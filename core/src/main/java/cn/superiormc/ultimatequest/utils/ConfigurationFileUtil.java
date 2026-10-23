package cn.superiormc.ultimatequest.utils;

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
}
