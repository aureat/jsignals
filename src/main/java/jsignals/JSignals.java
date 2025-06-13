package jsignals;

import jsignals.async.Resource;
import jsignals.core.*;
import jsignals.runtime.EffectRunner;
import jsignals.runtime.JSignalsVThreadPool;
import jsignals.tests.ComputedAsync;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Main API for the JSignals reactive library.
 * Provides factory methods for creating reactive primitives.
 */
public final class JSignals {

    private static final EffectRunner effectRunner = new EffectRunner();

    private static final JSignalsVThreadPool threadPool = JSignalsVThreadPool.getInstance();

    private JSignals() { }

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
     * Creates an async computed value that runs on virtual threads.
     */
    public static <T> ComputedAsync<T> computedAsync(
            Supplier<CompletableFuture<T>> asyncComputation) {
        return new ComputedAsync<>(asyncComputation);
    }

    /**
     * Creates a resource for async data fetching.
     */
    public static <T> Resource<T> resource(
            Supplier<CompletableFuture<T>> fetcher) {
        return new Resource<>(fetcher, true);
    }

    /**
     * Creates a resource that doesn't auto-fetch.
     */
    public static <T> Resource<T> resource(
            Supplier<CompletableFuture<T>> fetcher, boolean autoFetch) {
        return new Resource<>(fetcher, autoFetch);
    }

    /**
     * Creates an effect that re-runs when its dependencies change.
     */
    public static Disposable effect(Runnable effect) {
        return effectRunner.runEffect(effect);
    }

    /**
     * Creates an async effect that runs on virtual threads.
     */
//    public static Disposable watchEffectAsync(
//            Supplier<CompletableFuture<?>> asyncEffect) {
//        AsyncEffect effect = new AsyncEffect(asyncEffect);
//        effect.run();
//        return effect;
//    }

    /**
     * Runs a callback on the next tick (microtask).
     */
    public static CompletableFuture<Void> nextTick(Runnable callback) {
        return threadPool.submit(callback);
    }

    /**
     * Delays execution for the specified duration.
     */
    public static CompletableFuture<Void> delay(long duration, TimeUnit unit) {
        CompletableFuture<Void> future = new CompletableFuture<>();
        threadPool.schedule(() -> future.complete(null), duration, unit);
        return future;
    }

    /**
     * Delays execution for the specified duration in milliseconds.
     */
    public static CompletableFuture<Void> delay(long duration) {
        return delay(duration, TimeUnit.MILLISECONDS);
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
