package cn.superiormc.ultimatequest.objects.triggers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.api.trigger.AbstractTrigger;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import cn.superiormc.ultimatequest.managers.CacheManager;
import cn.superiormc.ultimatequest.managers.TriggerManager;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import java.util.LinkedHashMap;
import java.util.Objects;
import org.bukkit.block.data.type.Beehive;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.EnchantmentStorageMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.TileState;
import org.bukkit.NamespacedKey;
import org.bukkit.damage.DamageSource;
import org.bukkit.entity.AnimalTamer;
import org.bukkit.entity.AreaEffectCloud;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EvokerFangs;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Monster;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Tameable;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryMoveItemEvent;
import org.bukkit.inventory.BlockInventoryHolder;
import org.bukkit.inventory.Inventory;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

public final class TriggerRuntime {

    private static final Set<InventoryAction> OUTPUT_ACTIONS = Set.of(
            InventoryAction.PICKUP_ALL, InventoryAction.PICKUP_ONE, InventoryAction.PICKUP_HALF,
            InventoryAction.PICKUP_SOME, InventoryAction.MOVE_TO_OTHER_INVENTORY,
            InventoryAction.HOTBAR_SWAP, InventoryAction.HOTBAR_MOVE_AND_READD,
            InventoryAction.DROP_ALL_SLOT, InventoryAction.DROP_ONE_SLOT);

    private static final NamespacedKey WORKSTATION_PLAYER_KEY = new NamespacedKey("ultimatequest", "workstation_player");

    private static final NamespacedKey FIRING_SLOT_KEY = new NamespacedKey("ultimatequest", "firing_slot");

    private final TriggerManager manager;

    private final Map<UUID, TargetRecord> mobTargets = new ConcurrentHashMap<>();

    private final Map<UUID, UUID> barterPlayers = new ConcurrentHashMap<>();

    private final Map<UUID, TrackedProjectile> projectiles = new ConcurrentHashMap<>();

    private final Map<UUID, SchedulerUtil> playerTasks = new ConcurrentHashMap<>();

    private final Map<UUID, Runnable> pendingSpawns = new ConcurrentHashMap<>();

    private volatile boolean closed;

    public TriggerRuntime(TriggerManager manager) {
        this.manager = manager;
    }

    public TriggerData.Builder base(Player player, Event event) {
        return TriggerData.builder(player)
                .source(player)
                .location(player.getLocation())
                .event(event);
    }

    public ItemStack anvilOutput(InventoryClickEvent event, String operation) {
        ItemStack first = event.getInventory().getItem(0);
        ItemStack result = event.getCurrentItem();
        if (first == null || result == null || first.getType() != result.getType()) {
            return null;
        }
        ItemMeta before = first.getItemMeta();
        ItemMeta after = result.getItemMeta();
        boolean matches;
        if (operation.equals("repair")) {
            matches = before instanceof Damageable oldDamage && after instanceof Damageable newDamage
                    && newDamage.getDamage() < oldDamage.getDamage();
        } else if (operation.equals("enchant")) {
            Map<Enchantment, Integer> oldEnchants = enchantmentLevels(first);
            matches = enchantmentLevels(result).entrySet().stream()
                    .anyMatch(entry -> entry.getValue() > oldEnchants.getOrDefault(entry.getKey(), 0));
        } else {
            String oldName = before != null && before.hasDisplayName() ? before.getDisplayName() : "";
            String newName = after != null && after.hasDisplayName() ? after.getDisplayName() : "";
            matches = !Objects.equals(oldName, newName);
        }
        return matches ? inventoryOutput(event, result, List.of(), false) : null;
    }

    private Map<Enchantment, Integer> enchantmentLevels(ItemStack item) {
        Map<Enchantment, Integer> enchants = new LinkedHashMap<>(item.getEnchantments());
        if (item.getItemMeta() instanceof EnchantmentStorageMeta book) {
            book.getStoredEnchants().forEach((enchantment, level) -> enchants.merge(enchantment, level, Math::max));
        }
        return enchants;
    }

