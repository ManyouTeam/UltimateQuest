package cn.superiormc.ultimatequest.editor;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public enum EditorScope {
    QUEST("quests", "Objectives", "objective"),
    TASK("tasks", "Tasks", "normal"),
    CATEGORY("categories", "Categories", "category"),
    BATTLEPASS("battlepasses", "Battle Passes", "battlepass"),
    MILESTONE("milestones", "Milestones", "milestone"),
    PERIOD("periods", "Periods", "period"),
    SCROLL("scrolls", "Scrolls", "scroll"),
    POOL("pools", "Pools", "pool"),
    REWARD_POOL("reward-pools", "Reward Pools", "reward-pool"),
    MENU("menus", "Menus", "menu");

    private final String folderName;
    private final String displayName;
    private final String type;

    EditorScope(String folderName, String displayName, String type) {
        this.folderName = folderName;
        this.displayName = displayName;
        this.type = type;
    }

    public String getFolderName() {
        return folderName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getType() {
        return type;
    }

    public File getFolder() {
        return new File(UltimateQuest.instance.getDataFolder(), folderName);
    }

    public File getFile(String id) {
        if (id == null || !id.matches("[a-z0-9_-]+")) {
            throw new IllegalArgumentException("Invalid editor file ID");
        }
        Path folder = getFolder().toPath().toAbsolutePath().normalize();
        Path file = folder.resolve(id + ".yml");
        try {
            Path root = UltimateQuest.instance.getDataFolder().toPath().toRealPath();
            if (Files.isSymbolicLink(file) || !folder.toRealPath().startsWith(root)) {
                throw new IllegalArgumentException("Editor target is outside the plugin folder");
            }
        } catch (IOException exception) {
            throw new IllegalArgumentException("Editor folder is unavailable", exception);
        }
        return file.toFile();
    }

    public List<String> listIds() {
        File[] files = getFolder().listFiles((folder, name) -> name.matches("[a-z0-9_-]+\\.yml"));
        if (files == null) {
            return List.of();
        }
        return Arrays.stream(files).filter(File::isFile)
                .filter(file -> !Files.isSymbolicLink(file.toPath()))
                .map(file -> file.getName().substring(0, file.getName().length() - 4)).sorted().toList();
    }

    public static EditorScope ofName(String name) {
        if (name == null) {
            return null;
        }
        String value = name.toLowerCase(Locale.ROOT);
        return Arrays.stream(values()).filter(scope -> scope.name().equalsIgnoreCase(value)
                || scope.folderName.equals(value) || scope.type.equals(value)).findFirst().orElse(null);
    }
}
