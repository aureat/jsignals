package jsignals.async;

import jsignals.core.Disposable;
import jsignals.core.ReadableRef;
import jsignals.core.Ref;
import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResourceRef<T> implements ReadableRef<ResourceState<T>>, Dependent {

    private final Supplier<CompletableFuture<T>> fetcher;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final AtomicReference<CompletableFuture<T>> currentFetch = new AtomicReference<>();

    private final AtomicReference<Ref<ResourceState<T>>> state = new AtomicReference<>(new Ref<>(ResourceState.idle()));

    private final AtomicReference<T> cachedValue = new AtomicReference<>();

    private final AtomicReference<Boolean> isTracking = new AtomicReference<>(false);

    public ResourceRef(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch) {
        this.fetcher = Objects.requireNonNull(fetcher, "Fetcher cannot be null");

        if (autoFetch) {
            fetch();
        }
    }

    public ResourceRef(Supplier<CompletableFuture<T>> fetcher) {
        this(fetcher, true);
    }

    @Override
    public ResourceState<T> get() {
        var currentState = state.get();
        return currentState.get();
    }

    @Override
    public ResourceState<T> getValue() {
        return state.get().getValue();
    }

    public Ref<ResourceState<T>> getRef() {
        return state.get();
    }

    public T getCachedValue() {
        return cachedValue.get();
    }

    /**
     * Subscribes to changes in the resource state.
     * The listener will be called with the current state whenever it changes.
     *
     * @param listener The listener to subscribe to state changes.
     * @return A Disposable that can be used to unsubscribe from state changes.
     */
    public Disposable subscribe(Consumer<ResourceState<T>> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        return state.get().subscribe(listener);
    }

    @Override
    public void onDependencyChanged() {
        System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Dependency changed, refetching...");

        // When a dependency changes, refetch the data
        fetch();
    }

    /**
     * Initiates a new fetch operation. This method is thread-safe.
     * If a fetch is already in progress, it will be cancelled and a new one will start.
     *
     * @return A CompletableFuture representing the new fetch operation.
     */
    public CompletableFuture<T> fetch() {
        state.get().set(ResourceState.loading(cachedValue.get()));

        System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Starting fetch...");

        boolean shouldStartTracking = isTracking.compareAndSet(false, true);
        CompletableFuture<T> newFetch;

        try {
            if (shouldStartTracking) {
                tracker.startTracking(this);
            }
            newFetch = fetcher.get(); // Execute the fetcher to get the future. This part might access reactive dependencies.
        } catch (Exception e) {
            // Handle immediate exceptions during fetcher execution.
            state.get().set(ResourceState.error(cachedValue.get(), e));
            return CompletableFuture.failedFuture(e);
        } finally {
            // Always stop tracking after the fetcher has run.
            if (shouldStartTracking) {
                tracker.stopTracking();
                // Reset the flag so the next fetch can track again.
                isTracking.set(false);
            }
        }

        // Atomically set the new future and get the one that was previously in flight.
        CompletableFuture<T> oldFetch = currentFetch.getAndSet(newFetch);

        // If there was a previous fetch running, cancel it.
        if (oldFetch != null && !oldFetch.isDone()) {
            System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Cancelling previous fetch...");
            oldFetch.cancel(true);
        }

        // Handle the result
        return newFetch
                .thenApply(data -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Fetch succeeded");
                    cachedValue.set(data);
                    state.get().set(ResourceState.success(cachedValue.get()));
                    return data;
                })
                .exceptionally(error -> {
                    Throwable cause = error.getCause();

                    // Handle cancellation
                    if (cause instanceof CancellationException) {
                        System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Fetch was cancelled");
                        state.get().set(ResourceState.cancelled(cachedValue.get(), cause));
                        return null;
                    }

                    System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Fetch failed with error: " + cause);
                    state.get().set(ResourceState.error(cachedValue.get(), cause));
                    return null;
                });
    }

    /**
     * Cancels the current fetch operation if it is in progress.
     */
    public void cancel() {
        var fetchToCancel = currentFetch.getAndSet(null);

        if (fetchToCancel != null && !fetchToCancel.isDone()) {
            System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Cancelling current fetch...");
            fetchToCancel.cancel(true);
        }

        state.get().set(ResourceState.idle(cachedValue.get()));
    }

    public boolean isLoading() {
        return state.get().getValue().isLoading();
    }

    public boolean hasError() {
        return state.get().getValue().isError();
    }

    public T getData() {
        return state.get().getValue().getData();
    }

    public Throwable getError() {
        return state.get().getValue().getError();
    }

    public boolean isSuccess() {
        return state.get().getValue().isSuccess();
    }

}
