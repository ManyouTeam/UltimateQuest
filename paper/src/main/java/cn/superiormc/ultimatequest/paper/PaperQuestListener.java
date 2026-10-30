package cn.superiormc.ultimatequest.paper;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

public final class PaperQuestListener implements Listener {

    private final TriggerManager triggerManager;

    public PaperQuestListener(TriggerManager triggerManager) {
        this.triggerManager = triggerManager;
        for (Class<?> type : triggerManager.eventClasses()) {
            if (Event.class.isAssignableFrom(type) && !type.getName().startsWith("org.bukkit.")) {
                registerEvent(type.asSubclass(Event.class));
            }
        }
    }

    private <E extends Event> void registerEvent(Class<E> eventClass) {
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR,
                (listener, event) -> {
                    if (eventClass.isInstance(event) && accepted(event)) {
                        triggerManager.dispatch(eventClass, eventClass.cast(event));
                    }
                }, UltimateQuest.instance, true);
    }

    private boolean accepted(Event event) {
        if (event instanceof Cancellable cancellable && cancellable.isCancelled()) {
            return false;
        }
        return true;
    }
}
