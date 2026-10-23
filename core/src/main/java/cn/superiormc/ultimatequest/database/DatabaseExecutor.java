package cn.superiormc.ultimatequest.database;

import cn.superiormc.ultimatequest.managers.ErrorManager;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class DatabaseExecutor {

    private static final int PLAYER_DATA_QUEUE_CAPACITY = 10_000;

    private static final long WARNING_INTERVAL_MILLIS = 30_000L;

    private static final Map<String, AtomicReference<Runnable>> pendingSaves = new HashMap<>();

    private static final Map<String, ArrayDeque<Runnable>> playerTasks = new HashMap<>();

    private static final AtomicLong lastQueueWarning = new AtomicLong();

    private static TrackingExecutor playerDataExecutor;

    private static boolean acceptingTasks;

    private static TrackingExecutor createPlayerDataExecutor() {
        int threadCount = databaseThreadCount();
        return new TrackingExecutor(threadCount, threadCount, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(PLAYER_DATA_QUEUE_CAPACITY), "UltimateQuest-PlayerData");
    }

    public static synchronized void start() {
        if (playerDataExecutor == null || playerDataExecutor.isShutdown()) {
            playerDataExecutor = createPlayerDataExecutor();
        }
        acceptingTasks = true;
    }

    public static synchronized void executePlayerSave(String storageId, Runnable saveTask) {
        if (storageId == null || storageId.isEmpty() || saveTask == null) {
            return;
        }
        ensureAccepting();
        AtomicReference<Runnable> pending = pendingSaves.get(storageId);
        if (pending != null) {
            pending.set(saveTask);
            return;
        }
        AtomicReference<Runnable> request = new AtomicReference<>(saveTask);
        pendingSaves.put(storageId, request);
        CompletableFuture<Void> future = new CompletableFuture<>();
        future.exceptionally(throwable -> {
            ErrorManager.errorManager.sendErrorMessage(
                    "§cError: Could not save data for " + storageId + ": " + throwable.getMessage());
            return null;
        });
        try {
            enqueuePlayerTask(storageId, databaseTask(() -> {
                Runnable current;
                synchronized (DatabaseExecutor.class) {
                    pendingSaves.remove(storageId, request);
                    current = request.get();
                }
                current.run();
                return null;
            }, future));
        } catch (RejectedExecutionException exception) {
            pendingSaves.remove(storageId, request);
            throw exception;
        }
    }

    public static CompletableFuture<Void> executePlayerTask(String storageId, Runnable task) {
        return supplyPlayerTask(storageId, () -> {
            task.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> supplyPlayerTask(String storageId, Supplier<T> task) {
        Objects.requireNonNull(storageId, "storageId");
        Objects.requireNonNull(task, "task");
        CompletableFuture<T> future = new CompletableFuture<>();
        synchronized (DatabaseExecutor.class) {
            try {
                ensureAccepting();
                pendingSaves.remove(storageId);
                enqueuePlayerTask(storageId, databaseTask(task, future));
            } catch (RejectedExecutionException exception) {
                future.completeExceptionally(exception);
            }
        }
        return future;
    }

    private static <T> Runnable databaseTask(Supplier<T> task, CompletableFuture<T> future) {
        return () -> {
            try {
                future.complete(task.get());
            } catch (Throwable throwable) {
                future.completeExceptionally(throwable);
            }
        };
    }

    private static void enqueuePlayerTask(String storageId, Runnable task) {
        ArrayDeque<Runnable> queue = playerTasks.get(storageId);
        if (queue != null) {
            queue.addLast(task);
            return;
        }
        ArrayDeque<Runnable> newQueue = new ArrayDeque<>();
        newQueue.addLast(task);
        playerTasks.put(storageId, newQueue);
        try {
            playerDataExecutor.execute(() -> drainPlayerTasks(storageId, newQueue));
        } catch (RejectedExecutionException exception) {
            playerTasks.remove(storageId, newQueue);
            throw exception;
        }
    }

    public static CompletableFuture<Void> executeDatabaseTask(Runnable task) {
        return supplyDatabaseTask(() -> {
            task.run();
            return null;
        });
    }

    public static <T> CompletableFuture<T> supplyDatabaseTask(Supplier<T> task) {
        CompletableFuture<T> future = new CompletableFuture<>();
        TrackingExecutor executor;
        synchronized (DatabaseExecutor.class) {
            if (!acceptingTasks) {
                future.completeExceptionally(new RejectedExecutionException("Database executor is not accepting tasks"));
                return future;
            }
            executor = playerDataExecutor;
            try {
                executor.execute(() -> {
                    try {
                        future.complete(task.get());
                    } catch (Throwable throwable) {
                        future.completeExceptionally(throwable);
                    }
                });
            } catch (RejectedExecutionException exception) {
                future.completeExceptionally(exception);
            }
        }
        return future;
    }

    private static void drainPlayerTasks(String storageId, ArrayDeque<Runnable> queue) {
        while (true) {
            Runnable task;
            synchronized (DatabaseExecutor.class) {
                task = queue.pollFirst();
                if (task == null) {
                    playerTasks.remove(storageId, queue);
                    return;
                }
            }
            task.run();
        }
    }

    public static synchronized void quiesce() {
        acceptingTasks = false;
    }

    public static synchronized void resume() {
        if (playerDataExecutor == null || playerDataExecutor.isShutdown()) {
            throw new RejectedExecutionException("UltimateQuest database executor is not running");
        }
        acceptingTasks = true;
    }

    public static void await() {
        TrackingExecutor currentExecutor;
        synchronized (DatabaseExecutor.class) {
            currentExecutor = playerDataExecutor;
        }
        if (currentExecutor != null) {
            currentExecutor.awaitTasks();
        }
    }

    public static synchronized void shutdown() {
        acceptingTasks = false;
        if (playerDataExecutor != null) {
            playerDataExecutor.shutdownNow();
            playerDataExecutor = null;
        }
        pendingSaves.clear();
        playerTasks.values().forEach(ArrayDeque::clear);
        playerTasks.clear();
    }

    private static int databaseThreadCount() {
        return Math.max(2, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));
    }

    private static void ensureAccepting() {
        if (!acceptingTasks) {
            throw new RejectedExecutionException("UltimateQuest database executor is not accepting tasks");
        }
    }

    private static class TrackingExecutor extends ThreadPoolExecutor {

        private final Object taskLock = new Object();

        private int pendingTasks;

        private TrackingExecutor(int corePoolSize, int maximumPoolSize, long keepAliveTime, TimeUnit unit, BlockingQueue<Runnable> workQueue, String threadName) {
            super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, runnable -> new Thread(runnable, threadName), (runnable, executor) -> {
                if (executor.isShutdown()) {
                    throw new RejectedExecutionException("Database executor is shut down");
                }
                warnQueueFull(threadName);
                runnable.run();
            });
        }

        @Override
        public void execute(Runnable command) {
            synchronized (taskLock) {
                pendingTasks++;
            }
            try {
                super.execute(() -> {
                    try {
                        command.run();
                    } finally {
                        synchronized (taskLock) {
                            pendingTasks--;
                            if (pendingTasks == 0) {
                                taskLock.notifyAll();
                            }
                        }
                    }
                });
            } catch (RejectedExecutionException exception) {
                synchronized (taskLock) {
                    pendingTasks--;
                    if (pendingTasks == 0) {
                        taskLock.notifyAll();
                    }
                }
                throw exception;
            }
        }

        private void awaitTasks() {
            synchronized (taskLock) {
                while (pendingTasks > 0) {
                    try {
                        taskLock.wait();
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    private static void warnQueueFull(String executorName) {
        long now = System.currentTimeMillis();
        long previous = lastQueueWarning.get();
        if (now - previous < WARNING_INTERVAL_MILLIS || !lastQueueWarning.compareAndSet(previous, now)) {
            return;
        }
        ErrorManager.errorManager.sendErrorMessage("§cError: " + executorName + " queue is full; applying caller backpressure to prevent data loss.");
    }
}
