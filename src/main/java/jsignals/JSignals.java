package jsignals;

import jsignals.async.ResourceRef;
import jsignals.core.*;
import jsignals.runtime.EffectRunner;
import jsignals.runtime.JSignalsExecutor;
import jsignals.runtime.JSignalsRuntime;
import jsignals.util.JSignalsLogger;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Main API for the JSignals reactive library.
 * Provides factory methods for creating reactive primitives.
 */
public final class JSignals {

    private static final EffectRunner effectRunner = new EffectRunner();

    private static volatile JSignalsRuntime runtime;

    private static final Object runtimeLock = new Object();

    private static final Logger log = JSignalsLogger.getLogger("jsignals");

    private JSignals() { }

    /**
     * Initializes the JSignals shared runtime. Must be called before using
     * any other methods that rely on the default runtime.
     *
     * @return The newly created runtime.
     */
    public static JSignalsRuntime initRuntime() {
        synchronized (runtimeLock) {
            if (runtime != null) {
                log.error("Runtime is already initialized.");
                throw new IllegalStateException("JSignalsRuntime is already initialized.");
            }

            runtime = new JSignalsRuntime();
            log.info("Runtime initialized.");
            return runtime;
        }
    }

    /**
     * Shuts down the shared JSignals runtime, releasing all resources.
     */
    public static void shutdownRuntime() {
        synchronized (runtimeLock) {
            if (runtime != null) {
                runtime.close();
                runtime = null;
            }
        }
    }

    /**
     * Safely gets the active runtime, throwing an exception if it's not initialized.
     */
    private static JSignalsRuntime getRuntime() {
        JSignalsRuntime r = runtime; // Volatile read
        if (r == null) {
            throw new IllegalStateException("Runtime has not been initialized. Please call JSignals.initRuntime() first.");
        }

        return r;
    }

    /**
     * Submits a task to run on a virtual thread using the shared runtime.
     */
    public static CompletableFuture<Void> submitTask(Runnable task) {
        return getRuntime().getExecutor().submit(task);
    }

    /**
     * Submits a value-returning task to run on a virtual thread.
     */
    public static <T> CompletableFuture<T> submitTask(Supplier<T> task) {
        return getRuntime().getExecutor().submit(task);
    }

    /**
     * Schedules a task to run after a delay using the shared runtime's scheduler.
     */
    public static ScheduledFuture<?> scheduleTask(Runnable task, long delay, TimeUnit unit) {
        return getRuntime().getExecutor().schedule(task, delay, unit);
    }

    /**
     * Delays execution for the specified duration.
     */
    public static CompletableFuture<Void> delay(long duration, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        getRuntime().getExecutor().schedule(() -> future.complete(null), duration, unit);
        return future;
    }

    /**
     * Delays execution for the specified duration in milliseconds.
     */
    public static CompletableFuture<Void> delay(long duration) {
        return delay(duration, TimeUnit.MILLISECONDS);
    }

    /**
     * Creates a reactive reference with an initial value.
     */
    public static <T> Ref<T> ref(T initialValue) {
        return new Ref<>(initialValue);
    }

    /**
     * Creates a reactive reference with null initial value.
     */
    public static <T> Ref<T> ref() {
        return new Ref<>();
    }

    /**
     * Creates a computed value that automatically updates when dependencies change.
     */
    public static <T> ComputedRef<T> computed(Supplier<T> computation) {
        return new ComputedRef<>(computation);
    }

    /**
     * Creates a computed value that automatically updates when dependencies change,
     * with an initial value.
     */
    public static TriggerRef trigger() {
        return new TriggerRef();
    }

    /**
     * Creates a resource with default settings: auto-fetches, uses the default executor, and has no debounce.
     *
     * @param fetcher The supplier that provides a CompletableFuture to fetch the resource.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher) {
        return new ResourceRef<>(fetcher, true, getRuntime().getExecutor(), Duration.ZERO);
    }

    /**
     * Creates a resource, specifying whether it should fetch automatically.
     *
     * @param fetcher   The supplier for the fetch operation.
     * @param autoFetch Whether to automatically fetch the resource on creation.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch) {
        return new ResourceRef<>(fetcher, autoFetch, getRuntime().getExecutor(), Duration.ZERO);
    }

    /**
     * Creates a resource with a custom debounce delay.
     *
     * @param fetcher       The supplier for the fetch operation.
     * @param debounceDelay The delay to debounce fetch requests. If null or zero, no debouncing is applied.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher, Duration debounceDelay) {
        return new ResourceRef<>(fetcher, true, getRuntime().getExecutor(), debounceDelay);
    }

    /**
     * Creates a resource with a custom executor.
     *
     * @param fetcher  The supplier for the fetch operation.
     * @param executor The executor to run asynchronous parts of the fetch operation.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher, Executor executor) {
        return new ResourceRef<>(fetcher, true, executor, Duration.ZERO);
    }

    /**
     * Creates a resource with a custom executor and debounce delay.
     *
     * @param fetcher       The supplier for the fetch operation.
     * @param executor      The executor to run asynchronous parts of the fetch operation.
     * @param debounceDelay The delay to debounce fetch requests.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher, Executor executor, Duration debounceDelay) {
        return new ResourceRef<>(fetcher, true, executor, debounceDelay);
    }

    /**
     * Creates a resource with a custom auto-fetch behavior and debounce delay.
     *
     * @param fetcher       The supplier for the fetch operation.
     * @param autoFetch     Whether to automatically fetch the resource on creation.
     * @param debounceDelay The delay to debounce fetch requests.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(Supplier<CompletableFuture<T>> fetcher, boolean autoFetch, Duration debounceDelay) {
        return new ResourceRef<>(fetcher, autoFetch, getRuntime().getExecutor(), debounceDelay);
    }

    /**
     * Creates a resource that can be fetched with a custom executor and debounce delay.
     *
     * @param fetcher       The supplier that provides a CompletableFuture to fetch the resource.
     * @param autoFetch     Whether to automatically fetch the resource on creation.
     * @param executor      The executor to run asynchronous parts of the fetch operation. If null, uses the default {@link JSignalsExecutor}.
     * @param debounceDelay The delay to debounce fetch requests. If null or zero, no debouncing is applied.
     * @return A new {@link ResourceRef} instance.
     */
    public static <T> ResourceRef<T> resource(
            Supplier<CompletableFuture<T>> fetcher, boolean autoFetch, Executor executor, Duration debounceDelay) {
        return new ResourceRef<>(fetcher, autoFetch, executor, debounceDelay);
    }

    /**
     * Creates an effect that re-runs when its dependencies change.
     */
    public static Disposable effect(Runnable effect) {
        return effectRunner.runEffect(effect);
    }

    /**
     * Checks if an object is a reactive reference.
     */
    public static boolean isRef(Object obj) {
        return obj instanceof ReadableRef;
    }

    /**
     * Unwraps a ref to get its value, or returns the value itself if not a ref.
     */
    @SuppressWarnings("unchecked")
    public static <T> T unref(Object ref) {
        if (ref instanceof ReadableRef) {
            return ((ReadableRef<T>) ref).get();
        }
        return (T) ref;
    }

    /**
     * Disposes a disposable resource.
     * Safe to call with null.
     */
    public static void dispose(Disposable disposable) {
        if (disposable != null) {
            disposable.dispose();
        }
    }

}
