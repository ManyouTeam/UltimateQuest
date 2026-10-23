package cn.superiormc.ultimatequest.objects.eventconditions;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.api.trigger.EntitySelector;
import cn.superiormc.ultimatequest.objects.ObjectSingleCondition;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class EventConditionPerfectGuard extends AbstractEventCondition implements Listener {

    private final Map<UUID, Long> raisedAt = new ConcurrentHashMap<>();

    private boolean registered;

    public EventConditionPerfectGuard() {
        super("perfect_guard");
    }

    @Override
    public void onInit() {
        if (!registered && Bukkit.getServer() != null && UltimateQuest.instance != null) {
            Bukkit.getPluginManager().registerEvents(this, UltimateQuest.instance);
            registered = true;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onInteract(PlayerInteractEvent event) {
        Action action = event.getAction();
        if (action != Action.RIGHT_CLICK_AIR && action != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        if (item == null || item.getType() != Material.SHIELD) {
            return;
        }
        raisedAt.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
    }

    @Override
    protected boolean onMatch(ObjectSingleCondition condition) {
        var context = condition.getContext();
        Player player = context == null ? null : context.player(condition.getSection().getString("target"), EntitySelector.TARGET);
        if (player == null || !player.isBlocking()) {
            return false;
        }
        Long raised = raisedAt.get(player.getUniqueId());
        double windowMillis = Math.max(0.0D, condition.getDouble("window-millis", 120.0D, context));
        long elapsed = raised == null ? Long.MAX_VALUE : System.currentTimeMillis() - raised;
        if (elapsed < 0L || elapsed > windowMillis) {
            return false;
        }
        if (condition.getSection().getBoolean("consume", true)) {
            raisedAt.remove(player.getUniqueId(), raised);
        }
        return true;
    }

    @Override
    public void onEntityUnload(UUID entityId) {
        if (entityId != null) {
            raisedAt.remove(entityId);
        }
    }

    @Override
    public void onUnload() {
        if (registered) {
            HandlerList.unregisterAll(this);
            registered = false;
        }
        raisedAt.clear();
    }
}