    public void finishInventoryOutput(AbstractTrigger<?> trigger, InventoryClickEvent event, ItemStack output) {
        if (!(event.getWhoClicked() instanceof Player player) || event.isCancelled()) {
            return;
        }
        Inventory inventory = event.getInventory();
        ItemStack[] inputs = inventory.getContents();
        for (int slot = 0; slot < inputs.length; slot++) {
            inputs[slot] = inputs[slot] == null ? null : inputs[slot].clone();
        }
        int resultSlot = event.getSlot();
        SchedulerUtil.runTaskLater(player, () -> {
            if (closed || event.isCancelled() || !player.isOnline()) {
                return;
            }
            boolean changed = false;
            for (int slot = 0; slot < inputs.length; slot++) {
                if (slot == resultSlot) {
                    continue;
                }
                ItemStack before = inputs[slot];
                ItemStack after = inventory.getItem(slot);
                boolean oldEmpty = before == null || before.getType().isAir() || before.getAmount() <= 0;
                boolean newEmpty = after == null || after.getType().isAir() || after.getAmount() <= 0;
                if (!oldEmpty && (newEmpty || !before.isSimilar(after) || after.getAmount() < before.getAmount())) {
                    changed = true;
                    break;
                }
            }
            if (changed) {
                fire(trigger, base(player, event).outputItems(List.of(output)).triggerItem(output, null));
            }
        }, 1L);
    }

    public void harvestHoney(AbstractTrigger<?> trigger, PlayerInteractEvent event, Material tool) {
        Block block = event.getClickedBlock();
        ItemStack item = event.getItem();
        if (closed || event.getAction() != Action.RIGHT_CLICK_BLOCK || block == null || item == null
                || item.getType() != tool || event.useItemInHand() == Event.Result.DENY
                || !(block.getBlockData() instanceof Beehive hive) || hive.getHoneyLevel() < hive.getMaximumHoneyLevel()) {
            return;
        }
        Player player = event.getPlayer();
        EquipmentSlot hand = event.getHand();
        ItemStack bottle = item.clone();
        int before = item.getAmount();
        SchedulerUtil.runTaskLater(player, () -> {
            if (closed || event.isCancelled() || !player.isOnline()
                    || !(block.getBlockData() instanceof Beehive current) || current.getHoneyLevel() >= hive.getHoneyLevel()) {
                return;
            }
            ItemStack held = player.getInventory().getItem(hand);
            if (held.getType() != Material.HONEY_BOTTLE
                    && !(held.getType() == Material.GLASS_BOTTLE && held.getAmount() < before)) {
                return;
            }
            ItemStack output = new ItemStack(Material.HONEY_BOTTLE);
            fire(trigger, base(player, event).block(block).location(block.getLocation().add(0.5D, 0.5D, 0.5D))
                    .outputItems(List.of(output)).triggerItem(bottle, hand));
        }, 1L);
    }

