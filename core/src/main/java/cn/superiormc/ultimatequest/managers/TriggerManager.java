package cn.superiormc.ultimatequest.managers;

import cn.superiormc.ultimatequest.UltimateQuest;
import cn.superiormc.ultimatequest.api.registry.KeyedRegistry;
import cn.superiormc.ultimatequest.api.trigger.AbstractTrigger;
import cn.superiormc.ultimatequest.api.trigger.TriggerData;
import cn.superiormc.ultimatequest.objects.QuestContext;
import cn.superiormc.ultimatequest.objects.triggers.AirChangeTrigger;
import cn.superiormc.ultimatequest.objects.triggers.AttackTrigger;
import cn.superiormc.ultimatequest.objects.triggers.BlockBreakTrigger;
import cn.superiormc.ultimatequest.objects.triggers.BlockDamageTrigger;
import cn.superiormc.ultimatequest.objects.triggers.BlockDropItemTrigger;
import cn.superiormc.ultimatequest.objects.triggers.BlockPlaceTrigger;
import cn.superiormc.ultimatequest.objects.triggers.CombustTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ConsumeTrigger;
import cn.superiormc.ultimatequest.objects.triggers.CreeperExplodeTrigger;
import cn.superiormc.ultimatequest.objects.triggers.DamageByEntityTrigger;
import cn.superiormc.ultimatequest.objects.triggers.DamageTrigger;
import cn.superiormc.ultimatequest.objects.triggers.DeathTrigger;
import cn.superiormc.ultimatequest.objects.triggers.EnchantItemTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ExpChangeTrigger;
import cn.superiormc.ultimatequest.objects.triggers.FishTrigger;
import cn.superiormc.ultimatequest.objects.triggers.FoodLevelChangeTrigger;
import cn.superiormc.ultimatequest.objects.triggers.InputTrigger;
import cn.superiormc.ultimatequest.objects.triggers.InteractTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ItemDamageTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ItemHeldTrigger;
import cn.superiormc.ultimatequest.objects.triggers.KillTrigger;
import cn.superiormc.ultimatequest.objects.triggers.MeleeAttackTrigger;
import cn.superiormc.ultimatequest.objects.triggers.MoveTrigger;
import cn.superiormc.ultimatequest.objects.triggers.PiglinBarterTrigger;
import cn.superiormc.ultimatequest.objects.triggers.PotionEffectTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ProjectileHitTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ProjectileLaunchTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ProjectileTickTrigger;
import cn.superiormc.ultimatequest.objects.triggers.RegainTrigger;
import cn.superiormc.ultimatequest.objects.triggers.RespawnTrigger;
import cn.superiormc.ultimatequest.objects.triggers.RiptideTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ShieldBlockTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ShootBowTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ShootTrigger;
import cn.superiormc.ultimatequest.objects.triggers.SpawnTrigger;
import cn.superiormc.ultimatequest.objects.triggers.SwapHandTrigger;
import cn.superiormc.ultimatequest.objects.triggers.TargetTickTrigger;
import cn.superiormc.ultimatequest.objects.triggers.TargetTrigger;
import cn.superiormc.ultimatequest.objects.triggers.TickTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ToggleFlightTrigger;
import cn.superiormc.ultimatequest.objects.triggers.ToggleSneakTrigger;
import cn.superiormc.ultimatequest.objects.triggers.TriggerRuntime;
import cn.superiormc.ultimatequest.objects.triggers.UntagTrigger;
import cn.superiormc.ultimatequest.objects.triggers.VibrationReceiveTrigger;
import cn.superiormc.ultimatequest.utils.SchedulerUtil;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import org.bukkit.Bukkit;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;

public final class TriggerManager extends AbstractManager {

    public static TriggerManager triggerManager;

    private final KeyedRegistry<NamespacedKey, AbstractTrigger<?>> triggers = new KeyedRegistry<>();

    private final Map<Class<?>, List<AbstractTrigger<?>>> eventTriggers = new LinkedHashMap<>();

    private final List<Consumer<QuestContext>> subscribers = new CopyOnWriteArrayList<>();

    private final TriggerRuntime runtime = new TriggerRuntime(this);

    private volatile boolean closed;

