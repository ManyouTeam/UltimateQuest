package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.api.trigger.AbstractTrigger;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import cn.superiormc.ultimatequest.managers.QuestManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.damage.DamageSource;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

public final class TriggerRuntime {

    private static final NamespacedKey FIRING_SLOT_KEY = new NamespacedKey("ultimatequest", "firing_slot");

    private final TriggerManager manager;

    private final Map<UUID, TargetRecord> mobTargets = new ConcurrentHashMap<>();

    private final Map<UUID, UUID> barterPlayers = new ConcurrentHashMap<>();

    private final Map<UUID, TrackedProjectile> projectiles = new ConcurrentHashMap<>();

    private final Map<UUID, SchedulerUtil> playerTasks = new ConcurrentHashMap<>();

    private final Map<UUID, Double> attackCooldowns = new ConcurrentHashMap<>();

    private final Map<UUID, Runnable> pendingSpawns = new ConcurrentHashMap<>();

    private volatile boolean closed;

    public TriggerRuntime(TriggerManager manager) {
        this.manager = manager;
    }

    public TriggerData.Builder base(Player player, Event event) {
        return TriggerData.builder(player).source(player).location(player.getLocation()).event(event);
    }

    public TriggerData.Builder movement(Player player, Event event, Location from, Location to) {
        Location destination = to == null ? from : to;
        return base(player, event).movement(from, destination).location(destination).block(destination.getBlock());
    }

    public void fire(AbstractTrigger<?> trigger, TriggerData.Builder data) {
        if (!closed) {
            manager.fire(trigger, data.build());
        }
    }

    public void fire(AbstractTrigger<?> trigger, TriggerData data) {
        if (!closed) {
            manager.fire(trigger, data);
        }
    }

    public void startPlayerTicks(Player player) {
        if (closed || playerTasks.containsKey(player.getUniqueId())) {
            return;
        }
        SchedulerUtil task = SchedulerUtil.runTaskTimer(player, () -> {
            if (player.isOnline() && !closed
                    && (manager.isObserved("tick", player) || manager.isObserved("target_tick", player))) {
                manager.dispatch(PlayerTick.class, new PlayerTick(player, player.getTicksLived()));
            }
        }, 1L, 1L);
        SchedulerUtil previous = playerTasks.putIfAbsent(player.getUniqueId(), task);
        if (previous != null) {
            task.cancel();
        }
    }

    public boolean isObserved(String id, Player player) {
        return manager.isObserved(id, player);
    }

    public void whenPlayerReady(Player player, Runnable action) {
        if (closed) {
            return;
        }
        if (QuestManager.questManager == null || QuestManager.questManager.isLoaded(player)) {
            action.run();
        } else {
            pendingSpawns.put(player.getUniqueId(), action);
        }
    }

    public void playerReady(Player player) {
        Runnable action = pendingSpawns.remove(player.getUniqueId());
        if (!closed && action != null && player.isOnline()) {
            action.run();
        }
    }

    public void rememberTarget(UUID mob, UUID player, boolean monster) {
        mobTargets.put(mob, new TargetRecord(player, monster));
    }

    public Player removeTarget(UUID mob) {
        TargetRecord target = mobTargets.remove(mob);
        return target == null ? null : Bukkit.getPlayer(target.playerId());
    }

    public int countTargets(UUID player) {
        return (int) mobTargets.values().stream()
                .filter(TargetRecord::monster)
                .filter(target -> player.equals(target.playerId()))
                .count();
    }

    public void rememberBarter(UUID piglin, UUID player) {
        barterPlayers.put(piglin, player);
    }

    public Player takeBarterPlayer(UUID piglin) {
        UUID player = barterPlayers.remove(piglin);
        return player == null ? null : Bukkit.getPlayer(player);
    }

    public void recordAttackCooldown(Player player, double strength) {
        attackCooldowns.put(player.getUniqueId(), strength);
    }

    public double consumeAttackCooldown(Player player) {
        Double strength = attackCooldowns.remove(player.getUniqueId());
        return strength == null ? player.getAttackCooldown() : strength;
    }

