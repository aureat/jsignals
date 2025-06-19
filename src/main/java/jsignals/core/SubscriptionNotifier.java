package jsignals.core;

import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Manages a list of subscriptions of a specific listener type.
 * It handles adding, removing (via Disposable), and notifying listeners.
 * Thread-safe for managing subscriptions.
 *
 * @param <LISTENER_TYPE> The functional interface type of the listeners (e.g., Runnable, Consumer<T>).
 */
public final class SubscriptionNotifier<LISTENER_TYPE> {

    private final CopyOnWriteArrayList<ManagedSubscription<LISTENER_TYPE>> subscriptions = new CopyOnWriteArrayList<>();

    public SubscriptionNotifier() { }

    /**
     * Adds a listener and returns a Disposable to manage its lifecycle.
     *
     * @param listener The listener to add.
     * @return A Disposable to unsubscribe the listener.
     */
    public Disposable add(LISTENER_TYPE listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        ManagedSubscription<LISTENER_TYPE> subscription = new ManagedSubscription<>(listener, this);
        subscriptions.add(subscription);
        return subscription;
    }

    /**
     * Removes a specific subscription. Called by ManagedSubscription when disposed.
     *
     * @param subscription The subscription to remove.
     */
    private void remove(ManagedSubscription<LISTENER_TYPE> subscription) {
        subscriptions.remove(subscription);
    }

    public boolean isEmpty() {
        return subscriptions.isEmpty();
    }

    public int size() {
        return subscriptions.size();
    }

    public boolean hasSubscriptions() {
        return !subscriptions.isEmpty();
    }

    /**
     * Notifies all active subscribers.
     *
     * @param invokerAction A {@link Consumer} that defines how to invoke each listener. This design
     *                      provides flexibility, allowing the caller to pass arguments to the
     *                      listeners. For example, for a {@code Consumer<String>} listener, the
     *                      invoker action would be {@code listener -> listener.accept("someValue")}.
     */
    public void notify(Consumer<LISTENER_TYPE> invokerAction) {
        Objects.requireNonNull(invokerAction, "Invoker action cannot be null.");

        // The iterator of CopyOnWriteArrayList is a snapshot, so it's inherently safe from
        // concurrent modifications. No need to manually clean up disposed items here; the
        // `dispose` method handles removal.
        for (ManagedSubscription<LISTENER_TYPE> subscription : subscriptions) {
            try {
                subscription.invoke(invokerAction);
            } catch (Exception e) {
                // It's crucial to catch exceptions from individual listeners to prevent
                // one faulty listener from stopping the entire notification chain.
                // Consider replacing with a configurable error handler.
                System.err.println("JSignal Notification Error: A subscriber threw an exception.");
                e.printStackTrace(System.err);
            }
        }
    }

    /**
     * Represents a single managed subscription.
     */
    private static class ManagedSubscription<L> implements Disposable {

        private final L actualListener;

        private final SubscriptionNotifier<L> manager; // To call remove upon disposal

        private volatile boolean disposed = false;

        ManagedSubscription(L actualListener, SubscriptionNotifier<L> manager) {
            this.actualListener = actualListener;
            this.manager = manager;
        }

        /**
         * Invokes the listener if not disposed.
         *
         * @param invokerAction The action that knows how to call the listener.
         */
        void invoke(Consumer<L> invokerAction) {
            if (!disposed) {
                invokerAction.accept(actualListener);
            }
        }

        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
            // The volatile 'disposed' flag ensures that even if multiple threads call dispose(),
            // the removal logic runs only once.
            if (!disposed) {
                disposed = true;
                // Remove from the manager's list.
                // This ensures that once disposed, it won't be notified further
                // and allows the manager to clean up its reference.
                manager.remove(this);
            }
        }

    }

}