    public TriggerManager() {
        triggerManager = this;
        register(new BlockBreakTrigger());
        register(new KillTrigger());
        register(new FishTrigger());
        register(new ConsumeTrigger());
        register(new BlockDamageTrigger());
        register(new AttackTrigger());
        register(new MoveTrigger());
        register(new ProjectileLaunchTrigger());
        register(new PotionEffectTrigger());
        register(new SwapHandTrigger());
        register(new AirChangeTrigger());
        register(new ToggleSneakTrigger());
        register(new RiptideTrigger());
        register(new InteractTrigger());
        register(new ShootTrigger());
        register(new ShootBowTrigger());
        register(new RespawnTrigger());
        register(new ItemHeldTrigger());
        register(new MeleeAttackTrigger());
        register(new FoodLevelChangeTrigger());
        register(new UntagTrigger());
        register(new BlockDropItemTrigger());
        register(new PiglinBarterTrigger());
        register(new TickTrigger());
        register(new SpawnTrigger());
        register(new EnchantItemTrigger());
        register(new ItemDamageTrigger());
        register(new BlockPlaceTrigger());
        register(new VibrationReceiveTrigger());
        register(new ProjectileHitTrigger());
        register(new ShieldBlockTrigger());
        register(new ProjectileTickTrigger());
        register(new CombustTrigger());
        register(new CreeperExplodeTrigger());
        register(new TargetTickTrigger());
        register(new InputTrigger());
        register(new DamageTrigger());
        register(new ToggleFlightTrigger());
        register(new ExpChangeTrigger());
        register(new TargetTrigger());
        register(new DamageByEntityTrigger());
        register(new DeathTrigger());
        register(new RegainTrigger());
        boolean paper = UltimateQuest.methodUtil != null && "paper".equals(UltimateQuest.methodUtil.methodID());
        registerOptional("AttemptSmashAttackTrigger", paper);
        registerOptional("ElytraBoostTrigger", paper);
        registerOptional("PickupExperienceTrigger", paper);
        registerOptional("TradeTrigger", paper);
        registerOptional("BlockBreakProgressUpdateTrigger", paper);
        registerOptional("EndermanAttackPlayerTrigger", paper);
        registerOptional("NameEntityTrigger", paper);
        registerOptional("PhantomPreSpawnTrigger", paper);
        registerOptional("ShieldDisableTrigger", paper);
        registerOptional("KnockbackTrigger", paper);
        registerOptional("ExhaustionTrigger", true);
        registerOptional("WardenAngerTrigger", paper);
        registerOptional("JumpTrigger", paper);
        registerOptional("EffectTickTrigger", paper);
        registerOptional("ItemGroupCooldownTrigger", paper);
        registerOptional("InsideBlockTrigger", paper);
        registerOptional("PurchaseTrigger", paper);
        registerOptional("LoadCrossbowTrigger", paper);
        registerOptional("LungeTrigger", paper);
    }

    private void registerOptional(String name, boolean enabled) {
        if (!enabled) {
            return;
        }
        try {
            Class<?> type = Class.forName("cn.superiormc.ultimatequest.objects.triggers." + name);
            register((AbstractTrigger<?>) type.getDeclaredConstructor().newInstance());
        } catch (ClassNotFoundException | NoClassDefFoundError exception) {
            // Optional events are absent on older server APIs.
        } catch (ReflectiveOperationException | LinkageError exception) {
            if (exception.getCause() instanceof NoClassDefFoundError || exception.getCause() instanceof ClassNotFoundException) {
                return;
            }
            if (ErrorManager.errorManager != null) {
                ErrorManager.errorManager.sendErrorMessage("§cError: Could not register trigger " + name + ": " + exception.getMessage());
            }
        }
    }

    public synchronized void register(AbstractTrigger<?> trigger) {
        Set<? extends Class<?>> classes = Set.copyOf(trigger.getEventClasses());
        for (Class<?> eventClass : classes) {
            if (!trigger.eventClass().isAssignableFrom(eventClass)) {
                throw new IllegalArgumentException("Invalid event subscription: " + eventClass.getName());
            }
        }
        triggers.register(trigger.key(), trigger);
        for (Class<?> eventClass : classes) {
            eventTriggers.computeIfAbsent(eventClass, ignored -> new ArrayList<>()).add(trigger);
        }
    }

    public synchronized Optional<AbstractTrigger<?>> get(String id) {
        String normalized = id.contains(":") ? id : "ultimatequest:" + id;
        return Optional.ofNullable(triggers.get(NamespacedKey.fromString(normalized)));
    }

    public synchronized Collection<AbstractTrigger<?>> values() {
        return List.copyOf(triggers.values());
    }

    public synchronized Set<Class<?>> eventClasses() {
        return new LinkedHashSet<>(eventTriggers.keySet());
    }

    public TriggerRuntime runtime() {
        return runtime;
    }

    public boolean isObserved(String id, Player player) {
        return subscribers.size() > 1 || QuestManager.questManager == null || QuestManager.questManager.hasTrigger(player, id);
    }

    public void subscribe(Consumer<QuestContext> subscriber) {
        subscribers.add(subscriber);
    }

    public void dispatch(String id, Object event) {
        if (!closed) {
            get(id).ifPresent(trigger -> trigger.dispatch(event, runtime));
        }
    }

    public <E> void dispatch(Class<E> eventClass, E event) {
        if (closed || (event instanceof Cancellable cancellable && cancellable.isCancelled())) {
            return;
        }
        eventClass.cast(event);
        List<AbstractTrigger<?>> selected;
        synchronized (this) {
            selected = List.copyOf(eventTriggers.getOrDefault(eventClass, List.of()));
        }
        for (AbstractTrigger<?> trigger : selected) {
            trigger.dispatch(event, runtime);
        }
    }

    public void fire(AbstractTrigger<?> trigger, TriggerData data) {
        if (closed || (data.event() instanceof Cancellable cancellable && cancellable.isCancelled())) {
            return;
        }
        if (UltimateQuest.isFolia && Bukkit.getServer() != null && !Bukkit.isOwnedByCurrentRegion(data.player())) {
            SchedulerUtil.runSync(data.player(), () -> fire(trigger, data));
            return;
        }
        QuestContext context = new QuestContext(trigger, data, data.player());
        for (Consumer<QuestContext> subscriber : subscribers) {
            subscriber.accept(context);
        }
    }

    @Override
    public synchronized void onPluginDisable() {
        closed = true;
        runtime.close();
        subscribers.clear();
        eventTriggers.clear();
        triggers.clear();
        triggerManager = null;
    }
}
