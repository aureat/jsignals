package jsignals.core;

import jsignals.runtime.DependencyTracker;
import jsignals.runtime.DependencyTracker.Dependent;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Supplier;

/**
 * A computed reactive value that automatically updates when its dependencies change.
 * Thread-safe and lazily evaluated.
 */
public class ComputedRef<T> implements ReadableRef<T>, Dependent {

    private final Supplier<T> computation;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private volatile T cachedValue;

    private volatile boolean isDirty = true;

    private volatile Set<Object> currentDependencies = Set.of();

    private volatile boolean isComputing = false;

    /**
     * Creates a new computed value with the given computation.
     */
    public ComputedRef(Supplier<T> computation) {
        this.computation = Objects.requireNonNull(computation, "Computation cannot be null");
    }

    @Override
    public T get() {
        // Track access to this computed value
        tracker.trackAccess(this);

        // Check if we need to recompute
        if (isDirty) {
            recompute();
        }

        return cachedValue;
    }

    @Override
    public T getValue() {
        // Non-tracking access
        lock.readLock().lock();
        try {
            if (isDirty) {
                lock.readLock().unlock();
                recompute();
                lock.readLock().lock();
            }
            return cachedValue;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void onDependencyChanged() {
        // Mark as dirty when a dependency changes
        lock.writeLock().lock();
        try {
            isDirty = true;
            // TODO: Notify our own dependents
            tracker.notifyDependents(this);
        } finally {
            lock.writeLock().unlock();
        }
    }

    private void recompute() {
        lock.writeLock().lock();
        try {
            // Double-check in case another thread already recomputed
            if (!isDirty) {
                return;
            }

            // Prevent circular dependencies
            if (isComputing) {
                throw new IllegalStateException(
                        "Circular dependency detected in computed value"
                );
            }

            isComputing = true;

            try {
                // Start tracking dependencies for this computation
                tracker.startTracking(this);

                try {
                    // Run the computation
                    T newValue = computation.get();

                    // Update our dependency set
                    currentDependencies = tracker.stopTracking();

                    // Update cached value
                    cachedValue = newValue;
                    isDirty = false;

                } catch (Exception e) {
                    // Make sure we stop tracking even on error
                    tracker.stopTracking();
                    throw new RuntimeException("Error in computed value", e);
                }
            } finally {
                isComputing = false;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    /**
     * Forces the computed value to recompute on next access.
     */
    public void invalidate() {
        lock.writeLock().lock();
        try {
            isDirty = true;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public String toString() {
        return "ComputedRef()";
    }

}
