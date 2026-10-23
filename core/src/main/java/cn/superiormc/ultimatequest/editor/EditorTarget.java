package cn.superiormc.ultimatequest.editor;

import cn.superiormc.ultimatequest.managers.ErrorManager;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;

public class EditorTarget {

    private final EditorScope scope;

    private final String id;

    private final File file;

    private final YamlConfiguration config;

    private byte[] original;

    private String baseline;

    private EditorTarget(EditorScope scope, String id, File file, YamlConfiguration config, byte[] original) {
        this.scope = scope;
        this.id = id;
        this.file = file;
        this.config = config;
        this.original = original.clone();
        baseline = config.saveToString();
    }

    public EditorScope getScope() {
        return scope;
    }

    public String getId() {
        return id;
    }

    public File getFile() {
        return file;
    }

    public YamlConfiguration getConfig() {
        return config;
    }

    public byte[] getOriginal() {
        return original.clone();
    }

    public boolean dirty() {
        return !baseline.equals(config.saveToString());
    }

    public void markSaved(byte[] serialized) {
        original = serialized.clone();
        baseline = config.saveToString();
    }

    public static EditorTarget load(EditorScope scope, String id) {
        if (scope == null || id == null || !id.matches("[a-z0-9_-]+")) {
            return null;
        }
        try {
            File file = scope.getFile(id);
            if (!file.isFile()) {
                return null;
            }
            byte[] original = Files.readAllBytes(file.toPath());
            YamlConfiguration config = new YamlConfiguration();
            config.loadFromString(new String(original, StandardCharsets.UTF_8));
            return new EditorTarget(scope, id, file, config, original);
        } catch (Exception exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not load editor target " + id + ": " + exception.getMessage());
            return null;
        }
    }
}
