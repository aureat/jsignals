package jsignals.runtime;

import jsignals.core.Disposable;
import jsignals.runtime.DependencyTracker.Dependent;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages reactive effects (side effects that re-run when dependencies change).
 */
public class EffectRunner {

    private final DependencyTracker tracker = DependencyTracker.getInstance();

    /**
     * Runs an effect that will automatically re-run when its dependencies change.
     */
    public Disposable runEffect(Runnable effect) {
        Objects.requireNonNull(effect, "Effect cannot be null");

        EffectHandle handle = new EffectHandle(effect);

        // Run the effect immediately
        handle.run();

        return handle;
    }

    /**
     * Internal class representing an active effect.
     */
    private class EffectHandle implements Disposable, Dependent {

        private final Runnable effect;

        private final AtomicBoolean disposed = new AtomicBoolean(false);

        private volatile Set<Object> currentDependencies = Set.of();

        EffectHandle(Runnable effect) {
            this.effect = effect;
        }

        void run() {
            if (disposed.get()) {
                return;
            }

            // Start tracking dependencies
            tracker.startTracking(this);

            try {
                // Run the effect
                effect.run();

                // Get the dependencies that were accessed
                currentDependencies = tracker.stopTracking();
            } catch (Exception e) {
                // Make sure we stop tracking even on error
                tracker.stopTracking();
                throw new RuntimeException("Error in effect", e);
            }
        }

        @Override
        public void onDependencyChanged() {
            // Re-run the effect when dependencies change
            if (!disposed.get()) {
                run();
            }
        }

        @Override
        public void dispose() {
            if (disposed.compareAndSet(false, true)) {
                // Clean up our registrations
                for (Object dep : currentDependencies) {
                    if (dep instanceof Disposable disposable) {
                        disposable.dispose();
                    } else if (dep instanceof Dependent dependent) {
                        dependent.onDependencyChanged();
                    }
                }
            }
        }

        @Override
        public String getName() {
            return "EffectHandle@" + Integer.toHexString(hashCode());
        }

    }

}
