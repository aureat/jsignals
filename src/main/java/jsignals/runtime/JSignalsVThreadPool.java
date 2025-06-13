package jsignals.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Manages virtual threads for the reactive system.
 * Provides efficient scheduling and execution of reactive computations.
 */
public class JSignalsVThreadPool {

    private static final JSignalsVThreadPool INSTANCE = new JSignalsVThreadPool();

    private final ThreadFactory virtualThreadFactory;

    private final ScheduledExecutorService scheduler;

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    private JSignalsVThreadPool() {
        // Create virtual thread factory
        this.virtualThreadFactory = Thread.ofVirtual()
                .name("signals-vthread-", threadCounter.getAndIncrement())
                .factory();

        // Create a scheduler for delayed tasks
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "signals-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static JSignalsVThreadPool getInstance() {
        return INSTANCE;
    }

    /**
     * Submits a task to run on a virtual thread.
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return CompletableFuture.runAsync(task, this::execute);
    }

    /**
     * Submits a task that returns a value.
     */
    public <T> CompletableFuture<T> submit(Callable<T> task) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return task.call();
            } catch (Exception e) {
                throw new CompletionException(e);
            }
        }, this::execute);
    }

    /**
     * Schedules a task to run after a delay.
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        return scheduler.schedule(() -> execute(task), delay, unit);
    }

    /**
     * Executes a task on a virtual thread.
     */
    private void execute(Runnable task) {
        Thread vthread = virtualThreadFactory.newThread(task);
        vthread.start();
    }

    /**
     * Shuts down the thread pool.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

}
