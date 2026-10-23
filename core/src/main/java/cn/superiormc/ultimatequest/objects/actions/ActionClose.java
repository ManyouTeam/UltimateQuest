package cn.superiormc.ultimatequest.objects.actions;

import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import org.bukkit.entity.Player;

public class ActionClose<S extends AbstractConfiguredSection<C>, C> extends AbstractRunAction<S, C> {

    public ActionClose() {
        super("close");
    }

    @Override
    protected void onDoAction(S singleAction, Player player, C context) {
        player.closeInventory();
    }
}
