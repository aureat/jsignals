package jsignals.tests;

import jsignals.core.Disposable;

import java.util.concurrent.*;
import java.util.function.Supplier;

/**
 * Fixed async effect that properly cancels previous operations.
 */
public class AsyncEffect implements Disposable {

    private final Supplier<CompletableFuture<?>> effect;

    private final ScheduledExecutorService scheduler;

    private volatile CompletableFuture<?> currentOperation;

    private volatile ScheduledFuture<?> scheduledTask;

    private volatile boolean disposed = false;

    public AsyncEffect(Supplier<CompletableFuture<?>> effect, long debounceMs) {
        this.effect = effect;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Thread.ofVirtual().unstarted(r);
            t.setDaemon(true);
            return t;
        });

        if (debounceMs > 0) {
            scheduleDebounced(debounceMs);
        } else {
            execute();
        }
    }

    private void scheduleDebounced(long debounceMs) {
        // Cancel any existing scheduled task
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }

        scheduledTask = scheduler.schedule(this::execute, debounceMs, TimeUnit.MILLISECONDS);
    }

    private void execute() {
        if (disposed) return;

        // Cancel current operation if still running
        if (currentOperation != null && !currentOperation.isDone()) {
            currentOperation.cancel(true);
            System.out.println("  [AsyncEffect] Cancelled previous operation");
        }

        try {
            currentOperation = effect.get();

            currentOperation.exceptionally(error -> {
                if (!(error instanceof CancellationException)) {
                    System.err.println("  [AsyncEffect] Error: " + error.getMessage());
                }
                return null;
            });
        } catch (Exception e) {
            System.err.println("  [AsyncEffect] Failed to start: " + e.getMessage());
        }
    }

    public void trigger() {
        execute();
    }

    @Override
    public void dispose() {
        disposed = true;

        if (currentOperation != null && !currentOperation.isDone()) {
            currentOperation.cancel(true);
        }

        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(true);
        }

        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(100, TimeUnit.MILLISECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

}
