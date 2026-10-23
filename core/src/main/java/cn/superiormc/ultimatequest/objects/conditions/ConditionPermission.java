package cn.superiormc.ultimatequest.objects.conditions;

import cn.superiormc.ultimatequest.objects.AbstractConfiguredSection;
import org.bukkit.entity.Player;

public class ConditionPermission<S extends AbstractConfiguredSection<C>, C> extends AbstractCheckCondition<S, C> {

    public ConditionPermission() {
        super("permission");
        setRequiredArgs("permission");
    }

    @Override
    protected boolean onCheckCondition(S singleCondition, Player player, C context) {
        return player.hasPermission(singleCondition.getString("permission"));
    }
}
