package jsignals.async;

import jsignals.core.Disposable;
import jsignals.core.ReadableRef;
import jsignals.core.Ref;
import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;
import jsignals.runtime.JSignalsExecutor;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ResourceRef<T> implements ReadableRef<ResourceState<T>>, Dependent {

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final AtomicReference<CompletableFuture<T>> currentFetch = new AtomicReference<>();

    private final AtomicReference<Ref<ResourceState<T>>> state = new AtomicReference<>(new Ref<>(ResourceState.idle()));

    private final AtomicReference<T> cachedValue = new AtomicReference<>();

    private final AtomicReference<Boolean> isTracking = new AtomicReference<>(false);

    private final Supplier<CompletableFuture<T>> fetcher;

    private final boolean isAutoFetch;

    private final Executor executor;

    private final JSignalsExecutor defaultExecutor = JSignalsExecutor.getInstance();

    private final Duration debounceDelay;

    private final boolean isDebounceEnabled;

    private final AtomicReference<ScheduledFuture<?>> debounceTask = new AtomicReference<>();

    private final AtomicReference<CompletableFuture<T>> debouncedFetchCompletion = new AtomicReference<>();

    public ResourceRef(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch, Executor executor, Duration debounceDelay) {
        this.fetcher = Objects.requireNonNull(fetcher, "Fetcher cannot be null");
        this.executor = Objects.requireNonNull(executor, "Executor cannot be null");
        this.debounceDelay = Objects.requireNonNull(debounceDelay, "Debounce delay cannot be null");
        this.isDebounceEnabled = !debounceDelay.isZero() && !debounceDelay.isNegative();
        this.isAutoFetch = autoFetch;

        if (autoFetch) {
            fetch();
        }
    }

    public ResourceRef(Supplier<CompletableFuture<T>> fetcher) {
        this(fetcher, true, JSignalsExecutor.getInstance(), Duration.ZERO);
    }

    public ResourceRef(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch) {
        this(fetcher, autoFetch, JSignalsExecutor.getInstance(), Duration.ZERO);
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
    public Disposable watch(Consumer<ResourceState<T>> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        return state.get().watch(listener);
    }

    @Override
    public void onDependencyChanged() {
        System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Dependency changed, refetching...");

        // When a dependency changes, refetch the data
        fetch();
    }

    /**
     * Fetches the resource, applying debounce if configured.
     * If debounce is enabled, it will wait for the specified delay before executing the fetch.
     *
     * @return A CompletableFuture that completes with the fetched data or an error.
     */
    public CompletableFuture<T> fetch() {
        // If no debouncing is configured, fetch immediately
        if (!isDebounceEnabled) {
            return fetchNow();
        }

        // Atomically get and cancel any previously scheduled debounce task
        ScheduledFuture<?> oldTask = debounceTask.getAndSet(null);
        if (oldTask != null) {
            oldTask.cancel(false); // Don't interrupt if already running
        }

        // Ensure we have a single CompletableFuture to represent the eventual result of this debounced fetch
        debouncedFetchCompletion.updateAndGet(currentFuture ->
                (currentFuture == null || currentFuture.isDone()) ? new CompletableFuture<>() : currentFuture
        );

        // Schedule the actual fetch to run after the delay.
        Runnable fetchTask = () -> {
            CompletableFuture<T> fetchResult = fetchNow();

            // Link the result of the actual fetch to the completion source we returned.
            fetchResult.whenComplete((result, error) -> {
                CompletableFuture<T> completionSource = debouncedFetchCompletion.get();
                if (completionSource != null && !completionSource.isDone()) {
                    if (error != null) {
                        completionSource.completeExceptionally(error);
                    } else {
                        completionSource.complete(result);
                    }
                }
            });
        };

        // Create a new scheduled task with the debounce delay
        ScheduledFuture<?> newScheduledTask = defaultExecutor.getScheduler().schedule(fetchTask, debounceDelay.toMillis(), TimeUnit.MILLISECONDS);
        debounceTask.set(newScheduledTask);

        return debouncedFetchCompletion.get();
    }

    /**
     * Immediately fetches the resource without debounce.
     * This method is called internally and should not be used directly.
     *
     * @return A CompletableFuture that completes with the fetched data or an error.
     */
    private CompletableFuture<T> fetchNow() {
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
                .thenApplyAsync(data -> {
                    System.out.println("[" + Thread.currentThread().getName() + "] [Resource] Fetch succeeded");
                    cachedValue.set(data);
                    state.get().set(ResourceState.success(cachedValue.get()));
                    return data;
                }, executor)
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
