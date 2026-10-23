package cn.superiormc.ultimatequest.commands;

import cn.superiormc.ultimatequest.managers.CommandManager;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public final class MainCommandTab implements TabCompleter {

    @Override
    public List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String alias, @NotNull String[] args) {
        return suggest(sender, args);
    }

    public List<String> suggest(CommandSender sender, String[] args) {
        List<String> results = new ArrayList<>();
        if (args.length <= 1) {
            for (AbstractCommand object : CommandManager.commandManager.getSubCommandsMap().values()) {
                if (object.getOnlyInGame() && !(sender instanceof Player)) {
                    continue;
                }
                String permission = sender instanceof Player ? object.getRequiredPermission() : object.getRequiredConsolePermission();
                if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                    continue;
                }
                results.add(object.getId());
            }
            String input = args.length == 0 ? "" : args[0].toLowerCase(Locale.ROOT);
            results.removeIf(result -> !result.toLowerCase(Locale.ROOT).startsWith(input));
        } else {
            AbstractCommand object = CommandManager.commandManager.getSubCommandsMap().get(args[0].toLowerCase(Locale.ROOT));
            if (object != null && (!object.getOnlyInGame() || sender instanceof Player)) {
                String permission = sender instanceof Player ? object.getRequiredPermission() : object.getRequiredConsolePermission();
                if (permission != null && !permission.isEmpty() && !sender.hasPermission(permission)) {
                    return results;
                }
                results = sender instanceof Player player ? object.filterTabResult(args, player) : object.filterConsoleTabResult(args);
            }
        }
        return results;
    }
}