    public void trackProjectile(Player player, Projectile projectile, ItemStack item, EquipmentSlot slot) {
        if (closed) {
            return;
        }
        stopTrackingProjectile(projectile);
        EquipmentSlot hand = slot == null ? EquipmentSlot.HAND : slot;
        projectile.getPersistentDataContainer().set(FIRING_SLOT_KEY, PersistentDataType.STRING, hand.name());
        ItemStack snapshot = item == null ? null : item.clone();
        UUID id = projectile.getUniqueId();
        UUID shooter = projectile.getShooter() instanceof Entity entity ? entity.getUniqueId() : player.getUniqueId();
        SchedulerUtil task = SchedulerUtil.runTaskTimer(projectile, () -> {
            if (closed || !player.isOnline() || !projectile.isValid() || projectile.isDead()
                    || projectile.isOnGround()) {
                stopTrackingProjectile(projectile);
                return;
            }
            Entity source = projectile.getShooter() instanceof Entity entity ? entity : player;
            manager.dispatch(ProjectileTick.class, new ProjectileTick(player, source, projectile,
                    snapshot, hand, projectile.getTicksLived()));
        }, 1L, 1L);
        projectiles.put(id, new TrackedProjectile(player.getUniqueId(), shooter, snapshot, hand, task));
    }

    public TrackedProjectile trackedProjectile(Projectile projectile) {
        return projectiles.get(projectile.getUniqueId());
    }

    public TrackedProjectile stopTrackingProjectile(Projectile projectile) {
        TrackedProjectile tracked = projectiles.remove(projectile.getUniqueId());
        if (tracked != null) {
            tracked.task().cancel();
        }
        return tracked;
    }

    public static EquipmentSlot readFiringSlot(Projectile projectile) {
        String stored = projectile.getPersistentDataContainer().get(FIRING_SLOT_KEY, PersistentDataType.STRING);
        if (stored == null) {
            return null;
        }
        try {
            return EquipmentSlot.valueOf(stored);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public EquipmentSlot findSlot(Player player, ItemStack item) {
        PlayerInventory inventory = player.getInventory();
        for (EquipmentSlot slot : List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND,
                EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (item != null && item.equals(inventory.getItem(slot))) {
                return slot;
            }
        }
        return EquipmentSlot.HAND;
    }

    public Entity getDamager(Entity damager) {
        if (damager instanceof Projectile projectile && projectile.getShooter() instanceof Entity shooter) {
            return shooter;
        }
        if (damager instanceof TNTPrimed tnt && tnt.getSource() != null) {
            return tnt.getSource();
        }
        if (damager instanceof AreaEffectCloud cloud && cloud.getSource() instanceof Entity source) {
            return source;
        }
        if (damager instanceof Tameable tameable) {
            AnimalTamer owner = tameable.getOwner();
            if (owner instanceof Entity entity) {
                return entity;
            }
        }
        if (damager instanceof EvokerFangs fangs && fangs.getOwner() != null) {
            return fangs.getOwner();
        }
        return damager;
    }

    public Entity getDamager(Entity direct, EntityDamageEvent event) {
        Entity source = getDamager(direct);
        DamageSource damageSource = event.getDamageSource();
        if (!(source instanceof Player) && damageSource != null && damageSource.getCausingEntity() != null) {
            return damageSource.getCausingEntity();
        }
        return source;
    }

    public void clearEntity(UUID entityId) {
        mobTargets.remove(entityId);
        barterPlayers.remove(entityId);
        TrackedProjectile tracked = projectiles.remove(entityId);
        if (tracked != null) {
            tracked.task().cancel();
        }
    }

    public void playerQuit(Player player) {
        UUID id = player.getUniqueId();
        SchedulerUtil task = playerTasks.remove(id);
        if (task != null) {
            task.cancel();
        }
        attackCooldowns.remove(id);
        pendingSpawns.remove(id);
        mobTargets.values().removeIf(target -> id.equals(target.playerId()));
        barterPlayers.values().removeIf(id::equals);
        for (Map.Entry<UUID, TrackedProjectile> entry : projectiles.entrySet()) {
            if (id.equals(entry.getValue().ownerId()) && projectiles.remove(entry.getKey(), entry.getValue())) {
                entry.getValue().task().cancel();
            }
        }
        clearEntity(id);
        if (UltimateQuest.methodUtil != null) {
            UltimateQuest.methodUtil.clearBossBars(player);
        }
    }

    public void close() {
        closed = true;
        playerTasks.values().forEach(SchedulerUtil::cancel);
        playerTasks.clear();
        projectiles.values().forEach(tracked -> tracked.task().cancel());
        projectiles.clear();
        mobTargets.clear();
        barterPlayers.clear();
        attackCooldowns.clear();
        pendingSpawns.clear();
    }

    public record PlayerTick(Player player, long tick) {
    }

    public record ProjectileTick(Player player, Entity shooter, Projectile projectile,
                                 ItemStack item, EquipmentSlot slot, long tick) {
    }

    public record TrackedProjectile(UUID ownerId, UUID shooterId, ItemStack item,
                                    EquipmentSlot slot, SchedulerUtil task) {
    }

    private record TargetRecord(UUID playerId, boolean monster) {
    }
}