    public ItemStack inventoryOutput(InventoryClickEvent event, ItemStack result,
                                     List<ItemStack> ingredients, boolean repeat) {
        if (event.isCancelled() || !(event.getWhoClicked() instanceof Player player)
                || event.getSlotType() != InventoryType.SlotType.RESULT
                || result == null || result.getType().isAir() || result.getAmount() <= 0) {
            return null;
        }
        InventoryAction action = event.getAction();
        if (!OUTPUT_ACTIONS.contains(action)) {
            return null;
        }
        int operations = 1;
        if (repeat && event.isShiftClick()) {
            operations = ingredients.stream().filter(item -> item != null && !item.getType().isAir())
                    .mapToInt(ItemStack::getAmount).min().orElse(0);
        }
        if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY) {
            long capacity = 0L;
            for (ItemStack stored : player.getInventory().getStorageContents()) {
                if (stored == null || stored.getType().isAir()) {
                    capacity += result.getMaxStackSize();
                } else if (stored.isSimilar(result)) {
                    capacity += Math.max(0, Math.min(stored.getMaxStackSize(), result.getMaxStackSize()) - stored.getAmount());
                }
            }
            operations = (int) Math.min(operations, capacity / result.getAmount());
        } else if (action == InventoryAction.PICKUP_ALL || action == InventoryAction.PICKUP_ONE
                || action == InventoryAction.PICKUP_HALF || action == InventoryAction.PICKUP_SOME) {
            ItemStack cursor = event.getCursor();
            if (cursor != null && !cursor.getType().isAir()
                    && (!cursor.isSimilar(result) || cursor.getAmount() + result.getAmount() > result.getMaxStackSize())) {
                return null;
            }
        }
        if (operations <= 0) {
            return null;
        }
        ItemStack output = result.clone();
        output.setAmount((int) Math.min(Integer.MAX_VALUE, (long) operations * result.getAmount()));
        return output;
    }

    public void trackWorkstationInput(Event event, Class<? extends Inventory> inventoryType, int... slots) {
        if (closed || event instanceof Cancellable cancellable && cancellable.isCancelled()) {
            return;
        }
        if (event instanceof InventoryMoveItemEvent move) {
            if (inventoryType.isInstance(move.getDestination())
                    && move.getDestination().getHolder() instanceof BlockInventoryHolder holder) {
                setWorkstationPlayer(holder.getBlock(), null);
            }
            return;
        }
        Inventory inventory;
        Player player;
        if (event instanceof InventoryClickEvent click && click.getWhoClicked() instanceof Player actor) {
            inventory = click.getInventory();
            player = actor;
            boolean inputClick = click.getClickedInventory() == inventory
                    && Arrays.stream(slots).anyMatch(slot -> slot == click.getSlot());
            boolean shiftInput = click.isShiftClick() && click.getClickedInventory() != null
                    && click.getClickedInventory() != inventory;
            if (!inputClick && !shiftInput) {
                return;
            }
        } else if (event instanceof InventoryDragEvent drag && drag.getWhoClicked() instanceof Player actor) {
            inventory = drag.getInventory();
            player = actor;
            if (Arrays.stream(slots).noneMatch(slot -> drag.getRawSlots().contains(slot))) {
                return;
            }
        } else {
            return;
        }
        if (!inventoryType.isInstance(inventory) || !(inventory.getHolder() instanceof BlockInventoryHolder holder)) {
            return;
        }
        Block block = holder.getBlock();
        Material blockType = block.getType();
        UUID playerId = player.getUniqueId();
        ItemStack[] previous = new ItemStack[slots.length];
        for (int index = 0; index < slots.length; index++) {
            ItemStack item = inventory.getItem(slots[index]);
            previous[index] = item == null ? null : item.clone();
        }
        SchedulerUtil.runTaskLater(block.getLocation(), () -> {
            if (closed || block.getType() != blockType || !(block.getState() instanceof BlockInventoryHolder currentHolder)) {
                return;
            }
            Inventory current = currentHolder.getInventory();
            boolean empty = true;
            boolean added = false;
            for (int index = 0; index < slots.length; index++) {
                ItemStack item = current.getItem(slots[index]);
                if (item == null || item.getType().isAir() || item.getAmount() <= 0) {
                    continue;
                }
                empty = false;
                ItemStack before = previous[index];
                if (before == null || !item.isSimilar(before) || item.getAmount() > before.getAmount()) {
                    added = true;
                }
            }
            if (added) {
                setWorkstationPlayer(block, playerId);
            } else if (empty) {
                setWorkstationPlayer(block, null);
            }
        }, 1L);
    }

    public Player workstationPlayer(Block block) {
        if (!(block.getState() instanceof TileState state)) {
            return null;
        }
        long[] owner = state.getPersistentDataContainer().get(WORKSTATION_PLAYER_KEY, PersistentDataType.LONG_ARRAY);
        if (owner == null || owner.length != 2) {
            return null;
        }
        Player player = Bukkit.getPlayer(new UUID(owner[0], owner[1]));
        return player != null && player.isOnline() ? player : null;
    }

    private void setWorkstationPlayer(Block block, UUID playerId) {
        if (!(block.getState() instanceof TileState state)) {
            return;
        }
        if (playerId == null) {
            state.getPersistentDataContainer().remove(WORKSTATION_PLAYER_KEY);
        } else {
            state.getPersistentDataContainer().set(WORKSTATION_PLAYER_KEY, PersistentDataType.LONG_ARRAY,
                    new long[] {playerId.getMostSignificantBits(), playerId.getLeastSignificantBits()});
        }
        state.update();
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
                    && (manager.isObserved("tick", player) || manager.isObserved("target_tick", player)
                    || manager.isObserved("placeholder", player))) {
                PlayerTick tick = new PlayerTick(player, player.getTicksLived());
                manager.dispatch(PlayerTick.class, tick);
                if (tick.tick() % 20L == 0L && manager.isObserved("placeholder", player)) {
                    manager.dispatch("placeholder", tick);
                }
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
        if (CacheManager.cacheManager == null || CacheManager.cacheManager.isLoaded(player)) {
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
        return countTargets(player, null);
    }

    public int countTargets(UUID player, Entity pendingTarget) {
        if (player == null) {
            return 0;
        }
        Player targetPlayer = Bukkit.getPlayer(player);
        if (targetPlayer == null || !targetPlayer.isOnline() || targetPlayer.isDead()) {
            clearPlayerTargets(player);
            return 0;
        }
        UUID pendingId = pendingTarget == null ? null : pendingTarget.getUniqueId();
        int count = pendingTarget instanceof Monster ? 1 : 0;
        for (Map.Entry<UUID, TargetRecord> entry : mobTargets.entrySet()) {
            UUID mobId = entry.getKey();
            TargetRecord record = entry.getValue();
            if (!record.monster() || !player.equals(record.playerId()) || mobId.equals(pendingId)) {
                continue;
            }
            Entity entity = Bukkit.getEntity(mobId);
            if (!(entity instanceof Monster monster) || !monster.isValid() || monster.isDead()
                    || !monster.getWorld().equals(targetPlayer.getWorld())) {
                mobTargets.remove(mobId, record);
                continue;
            }
            if (UltimateQuest.isFolia && !Bukkit.isOwnedByCurrentRegion(monster)) {
                SchedulerUtil.runSync(monster, () -> {
                    if (!isCurrentTarget(monster, targetPlayer)) {
                        mobTargets.remove(mobId, record);
                    }
                });
            } else if (!isCurrentTarget(monster, targetPlayer)) {
                mobTargets.remove(mobId, record);
                continue;
            }
            count++;
        }
        return count;
    }

    private boolean isCurrentTarget(Monster monster, Player player) {
        if (!monster.isValid() || monster.isDead() || !player.isOnline() || player.isDead()
                || !monster.getWorld().equals(player.getWorld())) {
            return false;
        }
        LivingEntity target = monster.getTarget();
        return target != null && player.getUniqueId().equals(target.getUniqueId());
    }

    public void refreshTarget(Entity entity) {
        if (closed || !(entity instanceof Monster monster)) {
            return;
        }
        UUID mobId = monster.getUniqueId();
        TargetRecord record = mobTargets.get(mobId);
        if (record != null && !record.monster()) {
            return;
        }
        if (monster.isValid() && !monster.isDead()
                && monster.getTarget() instanceof Player player && isCurrentTarget(monster, player)) {
            rememberTarget(mobId, player.getUniqueId(), true);
        } else if (record != null) {
            mobTargets.remove(mobId, record);
        }
    }

    public void clearPlayerTargets(UUID playerId) {
        mobTargets.values().removeIf(target -> playerId.equals(target.playerId()));
    }

    public void rememberBarter(UUID piglin, UUID player) {
        barterPlayers.put(piglin, player);
    }

    public Player takeBarterPlayer(UUID piglin) {
        UUID player = barterPlayers.remove(piglin);
        return player == null ? null : Bukkit.getPlayer(player);
    }

    public void trackProjectile(Player player, Projectile projectile, ItemStack item, EquipmentSlot slot) {
        if (closed) {
            return;
        }
        stopTrackingProjectile(projectile);
        EquipmentSlot hand = slot == null ? EquipmentSlot.HAND : slot;
        writeFiringSlot(projectile, hand);
        ItemStack snapshot = item == null ? null : item.clone();
        UUID id = projectile.getUniqueId();
        UUID shooter = projectile.getShooter() instanceof Entity entity ? entity.getUniqueId() : player.getUniqueId();
        AtomicBoolean retired = new AtomicBoolean();
        SchedulerUtil[] task = new SchedulerUtil[1];
        task[0] = SchedulerUtil.runTaskTimer(projectile, () -> {
            TrackedProjectile tracked = projectiles.get(id);
            if (tracked == null || tracked.task() != task[0]) {
                task[0].cancel();
                return;
            }
            if (closed || !player.isOnline() || !projectile.isValid() || projectile.isDead()
                    || projectile.isOnGround()) {
                stopTrackingProjectile(projectile);
                return;
            }
            Entity source = projectile.getShooter() instanceof Entity entity ? entity : player;
            manager.dispatch(ProjectileTick.class, new ProjectileTick(player, source, projectile,
                    snapshot, hand, projectile.getTicksLived()));
        }, () -> {
            retired.set(true);
            TrackedProjectile tracked = projectiles.get(id);
            if (tracked != null && tracked.task() == task[0]) {
                projectiles.remove(id, tracked);
            }
        }, 1L, 1L);
        if (task[0] == null) {
            return;
        }
        TrackedProjectile tracked = new TrackedProjectile(player.getUniqueId(), shooter, snapshot, hand, task[0]);
        projectiles.put(id, tracked);
        if ((closed || retired.get()) && projectiles.remove(id, tracked)) {
            task[0].cancel();
        }
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

    public static void writeFiringSlot(Projectile projectile, EquipmentSlot slot) {
        if (projectile == null || slot == null) {
            return;
        }
        projectile.getPersistentDataContainer().set(
                FIRING_SLOT_KEY, PersistentDataType.STRING, slot.name());
    }

    public static EquipmentSlot readFiringSlot(Projectile projectile) {
        if (projectile == null) {
            return null;
        }
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
        for (EquipmentSlot slot : List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
            if (inventory.getItem(slot) == item) {
                return slot;
            }
        }
        for (EquipmentSlot slot : List.of(EquipmentSlot.HAND, EquipmentSlot.OFF_HAND, EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
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
        clearPlayerTargets(entityId);
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
        pendingSpawns.remove(id);
        clearPlayerTargets(id);
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
