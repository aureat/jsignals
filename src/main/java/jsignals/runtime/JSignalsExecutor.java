package jsignals.runtime;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Manages virtual threads for the reactive system.
 * Provides efficient scheduling and execution of reactive computations.
 */
public class JSignalsExecutor implements Executor, AutoCloseable {

    private static final JSignalsExecutor INSTANCE = new JSignalsExecutor();

    private final ThreadFactory virtualThreadFactory;

    private final ScheduledExecutorService scheduler;

    private final AtomicInteger threadCounter = new AtomicInteger(0);

    JSignalsExecutor() {
        // Create virtual thread factory
        this.virtualThreadFactory = Thread.ofVirtual()
                .name("jsignals-vthread-", threadCounter.getAndIncrement())
                .factory();

        // Create a scheduler for delayed tasks
        this.scheduler = Executors.newScheduledThreadPool(1, r -> {
            Thread t = new Thread(r, "jsignals-scheduler");
            t.setDaemon(true);
            return t;
        });
    }

    public static JSignalsExecutor getInstance() {
        return INSTANCE;
    }

    public ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * Submits a fire-and-forget task to run on a new virtual thread.
     *
     * @return A CompletableFuture that completes when the task is done.
     */
    public CompletableFuture<Void> submit(Runnable task) {
        return CompletableFuture.runAsync(task, this);
    }

    /**
     * Submits a task that returns a value to run on a new virtual thread.
     *
     * @return A CompletableFuture that will complete with the result of the task.
     */
    public <T> CompletableFuture<T> submit(Supplier<T> task) {
        return CompletableFuture.supplyAsync(task, this);
    }

    /**
     * Schedules a task to run on a virtual thread after a given delay.
     *
     * @return A ScheduledFuture representing the pending completion of the task.
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        // The task is first submitted to the scheduler, which then executes it on a virtual thread.
        return scheduler.schedule(() -> this.execute(task), delay, unit);
    }

    /**
     * Executes a task on a virtual thread.
     */
    public void execute(Runnable task) {
        Thread vthread = virtualThreadFactory.newThread(task);
        vthread.start();
    }

    /**
     * Initiates an orderly shutdown of the executor.
     */
    public void shutdown() {
        scheduler.shutdown();
    }

    /**
     * Initiates an immediate shutdown of the executor, interrupting all tasks.
     */
    @Override
    public void close() {
        scheduler.shutdownNow();
    }

}
