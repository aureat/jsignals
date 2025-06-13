package jsignals.async;

import jsignals.core.Disposable;
import jsignals.core.ReadableRef;
import jsignals.core.Ref;
import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;

import java.util.Objects;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class Resource<T> implements ReadableRef<ResourceState<T>>, Dependent {

    private final Supplier<CompletableFuture<T>> fetcher;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final Ref<ResourceState<T>> state;

    private volatile CompletableFuture<T> currentFetch;

    private volatile boolean isTracking = false;

    public Resource(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch) {
        this.fetcher = Objects.requireNonNull(fetcher, "Fetcher cannot be null");
        this.state = new Ref<>(ResourceState.idle());

        if (autoFetch) {
            fetch();
        }
    }

    public Resource(Supplier<CompletableFuture<T>> fetcher) {
        this(fetcher, true);
    }

    @Override
    public ResourceState<T> get() {
//        tracker.trackAccess(this); // TODO QUESTION: Should we track access here?
        return state.get();
    }

    @Override
    public ResourceState<T> getValue() {
        return state.getValue();
    }

    public Disposable subscribe(Consumer<ResourceState<T>> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        return state.subscribe(listener);
    }

    @Override
    public void onDependencyChanged() {
        System.out.println("  [Resource] Dependency changed, refetching...");
        // When a dependency changes, refetch the data
        fetch();
    }

    public CompletableFuture<T> fetch() {
        System.out.println("  [Resource] Starting fetch...");

        // Cancel any existing fetch
        if (currentFetch != null && !currentFetch.isDone()) {
            System.out.println("  [Resource] Cancelling previous fetch...");
            currentFetch.cancel(true);
        }

        // Set loading state
        state.set(ResourceState.loading());

        // Start tracking dependencies if not already
        if (!isTracking) {
            isTracking = true;
            tracker.startTracking(this);
        }

        try {
            // Execute the fetcher - this may access reactive values
            currentFetch = fetcher.get();

            // Stop tracking after fetcher runs
            if (isTracking) {
                tracker.stopTracking();
                isTracking = false;
            }

            // Handle the result
            return currentFetch
                    .thenApply(data -> {
                        System.out.println("  [Resource] Fetch succeeded");
                        state.set(ResourceState.success(data));
                        return data;
                    })
                    .exceptionally(error -> {
                        Throwable cause = error.getCause();
                        System.err.println("  [Resource] Fetch failed with error: " + error);
                        System.err.println("  [Resource] Fetch failed with cause: " + cause);

                        // Handle cancellation specifically
                        if (cause instanceof CancellationException) {
                            System.err.println("  [Resource] Fetch was cancelled");
                            state.set(ResourceState.cancelled(cause));
                            return null;
                        }

                        state.set(ResourceState.error(cause));
                        return null;
                    });
        } catch (Exception e) {
            // Make sure we stop tracking on error
            if (isTracking) {
                tracker.stopTracking();
                isTracking = false;
            }

            state.set(ResourceState.error(e));
            return CompletableFuture.failedFuture(e);
        }
    }

    public void cancel() {
        if (currentFetch != null && !currentFetch.isDone()) {
            System.out.println("  [Resource] Cancelling current fetch...");
            currentFetch.cancel(true);
        }
        state.set(ResourceState.idle());
    }

    public boolean isLoading() {
        return state.getValue().isLoading();
    }

    public boolean hasError() {
        return state.getValue().isError();
    }

    public T getData() {
        return state.getValue().getData();
    }

    public Throwable getError() {
        return state.getValue().getError();
    }

    public boolean isSuccess() {
        return state.getValue().isSuccess();
    }

}
