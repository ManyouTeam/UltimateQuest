package cn.superiormc.ultimatequest.api.trigger;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;

public final class TriggerData {

    private final Player player;

    private final Entity source;

    private final Entity target;

    private final Location from;

    private final Location to;

    private final EquipmentSlot triggerItemSlot;

    private final long tick;

    private final Block block;

    private final Location location;

    private final Event event;

    private final ItemStack triggerItem;

    private final List<ItemStack> outputItems;

    private final List<BlockState> placedBlocks;

    private final Map<ContextKey<?>, Object> extras;

    private TriggerData(Builder builder) {
        player = builder.player;
        source = builder.source;
        target = builder.target;
        from = builder.from == null ? null : builder.from.clone();
        to = builder.to == null ? null : builder.to.clone();
        triggerItemSlot = builder.triggerItemSlot;
        tick = builder.tick;
        block = builder.block;
        location = builder.location == null ? null : builder.location.clone();
        event = builder.event;
        triggerItem = builder.triggerItem;
        outputItems = builder.outputItems;
        placedBlocks = List.copyOf(builder.placedBlocks);
        extras = Map.copyOf(builder.extras);
    }

    public static Builder builder(Player player) {
        return new Builder(player);
    }

    public Player player() {
        return player;
    }

    public Entity sourceEntity() {
        return source;
    }

    public Entity targetEntity() {
        return target;
    }

    public Block block() {
        return block;
    }

    public Location location() {
        return location == null ? null : location.clone();
    }

    public Event event() {
        return event;
    }

    public ItemStack triggerItem() {
        return triggerItem;
    }

    public List<ItemStack> outputItems() {
        return outputItems.stream().map(ItemStack::clone).toList();
    }

    public List<BlockState> placedBlocks() {
        return placedBlocks;
    }

    public TriggerData withCountedItem(ItemStack item) {
        return copyBuilder().extra(BuiltinContextKeys.COUNTED_ITEM, item.clone()).build();
    }

    /** Snapshot a particular placed block for per-block conditions. */
    public TriggerData withPlacedBlock(BlockState state) {
        return copyBuilder().block(state.getBlock())
                .location(state.getLocation().add(0.5D, 0.5D, 0.5D))
                .extra(BuiltinContextKeys.BROKEN_BLOCK_STATE, state).build();
    }

    private Builder copyBuilder() {
        Builder copy = new Builder(player);
        copy.source = source;
        copy.target = target;
        copy.from = from;
        copy.to = to;
        copy.triggerItemSlot = triggerItemSlot;
        copy.tick = tick;
        copy.block = block;
        copy.location = location;
        copy.event = event;
        copy.triggerItem = triggerItem;
        copy.outputItems = outputItems;
        copy.placedBlocks = placedBlocks;
        copy.extras.putAll(extras);
        return copy;
    }

    public EquipmentSlot triggerItemSlot() {
        return triggerItemSlot;
    }

    public Location from() {
        return from == null ? null : from.clone();
    }

    public Location to() {
        return to == null ? null : to.clone();
    }

    public long tick() {
        return tick;
    }

    public Entity resolveEntity(EntitySelector selector) {
        return switch(selector) {
            case PLAYER -> {
                    yield player;
                }
            case SOURCE -> {
                    yield source;
                }
            case TARGET -> {
                    yield target;
                }
        };
    }

    public <T> Optional<T> extra(ContextKey<T> key) {
        Object value = extras.get(key);
        return value == null ? Optional.empty() : Optional.of(key.type().cast(value));
    }

    public static final class Builder {

        private final Player player;

        private Entity source;

        private Entity target;

        private Location from;

        private Location to;

        private EquipmentSlot triggerItemSlot;

        private long tick;

        private Block block;

        private Location location;

        private Event event;

        private ItemStack triggerItem;

        private List<ItemStack> outputItems = List.of();
        private List<BlockState> placedBlocks = List.of();

        private final Map<ContextKey<?>, Object> extras = new LinkedHashMap<>();

        private Builder(Player player) {
            this.player = Objects.requireNonNull(player, "player");
            source = player;
        }

        public Builder source(Entity value) {
            source = value;
            return this;
        }

        public Builder target(Entity value) {
            target = value;
            return this;
        }

        public Builder block(Block value) {
            block = value;
            return this;
        }

        public Builder location(Location value) {
            location = value;
            return this;
        }

        public Builder event(Event value) {
            event = value;
            return this;
        }

        public Builder triggerItem(ItemStack value, EquipmentSlot slot) {
            triggerItem = value;
            triggerItemSlot = slot;
            return this;
        }

        public Builder outputItems(List<ItemStack> items) {
            outputItems = items.stream().filter(Objects::nonNull)
                    .filter(item -> !item.getType().isAir() && item.getAmount() > 0)
                    .map(ItemStack::clone).toList();
            return this;
        }

        public Builder placedBlocks(List<BlockState> blocks) {
            placedBlocks = List.copyOf(blocks);
            return this;
        }

        public Builder triggerItem() {
            EquipmentSlot slot = EquipmentSlot.HAND;
            ItemStack item = player.getInventory().getItemInMainHand();
            if (item.getType() != Material.SHIELD
                    && player.getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                slot = EquipmentSlot.OFF_HAND;
                item = player.getInventory().getItemInOffHand();
            }
            return triggerItem(item, slot);
        }

        public Builder movement(Location original, Location destination) {
            from = original;
            to = destination;
            return this;
        }

        public Builder tick(long value) {
            tick = value;
            return this;
        }

        public <T> Builder extra(ContextKey<T> key, T value) {
            key.validate(value);
            if (value != null) {
                extras.put(key, value);
            }
            return this;
        }

        public TriggerData build() {
            return new TriggerData(this);
        }
    }
}
