package jsignals.core;

import jsignals.runtime.DependencyTracker;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reactive reference holding a value of type T.
 * Thread-safe implementation supporting concurrent reads and writes.
 */
public class Ref<T> implements WritableRef<T> {

    private final AtomicReference<T> value;

    private final CopyOnWriteArrayList<RefSubscription<T>> subscriptions;

    private final Object notificationLock = new Object();

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    // TODO: Package-private for now, will integrate with DependencyTracker later
    volatile boolean isNotifying = false;

    /**
     * Creates a new Ref with an initial value.
     */
    public Ref(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
        this.subscriptions = new CopyOnWriteArrayList<>();
    }

    /**
     * Creates a new Ref with null initial value.
     */
    public Ref() {
        this(null);
    }

    @Override
    public T get() {
        // Track dependency access here
        tracker.trackAccess(this);
        return value.get();
    }

    @Override
    public T getValue() {
        // Non-tracking access
        return value.get();
    }

    @Override
    public void set(T newValue) {
        T oldValue = value.getAndSet(newValue);

        // Only notify if value actually changed
        if (!Objects.equals(oldValue, newValue)) {
            notifySubscribers(oldValue, newValue);
        }
    }

    @Override
    public void update(Function<T, T> updater) {
        Objects.requireNonNull(updater, "Updater function cannot be null");

        T oldValue;
        T newValue;

        // Atomic update
        do {
            oldValue = value.get();
            newValue = updater.apply(oldValue);
        } while (!value.compareAndSet(oldValue, newValue));

        // Notify if changed
        if (!Objects.equals(oldValue, newValue)) {
            notifySubscribers(oldValue, newValue);
        }
    }

    /**
     * Subscribes to value changes.
     *
     * @param listener Called when the value changes
     * @return A Disposable to unsubscribe
     */
    public Disposable watch(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        RefSubscription<T> subscription = new RefSubscription<>(listener, this);
        subscriptions.add(subscription);

        return subscription;
    }

    /**
     * Subscribes to value changes with access to old and new values.
     */
    public Disposable watch(BiConsumer<T, T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        RefSubscription<T> subscription = new RefSubscription<>(
                newValue -> listener.accept(getValue(), newValue),
                this
        );
        subscriptions.add(subscription);

        return subscription;
    }

    private void notifySubscribers(T oldValue, T newValue) {
        synchronized (notificationLock) {
            if (isNotifying) {
                // Prevent recursive notifications
                return;
            }
            isNotifying = true;
        }

        try {
            // Clean up disposed subscriptions first
            subscriptions.removeIf(RefSubscription::isDisposed);

            // Notify all active subscribers
            for (RefSubscription<T> subscription : subscriptions) {
                try {
                    subscription.notifySubscription(newValue);
                } catch (Exception e) {
                    // Log error but continue notifying others
                    System.err.println("Error in subscription: " + e.getMessage());
                }
            }

            // Notify the dependency tracker
            tracker.notifyDependents(this);

        } finally {
            synchronized (notificationLock) {
                isNotifying = false;
            }
        }
    }

    void removeSubscription(RefSubscription<T> subscription) {
        subscriptions.remove(subscription);
    }

    @Override
    public String toString() {
        return "Ref(" + getValue() + ")";
    }

    /**
     * Internal subscription class
     */
    private static class RefSubscription<T> implements Disposable {

        private final Consumer<T> listener;

        private final Ref<T> ref;

        private volatile boolean disposed = false;

        RefSubscription(Consumer<T> listener, Ref<T> ref) {
            this.listener = listener;
            this.ref = ref;
        }

        void notifySubscription(T value) {
            if (!disposed) {
                listener.accept(value);
            }
        }

        boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            if (!disposed) {
                disposed = true;
                ref.removeSubscription(this);
            }
        }

    }

}
