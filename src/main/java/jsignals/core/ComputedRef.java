package jsignals.core;

import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;
import jsignals.util.JSignalsLogger;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static jsignals.JSignals.submitTask;

/**
 * A computed reactive value that automatically updates when its dependencies change.
 * Thread-safe and lazily evaluated.
 */
public class ComputedRef<T> implements ReadableRef<T>, Dependent {

    private final Supplier<T> computation;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final DependentNotifier dependentNotifier = new DependentNotifier(this);

    private final SubscriptionNotifier<BiConsumer<T, T>> subscriptions = new SubscriptionNotifier<>();

    private final ReentrantReadWriteLock computationLock = new ReentrantReadWriteLock();

    private final AtomicReference<T> cachedValue = new AtomicReference<>();

    private final AtomicBoolean dirty = new AtomicBoolean(true);

    private final AtomicBoolean computing = new AtomicBoolean(false);

    private final boolean lazy;

    private final Logger log = JSignalsLogger.getLogger(getName());

    /**
     * Creates a new computed value with the given computation.
     */
    public ComputedRef(Supplier<T> computation) {
        this(computation, true);
    }

    public ComputedRef(Supplier<T> computation, boolean lazy) {
        this.computation = Objects.requireNonNull(computation, "Computation cannot be null");
        this.lazy = lazy;

        recompute(); // Initial computation to set the cached value
    }

    private T getVal() {
        // An optimistic read without a lock. If the value is clean, we avoid locking entirely.
        if (!dirty.get()) {
            log.trace("Returning cached value without recomputation.");
            return cachedValue.get();
        }

        // If the value might be dirty, we proceed to the safe recomputation path.
        return recompute();
    }

    @Override
    public T get() {
        log.trace("ComputedRef.get()");

        // First, register that the current running computation (if any) depends on this ComputedRef.
        dependentNotifier.trackAccess();
        return getVal();
    }

    @Override
    public T getValue() {
        log.trace("ComputedRef.getValue()");
        return getVal();
    }

    public boolean isDirty() {
        return dirty.get();
    }

    public boolean isComputing() {
        return computing.get();
    }

    public boolean isLazy() {
        return lazy;
    }

    /**
     * Subscribes to value changes.
     *
     * @param listener Called when the value changes
     * @return A Disposable to unsubscribe
     */
    public Disposable watch(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        BiConsumer<T, T> consumer = (_, newValue) -> listener.accept(newValue);
        return subscriptions.add(consumer);
    }

    /**
     * Subscribes to value changes with access to old and new values.
     */
    public Disposable watch(BiConsumer<T, T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        return subscriptions.add(listener);
    }

    @Override
    public void onDependencyChanged() {
        log.debug("Dependency changed, marking ref as dirty...");

        // Atomically set the dirty flag. If we are the thread that successfully
        // changed it from false to true, then we are responsible for notifying dependents.
        if (dirty.compareAndSet(false, true)) {
            // Notify other signals that depend on this ComputedRef.
            // Direct subscribers will be notified later, only if the value actually changes
            // during recomputation.
            // The main purpose here is to have the coordinator notify the dependency graph.
            dependentNotifier.notifyDependents();

            if (!lazy || subscriptions.hasSubscriptions()) {
                // Submit a task to call .get(), which will safely trigger the recomputation
                // on a background thread without blocking the current one.
                submitTask(this::get);
            }
        }
    }

    public void invalidate() {
        log.debug("Invalidating ref...");
        if (dirty.compareAndSet(false, true)) {
            dependentNotifier.notifyDependents();
        }
    }

    private T recompute() {
        // Acquire a read lock first to check the state again. This is more efficient
        // than immediately acquiring a write lock if another thread just finished.
        computationLock.readLock().lock();
        try {
            if (!dirty.get()) {
                return cachedValue.get();
            }
        } finally {
            computationLock.readLock().unlock();
        }

        // If we got here, the value is still dirty. We must acquire the write lock to compute.
        computationLock.writeLock().lock();
        try {
            // Double-check inside the write lock, as another thread might have recomputed
            // while we were waiting for the lock.
            if (!dirty.get()) {
                return cachedValue.get();
            }

            // Check for and prevent circular dependencies.
            if (!computing.compareAndSet(false, true)) {
                log.error("Circular dependency detected in ref. Computation is already in progress.");
                throw new IllegalStateException("Circular dependency detected in ComputedRef.");
            }

            log.debug("Computing value...");

            T newValue;
            try {
                tracker.startTracking(this);
                newValue = computation.get();
            } finally {
                // CRITICAL: Always release the computing flag and stop tracking.
                tracker.stopTracking();
                computing.set(false);
            }

            // Atomically set the new value and get the old one back for comparison.
            T oldValue = cachedValue.getAndSet(newValue);

            // The computation is done, so the value is no longer dirty.
            dirty.set(false);

            log.debug("Computed value: {}", newValue);

            // If the value actually changed, notify all subscribers.
            if (!Objects.equals(oldValue, newValue)) {
                log.debug("Value changed from `{}` to `{}`, notifying dependents and subscribers...", oldValue, newValue);
                notifyDependents(oldValue, newValue);
            }

            return newValue;
        } finally {
            computationLock.writeLock().unlock();
        }
    }

    private void notifyDependents(T oldValue, T newValue) {
        dependentNotifier.notifyDependents(() ->
                subscriptions.notify(listener -> listener.accept(oldValue, newValue))
        );
    }

    @Override
    public String getName() {
        return String.format("ComputedRef@%s", Integer.toHexString(hashCode()));
    }

    @Override
    public String toString() {
        return getName() + "{cachedValue=" + cachedValue.get() + ", isDirty=" + dirty.get() + "}";
    }

}
