package cn.superiormc.ultimatequest.editor;

import org.bukkit.configuration.ConfigurationSection;

import java.util.List;
import java.util.Locale;

public class EditorTypeResolver {

    public static EditorValueKind resolve(ConfigurationSection parent, String key) {
        Object value = parent.get(key);
        String path = buildPath(parent.getCurrentPath(), key);

        if (value instanceof ConfigurationSection section) {
            if (isItemSection(path, section)) {
                return EditorValueKind.ITEM_SECTION;
            }
            return EditorValueKind.SECTION;
        }

        if (value instanceof Boolean) {
            return EditorValueKind.BOOLEAN;
        }
        if (value instanceof Byte || value instanceof Short || value instanceof Integer || value instanceof Long) {
            return EditorValueKind.INTEGER;
        }
        if (value instanceof Float || value instanceof Double) {
            return EditorValueKind.DOUBLE;
        }
        if (value instanceof List<?> list) {
            if (list.isEmpty()) {
                return EditorValueKind.STRING_LIST;
            }
            boolean allNumbers = true;
            for (Object object : list) {
                if (!(object instanceof Byte || object instanceof Short || object instanceof Integer || object instanceof Long)) {
                    allNumbers = false;
                    break;
                }
            }
            return allNumbers ? EditorValueKind.INTEGER_LIST : EditorValueKind.STRING_LIST;
        }
        if (value instanceof String) {
            return EditorValueKind.STRING;
        }

        return EditorValueKind.UNKNOWN;
    }

    public static boolean isItemSection(String path, ConfigurationSection section) {
        String lowerPath = lastSegment(path);
        if (lowerPath.contains("match-item")) {
            return false;
        }
        return lowerPath.equals("item") || lowerPath.equals("icon") || lowerPath.endsWith("-item")
                || section.contains("material") && !section.contains("click-actions")
                && !section.contains("trigger") && !section.contains("slots") && !section.contains("type");
    }

    public static String buildPath(String parentPath, String key) {
        if (parentPath == null || parentPath.isEmpty()) {
            return key;
        }
        return parentPath + "." + key;
    }

    public static String lastSegment(String path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        String[] split = path.split("\\.");
        return split[split.length - 1].toLowerCase(Locale.ENGLISH);
    }

}
