package jsignals.core.support;

import jsignals.runtime.DependencyTracker;

import java.util.Objects;

/**
 * Coordinates notification processes, handling re-entrancy and dependency tracking.
 * This class is intended to be used by a reactive source (e.g., Ref, Trigger).
 */
public class NotificationCoordinator {

    private final Object source; // The reactive component (Ref, Trigger) this coordinator is for

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    private final Object notificationLock = new Object();

    private volatile boolean isCurrentlyNotifying = false;

    /**
     * Creates a NotificationCoordinator for a specific reactive source.
     *
     * @param source The source object (e.g., the Ref or Trigger instance) that owns this coordinator.
     */
    public NotificationCoordinator(Object source) {
        this.source = Objects.requireNonNull(source, "Notification source cannot be null");
    }

    /**
     * Tracks an access to the reactive source. Call this when the source's state is read.
     */
    public void trackAccess() {
        tracker.trackAccess(source);
    }

    /**
     * Coordinates the notification process.
     * It ensures that notifications are not re-entrant for this source and
     * notifies the DependencyTracker after direct subscribers have been handled.
     *
     * @param directNotificationAction A Runnable that performs the actual notification
     *                                 to the direct subscribers of the source.
     */
    public void coordinateNotification(Runnable directNotificationAction) {
        Objects.requireNonNull(directNotificationAction, "Direct notification action cannot be null");

        boolean proceedWithNotification = false;
        synchronized (notificationLock) {
            if (!isCurrentlyNotifying) {
                isCurrentlyNotifying = true;
                proceedWithNotification = true;
            }
        }

        if (!proceedWithNotification) {
            return; // Notification already in progress for this source, prevent re-entrancy
        }

        try {
            // 1. Perform the action that notifies direct subscribers
            directNotificationAction.run();

            // 2. Notify dependents in the broader dependency graph
            tracker.notifyDependents(source);
        } finally {
            // Ensure the flag is reset even if an exception occurs
            synchronized (notificationLock) {
                isCurrentlyNotifying = false;
            }
        }
    }

    /**
     * Checks if this coordinator is currently in the process of notifying.
     *
     * @return true if notifying, false otherwise.
     */
    public boolean isNotifying() {
        synchronized (notificationLock) {
            return isCurrentlyNotifying;
        }
    }

}