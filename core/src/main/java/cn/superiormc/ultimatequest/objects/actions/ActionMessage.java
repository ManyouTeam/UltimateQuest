package cn.superiormc.ultimatequest.objects.actions;

import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import cn.superiormc.ultimatequest.utils.TextUtil;
import org.bukkit.entity.Player;

public class ActionMessage<S extends AbstractConfiguredSection<C>, C> extends AbstractRunAction<S, C> {

    public ActionMessage() {
        super("message");
        setRequiredArgs("message");
    }

    @Override
    protected void onDoAction(S singleAction, Player player, C context) {
        TextUtil.sendMessage(player, singleAction.getString("message", player, context));
    }
}
