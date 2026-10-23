package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.hooks.BlockPriceUtil;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.utils.CommonUtil;
import java.util.Set;
import org.bukkit.block.BlockState;

public final class EventConditionBlockType extends AbstractEventCondition {

    public EventConditionBlockType() {
        super("block_type");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        QuestContext context = condition.getContext();
        if (context == null) {
            return false;
        }
        Set<String> types = CommonUtil.values(condition.getSection(), "types", "type-name");
        if (types.isEmpty()) {
            return false;
        }
        // Prefer the pre-break snapshot when present (the live block may already be air, e.g.
        // during on-block-drop-item). Fall back to the live block otherwise.
        BlockState broken = context.brokenBlockState();
        if (broken != null) {
            return BlockPriceUtil.matchesAny(broken, types);
        }
        return context.block() != null && BlockPriceUtil.matchesAny(context.block(), types);
    }
}
