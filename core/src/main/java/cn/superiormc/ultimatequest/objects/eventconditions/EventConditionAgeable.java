package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;

public final class EventConditionAgeable extends AbstractEventCondition {

    public EventConditionAgeable() {
        super("ageable");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        BlockState broken = context.brokenBlockState();
        Block block = context.block();
        BlockData data = broken != null ? broken.getBlockData() : block == null ? null : block.getBlockData();
        return (data instanceof Ageable ageable && ageable.getAge() >= ageable.getMaximumAge()) == condition.getSection().getBoolean("value", true);
    }
}
