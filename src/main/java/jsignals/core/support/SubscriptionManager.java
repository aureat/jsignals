package jsignals.core.support;

import jsignals.core.Disposable;

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
public class SubscriptionManager<LISTENER_TYPE> {

    private final CopyOnWriteArrayList<ManagedSubscription<LISTENER_TYPE>> subscriptions = new CopyOnWriteArrayList<>();

    public SubscriptionManager() { }

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
    void remove(ManagedSubscription<LISTENER_TYPE> subscription) {
        subscriptions.remove(subscription);
    }

    /**
     * Notifies all active subscribers.
     *
     * @param invokerAction A Consumer that defines how to invoke each listener.
     *                      For example, for Consumer<String> listeners, this could be `listener -> listener.accept("hello")`.
     *                      For Runnable listeners, this could be `listener -> listener.run()`.
     */
    public void notifyAll(Consumer<LISTENER_TYPE> invokerAction) {
        Objects.requireNonNull(invokerAction, "Invoker action cannot be null");

        // Clean up any subscriptions that were disposed but perhaps not yet removed
        // (e.g., if dispose() was called directly on a ManagedSubscription instance that
        // somehow didn't complete its removal from the manager immediately).
        // CopyOnWriteArrayList's iterator is safe, but removeIf can be costly if called excessively.
        // For many use cases, cleaning here is fine.
        subscriptions.removeIf(ManagedSubscription::isDisposed);

        for (ManagedSubscription<LISTENER_TYPE> subscription : subscriptions) {
            try {
                subscription.invokeListener(invokerAction);
            } catch (Exception e) {
                // Consider a configurable error handler strategy
                System.err.println("Error during subscriber notification: " + e.getMessage());
                e.printStackTrace(); // For more detailed debugging
            }
        }
    }

    /**
     * Represents a single managed subscription.
     */
    private static class ManagedSubscription<L> implements Disposable {

        private final L actualListener;

        private final SubscriptionManager<L> manager; // To call remove upon disposal

        private volatile boolean disposed = false;

        ManagedSubscription(L actualListener, SubscriptionManager<L> manager) {
            this.actualListener = actualListener;
            this.manager = manager;
        }

        /**
         * Invokes the listener if not disposed.
         *
         * @param invokerAction The action that knows how to call the listener.
         */
        void invokeListener(Consumer<L> invokerAction) {
            if (!disposed) {
                invokerAction.accept(actualListener);
            }
        }

        public boolean isDisposed() {
            return disposed;
        }

        @Override
        public void dispose() {
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