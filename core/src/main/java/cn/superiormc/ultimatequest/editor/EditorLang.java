package cn.superiormc.ultimatequest.editor;

import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.Locale;
import org.bukkit.entity.Player;

public class EditorLang {

    public static String text(Player player, String key, String fallback, String... args) {
        String[] values = new String[args.length + 1];
        values[0] = fallback;
        System.arraycopy(args, 0, values, 1, args.length);
        return LanguageManager.languageManager.getStringText(player, key, values);
    }

    public static void send(Player player, String key, String fallback, String... args) {
        TextUtil.sendMessage(player, text(player, key, fallback, args));
    }

    public static String scope(Player player, EditorScope scope) {
        return text(player, "editor.scope." + scope.name().toLowerCase(Locale.ROOT), scope.getDisplayName());
    }

    public static String valueKind(Player player, EditorValueKind kind) {
        return text(player, "editor.value-kind." + kind.name().toLowerCase(Locale.ROOT), kind.name());
    }

    public static String displayPath(Player player, String path) {
        if (path == null || path.isEmpty()) {
            return text(player, "editor.common.root", "root");
        }
        return path;
    }

}
