package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;

public final class EventConditionVillagerTrade extends AbstractEventCondition {

    public EventConditionVillagerTrade() {
        super("villager_trade");
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        if (condition.getContext() == null || !eventIs(condition.getContext().event(), "io.papermc.paper.event.player.PlayerPurchaseEvent")) {
            return false;
        }
        boolean expected = condition.getSection().getBoolean("value", true);
        return eventIs(condition.getContext().event(), "io.papermc.paper.event.player.PlayerTradeEvent") == expected;
    }
}
