package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.hooks.BlockPriceUtil;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import org.bukkit.block.Block;
import java.util.Set;

public final class EventConditionBlockTypeOffset extends AbstractEventCondition {

    public EventConditionBlockTypeOffset() {
        super("block_type_offset");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null || context.block() == null) {
            return false;
        }
        Set<String> types = CommonUtil.values(condition.getSection(), "types", "type-name");
        if (types.isEmpty()) {
            return false;
        }
        Block block = context.block().getRelative(condition.getInt("offset.x", 0, context), condition.getInt("offset.y", 0, context), condition.getInt("offset.z", 0, context));
        return !types.isEmpty() && BlockPriceUtil.matchesAny(block, types);
    }
}
