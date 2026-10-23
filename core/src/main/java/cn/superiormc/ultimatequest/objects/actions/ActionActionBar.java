package cn.superiormc.ultimatequest.objects.actions;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import org.bukkit.entity.Player;

public class ActionActionBar<S extends AbstractConfiguredSection<C>, C> extends AbstractRunAction<S, C> {

    public ActionActionBar() {
        super("action_bar");
        setRequiredArgs("message");
    }

    @Override
    protected void onDoAction(S singleAction, Player player, C context) {
        UltimateQuest.methodUtil.sendActionBar(player, singleAction.getString("message", player, context));
    }
}
