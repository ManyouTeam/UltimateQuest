package cn.superiormc.ultimatequest.objects.actions;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import org.bukkit.entity.Player;

public class ActionOPCommand<S extends AbstractConfiguredSection<C>, C> extends AbstractRunAction<S, C> {

    public ActionOPCommand() {
        super("op_command");
        setRequiredArgs("command");
    }

    @Override
    protected void onDoAction(S singleAction, Player player, C context) {
        UltimateQuest.methodUtil.dispatchOpCommand(player, singleAction.getString("command", player, context));
    }
}
