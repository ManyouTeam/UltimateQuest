package cn.superiormc.ultimatequest.objects;

import cn.superiormc.ultimatequest.api.trigger.AbstractTrigger;
import cn.superiormc.ultimatequest.api.trigger.BuiltinContextKeys;
import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.inventory.ItemStack;

public record QuestContext(AbstractTrigger<?> trigger, TriggerData triggerData, Player player) {

    public int level() {
        return 1;
    }

    public Entity source() {
        return triggerData == null ? player : triggerData.sourceEntity();
    }

    public Entity target() {
        return triggerData == null ? null : triggerData.targetEntity();
    }

    public Entity entity(EntitySelector selector) {
        if (selector == EntitySelector.PLAYER) {
            return player;
        }
        return triggerData == null ? null : triggerData.resolveEntity(selector);
    }

    public Entity entity(String selector, EntitySelector fallback) {
        return entity(EntitySelector.parse(selector, fallback));
    }

    public LivingEntity livingEntity(String selector, EntitySelector fallback) {
        Entity entity = entity(selector, fallback);
        return entity instanceof LivingEntity living ? living : null;
    }

    public Event event() {
        return triggerData == null ? null : triggerData.event();
    }

    public Block block() {
        return triggerData == null ? null : triggerData.block();
    }

    public BlockState brokenBlockState() {
        return triggerData == null ? null : triggerData.extra(BuiltinContextKeys.BROKEN_BLOCK_STATE).orElse(null);
    }

    public ItemStack item() {
        return triggerItem();
    }

    public ItemStack triggerItem() {
        return triggerData == null ? null : triggerData.triggerItem();
    }

    public Location location() {
        return triggerData == null ? (player == null ? null : player.getLocation()) : triggerData.location();
    }

    public Player player(String selector, EntitySelector fallback) {
        Entity entity = entity(selector, fallback);
        return entity instanceof Player selected ? selected : null;
    }

    public QuestContext withTriggerData(TriggerData value) {
        return new QuestContext(trigger, value, player);
    }

    public QuestContext withPlayer(Player value) {
        return new QuestContext(trigger, triggerData, value);
    }
}
