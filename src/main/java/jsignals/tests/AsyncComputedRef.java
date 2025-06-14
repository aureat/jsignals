package jsignals.tests;

import jsignals.core.ReadableRef;
import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;
import jsignals.runtime.JSignalsExecutor;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Async computed value that properly tracks dependencies and re-executes when they change.
 */
public class AsyncComputedRef<T> implements ReadableRef<CompletableFuture<T>>, Dependent {

    private final Supplier<CompletableFuture<T>> computation;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final JSignalsExecutor threadPool = JSignalsExecutor.getInstance();

    private volatile CompletableFuture<T> currentComputation;

    private volatile boolean isDirty = true;

    private volatile boolean isComputing = false;

    public AsyncComputedRef(Supplier<CompletableFuture<T>> computation) {
        this.computation = Objects.requireNonNull(computation, "Computation cannot be null");
    }

    @Override
    public CompletableFuture<T> get() {
        // Track access to this async computed
        if (!isComputing) {
            tracker.trackAccess(this);
        }

        // Check if we need to recompute
        if (isDirty || currentComputation == null) {
            recompute();
        }

        return currentComputation;
    }

    @Override
    public CompletableFuture<T> getValue() {
        return currentComputation;
    }

    @Override
    public void onDependencyChanged() {
        System.out.println("  [AsyncComputed] Dependency changed, marking as dirty");

        // Mark as dirty
        isDirty = true;

        // Cancel current computation if it's still running
        if (currentComputation != null && !currentComputation.isDone()) {
            currentComputation.cancel(true);
            System.out.println("  [AsyncComputed] Cancelled previous computation");
        }

        // Notify our dependents
        tracker.notifyDependents(this);
    }

    private void recompute() {
        if (isComputing) {
            return; // Prevent circular dependencies
        }

        System.out.println("  [AsyncComputed] Starting async computation...");

        isComputing = true;
        isDirty = false;

        // Start tracking dependencies
        tracker.startTracking(this);

        try {
            // Run the async computation
            CompletableFuture<T> newComputation = computation.get();

            // Stop tracking
            tracker.stopTracking();

            // Store the computation
            currentComputation = newComputation;

            // When computation completes, notify dependents
            newComputation.whenComplete((result, error) -> {
                if (error != null) {
                    System.out.println("  [AsyncComputed] Computation failed: " + error.getMessage());
                } else {
                    System.out.println("  [AsyncComputed] Computation completed");
                }
                tracker.notifyDependents(this);
            });

        } catch (Exception e) {
            // Make sure we stop tracking on error
            tracker.stopTracking();
            currentComputation = CompletableFuture.failedFuture(e);
            throw new RuntimeException("Error in async computed value", e);
        } finally {
            isComputing = false;
        }
    }

}
