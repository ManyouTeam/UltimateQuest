package cn.superiormc.ultimatequest.utils;

import cn.superiormc.ultimatequest.UltimateQuest;
import java.util.concurrent.TimeUnit;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.scheduler.BukkitTask;

public class SchedulerUtil {

    private final Object task;

    public SchedulerUtil(BukkitTask task) {
        this.task = task;
    }

    public SchedulerUtil(Object task) {
        this.task = task;
    }

    public void cancel() {
        if (task == null) {
            return;
        }
        if (task instanceof BukkitTask bukkitTask) {
            bukkitTask.cancel();
            return;
        }
        try {
            task.getClass().getMethod("cancel").invoke(task);
        } catch (ReflectiveOperationException ignored) {
        }

    }

    public static void runSync(Runnable task) {
        if (UltimateQuest.isFolia) {
            Bukkit.getGlobalRegionScheduler().execute(UltimateQuest.instance, task);
        } else {
            Bukkit.getScheduler().runTask(UltimateQuest.instance, task);
        }
    }

    public static void runSync(Entity entity, Runnable task) {
        if (UltimateQuest.isFolia) {
            entity.getScheduler().run(UltimateQuest.instance, scheduledTask -> task.run(), null);
        } else {
            Bukkit.getScheduler().runTask(UltimateQuest.instance, task);
        }
    }

    public static void runSync(Entity entity, Runnable task, Runnable retired) {
        if (UltimateQuest.isFolia) {
            entity.getScheduler().run(UltimateQuest.instance, scheduledTask -> task.run(), retired);
        } else {
            Bukkit.getScheduler().runTask(UltimateQuest.instance, task);
        }
    }

    public static void runSync(Location location, Runnable task) {
        if (UltimateQuest.isFolia) {
            Bukkit.getRegionScheduler().run(UltimateQuest.instance, location, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTask(UltimateQuest.instance, task);
        }
    }

    public static void runTaskAsynchronously(Runnable task) {
        if (UltimateQuest.isFolia) {
            Bukkit.getAsyncScheduler().runNow(UltimateQuest.instance, scheduledTask -> task.run());
        } else {
            Bukkit.getScheduler().runTaskAsynchronously(UltimateQuest.instance, task);
        }
    }

    public static SchedulerUtil runTaskLater(Runnable task, long delayTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            return new SchedulerUtil(Bukkit.getGlobalRegionScheduler().runDelayed(UltimateQuest.instance, scheduledTask -> task.run(), delayTicks));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskLater(UltimateQuest.instance, task, delayTicks));
    }

    public static SchedulerUtil runTaskLater(Entity entity, Runnable task, long delayTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            return new SchedulerUtil(entity.getScheduler().runDelayed(UltimateQuest.instance, scheduledTask -> task.run(), null, delayTicks));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskLater(UltimateQuest.instance, task, delayTicks));
    }

    public static SchedulerUtil runTaskLater(Location location, Runnable task, long delayTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            return new SchedulerUtil(Bukkit.getRegionScheduler().runDelayed(UltimateQuest.instance, location, scheduledTask -> task.run(), delayTicks));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskLater(UltimateQuest.instance, task, delayTicks));
    }

    public static SchedulerUtil runTaskLaterAsynchronously(Runnable task, long delayTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            return new SchedulerUtil(Bukkit.getAsyncScheduler().runDelayed(UltimateQuest.instance, scheduledTask -> task.run(), delayTicks * 50L, TimeUnit.MILLISECONDS));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskLaterAsynchronously(UltimateQuest.instance, task, delayTicks));
    }

    public static SchedulerUtil runTaskTimer(Runnable task, long delayTicks, long periodTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            periodTicks = validTicks(periodTicks);
            return new SchedulerUtil(Bukkit.getGlobalRegionScheduler().runAtFixedRate(UltimateQuest.instance, scheduledTask -> task.run(), delayTicks, periodTicks));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskTimer(UltimateQuest.instance, task, delayTicks, periodTicks));
    }

    public static SchedulerUtil runTaskTimer(Entity entity, Runnable task, long delayTicks, long periodTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            periodTicks = validTicks(periodTicks);
            return new SchedulerUtil(entity.getScheduler().runAtFixedRate(UltimateQuest.instance, scheduledTask -> task.run(), null, delayTicks, periodTicks));
        }
        return runTaskTimer(task, delayTicks, periodTicks);
    }

    public static SchedulerUtil runTaskTimer(Entity entity, Runnable task, Runnable retired,
                                             long delayTicks, long periodTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            periodTicks = validTicks(periodTicks);
            var scheduledTask = entity.getScheduler().runAtFixedRate(
                    UltimateQuest.instance, ignored -> task.run(), retired, delayTicks, periodTicks);
            return scheduledTask == null ? null : new SchedulerUtil(scheduledTask);
        }
        return runTaskTimer(task, delayTicks, periodTicks);
    }

    public static SchedulerUtil runTaskTimer(Location location, Runnable task, long delayTicks, long periodTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            periodTicks = validTicks(periodTicks);
            return new SchedulerUtil(Bukkit.getRegionScheduler().runAtFixedRate(UltimateQuest.instance, location, scheduledTask -> task.run(), delayTicks, periodTicks));
        }
        return runTaskTimer(task, delayTicks, periodTicks);
    }

    public static SchedulerUtil runTaskTimerAsynchronously(Runnable task, long delayTicks, long periodTicks) {
        if (UltimateQuest.isFolia) {
            delayTicks = validTicks(delayTicks);
            periodTicks = validTicks(periodTicks);
            return new SchedulerUtil(Bukkit.getAsyncScheduler().runAtFixedRate(UltimateQuest.instance, scheduledTask -> task.run(), delayTicks * 50L, periodTicks * 50L, TimeUnit.MILLISECONDS));
        }
        return new SchedulerUtil(Bukkit.getScheduler().runTaskTimerAsynchronously(UltimateQuest.instance, task, delayTicks, periodTicks));
    }

    public static void teleport(Entity entity, Location location) {
        if (UltimateQuest.isFolia) {
            entity.teleportAsync(location);
            return;
        }
        entity.teleport(location);
    }

    private static long validTicks(long ticks) {
        return Math.max(1L, ticks);
    }
}
