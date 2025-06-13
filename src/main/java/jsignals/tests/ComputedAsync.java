package jsignals.tests;

import jsignals.core.ReadableRef;
import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;
import jsignals.runtime.JSignalsVThreadPool;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * An async computed value that runs on virtual threads.
 * Automatically cancels and re-runs when dependencies change.
 */
public class ComputedAsync<T> implements ReadableRef<T>, Dependent {

    private final Supplier<CompletableFuture<T>> asyncComputation;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final JSignalsVThreadPool threadPool = JSignalsVThreadPool.getInstance();

    private final AtomicReference<T> cachedValue = new AtomicReference<>();

    private final AtomicReference<CompletableFuture<T>> currentComputation = new AtomicReference<>();

    private final AtomicReference<State> state = new AtomicReference<>(State.IDLE);

    private enum State {
        IDLE, COMPUTING, READY, ERROR
    }

    public ComputedAsync(Supplier<CompletableFuture<T>> asyncComputation) {
        this.asyncComputation = asyncComputation;
        recompute();
    }

    @Override
    public T get() {
        tracker.trackAccess(this);

        State currentState = state.get();
        if (currentState == State.READY) {
            return cachedValue.get();
        } else if (currentState == State.COMPUTING) {
            // Wait for computation to complete
            try {
                return currentComputation.get().join();
            } catch (Exception e) {
                System.out.println(e);
                throw new RuntimeException("Async computation failed", e);
            }
        } else {
            throw new IllegalStateException("Async computed value not ready");
        }
    }

    @Override
    public T getValue() {
        return cachedValue.get();
    }

    @Override
    public void onDependencyChanged() {
        recompute();
    }

    private void recompute() {
        // Cancel any existing computation
        CompletableFuture<T> existing = currentComputation.get();
        if (existing != null && !existing.isDone()) {
            existing.cancel(true);
        }

        state.set(State.COMPUTING);
        tracker.notifyDependents(this);

        // Start new computation
        tracker.startTracking(this);

        CompletableFuture<T> future = threadPool.submit(() -> {
                    try {
                        return asyncComputation.get();
                    } finally {
                        tracker.stopTracking();
                    }
                }).thenCompose(f -> f)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        state.set(State.ERROR);
                    } else {
                        cachedValue.set(result);
                        state.set(State.READY);
                    }
                    tracker.notifyDependents(this);
                });

        currentComputation.set(future);
    }

    /**
     * Checks if the async computation is currently running.
     */
    public boolean isComputing() {
        tracker.trackAccess(this);
        return state.get() == State.COMPUTING;
    }

    /**
     * Checks if the value is ready.
     */
    public boolean isReady() {
        tracker.trackAccess(this);
        return state.get() == State.READY;
    }

}
