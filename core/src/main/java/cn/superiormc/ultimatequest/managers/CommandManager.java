package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.commands.AbstractCommand;
import cn.superiormc.ultimatequest.commands.SubHelp;
import cn.superiormc.ultimatequest.commands.SubMenu;
import cn.superiormc.ultimatequest.commands.SubAccept;
import cn.superiormc.ultimatequest.commands.SubInfo;
import cn.superiormc.ultimatequest.commands.SubClaim;
import cn.superiormc.ultimatequest.commands.SubAbandon;
import cn.superiormc.ultimatequest.commands.SubBattlePass;
import cn.superiormc.ultimatequest.commands.SubMilestone;
import cn.superiormc.ultimatequest.commands.SubPeriod;
import cn.superiormc.ultimatequest.commands.SubScroll;
import cn.superiormc.ultimatequest.commands.MainCommand;
import cn.superiormc.ultimatequest.commands.MainCommandTab;
import cn.superiormc.ultimatequest.commands.SubGenerateItemFormat;
import cn.superiormc.ultimatequest.commands.SubGiveSaveItem;
import cn.superiormc.ultimatequest.commands.SubReload;
import cn.superiormc.ultimatequest.commands.SubSaveItem;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.bukkit.command.PluginCommand;

public final class CommandManager extends AbstractManager {

    public static CommandManager commandManager;

    private final Map<String, AbstractCommand> registeredCommands = new LinkedHashMap<>();

    public CommandManager() {
        commandManager = this;
        registerBukkitCommands();
        registerObjectCommand();
    }

    private void registerObjectCommand() {
        registerNewSubCommand(new SubHelp());
        registerNewSubCommand(new SubMenu());
        registerNewSubCommand(new SubAccept());
        registerNewSubCommand(new SubInfo());
        registerNewSubCommand(new SubClaim());
        registerNewSubCommand(new SubAbandon());
        registerNewSubCommand(new SubBattlePass());
        registerNewSubCommand(new SubMilestone());
        registerNewSubCommand(new SubPeriod());
        registerNewSubCommand(new SubScroll());
        registerNewSubCommand(new SubReload());
        registerNewSubCommand(new SubSaveItem());
        registerNewSubCommand(new SubGiveSaveItem());
        registerNewSubCommand(new SubGenerateItemFormat());
    }

    private void registerBukkitCommands() {
        if (UltimateQuest.methodUtil.methodID().equals("paper")) {
            return;
        }
        PluginCommand command = Objects.requireNonNull(UltimateQuest.instance.getCommand("ultimatequest"));
        command.setExecutor(new MainCommand());
        command.setTabCompleter(new MainCommandTab());
    }

    public void registerNewSubCommand(AbstractCommand command) {
        registeredCommands.put(command.getId(), command);
    }

    public Map<String, AbstractCommand> getSubCommandsMap() {
        return registeredCommands;
    }
}
