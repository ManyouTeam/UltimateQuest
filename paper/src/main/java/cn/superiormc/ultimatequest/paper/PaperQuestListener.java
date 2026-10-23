package cn.superiormc.ultimatequest.paper;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.ErrorManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        registerAttackCooldown();
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

    private void registerAttackCooldown() {
        try {
            Class<? extends Event> type = Class.forName("io.papermc.paper.event.player.PrePlayerAttackEntityEvent")
                    .asSubclass(Event.class);
            Bukkit.getPluginManager().registerEvent(type, this, EventPriority.MONITOR,
                    (listener, event) -> recordAttackCooldown(type, event), UltimateQuest.instance, true);
        } catch (ClassNotFoundException exception) {
            // Older Paper versions have no pre-attack event.
        }
    }

    private void recordAttackCooldown(Class<? extends Event> type, Event event) {
        if (!type.isInstance(event)) {
            return;
        }
        try {
            if ((boolean) type.getMethod("willAttack").invoke(event)) {
                Player player = (Player) type.getMethod("getPlayer").invoke(event);
                triggerManager.runtime().recordAttackCooldown(player, player.getAttackCooldown());
            }
        } catch (ReflectiveOperationException exception) {
            ErrorManager.errorManager.sendErrorMessage("§cError: Could not read pre-attack event: " + exception.getMessage());
        }
    }
}
