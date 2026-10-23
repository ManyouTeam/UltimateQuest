package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.listeners.AntiAbuseListener;
import cn.superiormc.ultimatequest.listeners.GUIListener;
import cn.superiormc.ultimatequest.listeners.QuestListener;
import org.bukkit.Bukkit;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;

public final class ListenerManager extends AbstractManager {

    public static ListenerManager listenerManager;

    public ListenerManager() {
        listenerManager = this;
        Bukkit.getPluginManager().registerEvents(new GUIListener(), UltimateQuest.instance);
        Bukkit.getPluginManager().registerEvents(new AntiAbuseListener(), UltimateQuest.instance);
        Bukkit.getPluginManager().registerEvents(new QuestListener(TriggerManager.triggerManager), UltimateQuest.instance);
        if (UltimateQuest.methodUtil.methodID().equals("paper")) {
            try {
                Class<?> type = Class.forName("cn.superiormc.ultimatequest.paper.PaperQuestListener");
                Listener listener = (Listener) type.getDeclaredConstructor(TriggerManager.class)
                        .newInstance(TriggerManager.triggerManager);
                Bukkit.getPluginManager().registerEvents(listener, UltimateQuest.instance);
            } catch (ReflectiveOperationException | LinkageError exception) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not register Paper quest events: " + exception.getMessage());
                throw new IllegalStateException("Could not register Paper quest events", exception);
            }
        }
    }

    public void unregisterAllListener() {
        HandlerList.unregisterAll(UltimateQuest.instance);
    }

    @Override
    public void onPluginDisable() {
        unregisterAllListener();
        listenerManager = null;
    }
}
