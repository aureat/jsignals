package jsignals.core;

import jsignals.runtime.DependencyTracker;

import java.util.Objects;

/**
 * Coordinates notification processes, handling re-entrancy and dependency tracking.
 * This class is intended to be used by a reactive source (e.g., Ref, Trigger).
 */
public final class DependentNotifier {

    private final Object source;

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final Object notificationLock = new Object();

    private volatile boolean isNotifying = false;

    /**
     * Creates a DependentNotifier for a specific reactive source.
     *
     * @param source The source object (e.g., the Ref or Trigger instance) that owns this coordinator.
     */
    public DependentNotifier(Object source) {
        this.source = Objects.requireNonNull(source, "Notification source cannot be null");
    }

    /**
     * Tracks an access to the reactive source. This should be called whenever the source's
     * value is read within a reactive computation context.
     */
    public void trackAccess() {
        tracker.trackAccess(source);
    }

    /**
     * Safely executes a notification action, guarding against re-entrancy.
     *
     * @param notificationAction A {@link Runnable} that performs the actual notification
     *                           to the direct subscribers of the source (e.g., calling listeners).
     *                           This action will only be executed if a notification for this
     *                           source is not already in progress. After this action completes,
     *                           the global dependency tracker is notified.
     */
    public void notifyDependents(Runnable notificationAction) {
        Objects.requireNonNull(notificationAction, "Direct notification action cannot be null.");

        // An early check without a lock. This is a minor optimization for the common case
        // where there is no contention and no notification is in progress.
        if (isNotifying) {
            return;
        }

        synchronized (notificationLock) {
            // Re-check inside the synchronized block to ensure correctness under contention.
            if (isNotifying) {
                return;
            }
            isNotifying = true;
        }

        try {
            notificationAction.run(); // Notify the direct subscribers first
            tracker.notifyDependents(source); // After direct subscribers are handled, notify dependents in the graph.
        } finally {
            // The state must be reset to allow future notifications.
            // This is not synchronized, as we only ever care about setting it back to false.
            isNotifying = false;
        }
    }

    /**
     * Notifies dependents without any direct subscribers.
     * This is useful when the source has changed but there are no direct listeners to notify.
     * It will still trigger the dependency graph to update.
     */
    public void notifyDependents() {
        notifyDependents(() -> {
            // This is a no-op action, used when we just want to notify dependents
            // without any direct subscribers.
        });
    }

    /**
     * Checks if this coordinator is currently in the process of notifying.
     *
     * @return true if notifying, false otherwise.
     */
    public boolean isNotifying() {
        synchronized (notificationLock) {
            return isNotifying;
        }
    }

}