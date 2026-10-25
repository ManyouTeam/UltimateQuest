package cn.superiormc.ultimatequest.listeners;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.EventConditionsManager;
import cn.superiormc.ultimatequest.managers.LanguageManager;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.managers.QuestScrollManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Item;
import org.bukkit.entity.Piglin;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.EntitiesUnloadEvent;
import org.bukkit.inventory.EquipmentSlot;

public class QuestListener implements Listener, AutoCloseable {

    protected final TriggerManager triggerManager;

    public QuestListener(TriggerManager triggerManager) {
        this.triggerManager = triggerManager;
        for (Class<?> type : triggerManager.eventClasses()) {
            if (Event.class.isAssignableFrom(type) && type.getName().startsWith("org.bukkit.")) {
                registerEvent(type.asSubclass(Event.class));
            }
        }
        for (Player player : Bukkit.getOnlinePlayers()) {
            triggerManager.runtime().startPlayerTicks(player);
        }
    }

    private <E extends Event> void registerEvent(Class<E> eventClass) {
        Bukkit.getPluginManager().registerEvent(eventClass, this, EventPriority.MONITOR, (listener, event) -> route(eventClass, event), UltimateQuest.instance, true);
    }

    @SuppressWarnings("deprecation")
    private <E extends Event> void route(Class<E> eventClass, Event event) {
        if (!eventClass.isInstance(event)) {
            return;
        }
        if (event instanceof PlayerFishEvent fishing && fishing.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
            return;
        }
        triggerManager.dispatch(eventClass, eventClass.cast(event));
        if (eventClass == EntityDamageByEntityEvent.class && event instanceof EntityDamageByEntityEvent damage && damage.getEntity() instanceof Player player && player.isBlocking() && damage.isApplicable(EntityDamageEvent.DamageModifier.BLOCKING) && damage.getDamage(EntityDamageEvent.DamageModifier.BLOCKING) < 0.0D) {
            triggerManager.dispatch("shield_block", damage);
        }
        if (event instanceof ProjectileHitEvent hit) {
            SchedulerUtil.runTaskLater(hit.getEntity(), () -> triggerManager.runtime().stopTrackingProjectile(hit.getEntity()), 1L);
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        CacheManager.cacheManager.join(event.getPlayer());
        triggerManager.runtime().startPlayerTicks(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onScroll(PlayerInteractEvent event) {
        if (event.getHand() != EquipmentSlot.HAND || event.useItemInHand() == Event.Result.DENY || (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK)) {
            return;
        }
        UUID token = QuestScrollManager.questScrollManager.token(event.getItem());
        if (token == null) {
            return;
        }
        event.setCancelled(true);
        if (!event.getPlayer().hasPermission("ultimatequest.use")) {
            LanguageManager.languageManager.sendStringText(event.getPlayer(), "error.miss-permission");
            return;
        }
        QuestScrollManager.questScrollManager.activate(event.getPlayer(), token);
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPiglinPickup(EntityPickupItemEvent event) {
        if (!(event.getEntity() instanceof Piglin piglin)) {
            return;
        }
        Item item = event.getItem();
        if (item.getItemStack().getType() == Material.GOLD_INGOT && item.getThrower() != null) {
            triggerManager.runtime().rememberBarter(piglin.getUniqueId(), item.getThrower());
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onUnload(EntitiesUnloadEvent event) {
        event.getEntities().forEach(entity -> {
            triggerManager.runtime().clearEntity(entity.getUniqueId());
            if (EventConditionsManager.eventConditions != null) {
                EventConditionsManager.eventConditions.onEntityUnload(entity.getUniqueId());
            }
        });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        triggerManager.runtime().playerQuit(event.getPlayer());
        QuestManager.questManager.clearProgressFeedback(event.getPlayer().getUniqueId());
        if (EventConditionsManager.eventConditions != null) {
            EventConditionsManager.eventConditions.onEntityUnload(event.getPlayer().getUniqueId());
        }
        CacheManager.cacheManager.quit(event.getPlayer());
    }

    @Override
    public void close() {
        triggerManager.runtime().close();
    }
}
