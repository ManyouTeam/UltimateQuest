package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import cn.superiormc.ultimatequest.utils.TextUtil;
import java.util.UUID;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;

public abstract class AbstractEventCondition {

    private final String type;

    protected AbstractEventCondition(String type) {
        this.type = type;
    }

    public final boolean matches(ObjectSingleCondition condition) {
        boolean matched = alwaysMatches(condition) || condition.getSection().getBoolean("not", false) != onMatch(condition);
        sendResultMessage(condition, matched);
        return matched;
    }

    /**
     * Allows a condition type to be globally bypassed, including its {@code not} option.
     */
    protected boolean alwaysMatches(ObjectSingleCondition condition) {
        return false;
    }

    private void sendResultMessage(ObjectSingleCondition condition, boolean matched) {
        Player player = condition.player();
        if (player == null) {
            return;
        }
        String path = matched ? "meet-message" : "not-meet-message";
        TextUtil.sendMessage(player, condition.getString(path, player, condition.getContext()));
    }

    protected abstract boolean onMatch(ObjectSingleCondition condition);

    protected final boolean eventIs(Event event, String typeName) {
        if (event == null) {
            return false;
        }
        for (Class<?> type = event.getClass(); type != null; type = type.getSuperclass()) {
            if (type.getName().equals(typeName)) {
                return true;
            }
        }
        return false;
    }

    protected final Object eventValue(Event event, String typeName, String method) {
        if (!eventIs(event, typeName)) {
            return null;
        }
        try {
            return event.getClass().getMethod(method).invoke(event);
        } catch (ReflectiveOperationException exception) {
            return null;
        }
    }

    public void onInit() {
    }

    /**
     * Cleans type-wide runtime state.
     */
    public void onUnload() {
    }

    /**
     * Cleans type-wide runtime state associated with one entity.
     */
    public void onEntityUnload(UUID entityId) {
    }

    public final String getType() {
        return type;
    }
}
