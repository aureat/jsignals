package jsignals.core;

import jsignals.runtime.DependencyTracker;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents a trigger in the reactive system.
 * This class can be extended to implement specific trigger behaviors.
 */
public class TriggerRef implements TrackableRef {

    private final CopyOnWriteArrayList<TriggerSubscription> subscriptions;

    private final Object notificationLock = new Object();

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    // TODO: Package-private for now, will integrate with DependencyTracker later
    volatile boolean isNotifying = false;

    public TriggerRef() {
        this.subscriptions = new CopyOnWriteArrayList<>();
    }

    public void track() {
        tracker.trackAccess(this);
    }

    public void trigger() {
        notifySubscribers();
    }

    public Disposable subscribe(Runnable listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");

        TriggerSubscription subscription = new TriggerSubscription(listener, this);
        subscriptions.add(subscription);

        return subscription;
    }

    private void notifySubscribers() {
        synchronized (notificationLock) {
            if (isNotifying) {
                // Prevent recursive notifications
                return;
            }
            isNotifying = true;
        }

        try {
            // Clean up disposed subscriptions first
            subscriptions.removeIf(TriggerSubscription::isDisposed);

            // Notify all active subscribers
            for (TriggerSubscription subscription : subscriptions) {
                try {
                    subscription.notifySubscription();
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

    void removeSubscription(TriggerSubscription subscription) {
        subscriptions.remove(subscription);
    }

    @Override
    public String toString() {
        return "TriggerRef()";
    }


    private static class TriggerSubscription implements Disposable {

        private final Runnable listener;

        private final TriggerRef ref;

        private volatile boolean disposed = false;

        TriggerSubscription(Runnable listener, TriggerRef ref) {
            this.listener = listener;
            this.ref = ref;
        }

        void notifySubscription() {
            if (!disposed) {
                listener.run();
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
