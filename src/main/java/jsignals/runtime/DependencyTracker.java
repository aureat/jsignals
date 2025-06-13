package jsignals.runtime;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks dependencies between reactive values and their dependents.
 */
public class DependencyTracker {

    private static final DependencyTracker INSTANCE = new DependencyTracker();

    // Thread-local to track the current computation being executed
    private final ThreadLocal<ComputationContext> currentContext = new ThreadLocal<>();

    // Maps dependencies to their dependents
    private final ConcurrentHashMap<Object, Set<WeakReference<Dependent>>> dependentsMap =
            new ConcurrentHashMap<>();

    // Maps dependents to their current dependencies (for cleanup)
    private final ConcurrentHashMap<Dependent, Set<Object>> dependentToDepsMap =
            new ConcurrentHashMap<>();

    private DependencyTracker() { }

    public static DependencyTracker getInstance() {
        return INSTANCE;
    }

    public void registerDependency(Dependent dependent, Object dependency) {
        dependentsMap.computeIfAbsent(dependency, k -> ConcurrentHashMap.newKeySet())
                .add(new WeakReference<>(dependent));
        dependentToDepsMap.computeIfAbsent(dependent, k -> new HashSet<>())
                .add(dependency);
    }

    /**
     * Starts tracking dependencies for a computation.
     */
    public void startTracking(Dependent dependent) {
        // Clean up old dependencies first
        cleanupDependent(dependent);

        ComputationContext context = new ComputationContext(dependent);
        currentContext.set(context);
    }

    /**
     * Stops tracking dependencies for the current computation.
     */
    public Set<Object> stopTracking() {
        ComputationContext context = currentContext.get();
        currentContext.remove();

        if (context != null) {
            Set<Object> newDeps = context.getDependencies();
            Dependent dependent = context.getDependent();

            // Update the dependent's dependency set
            dependentToDepsMap.put(dependent, newDeps);

            return newDeps;
        }

        return Collections.emptySet();
    }

    /**
     * Records that the current computation accessed a dependency.
     */
    public void trackAccess(Object dependency) {
        ComputationContext context = currentContext.get();
        if (context != null) {
            context.addDependency(dependency);

            // Register this computation as a dependent of the accessed value
            Set<WeakReference<Dependent>> dependents = dependentsMap.computeIfAbsent(
                    dependency,
                    k -> ConcurrentHashMap.newKeySet()
            );

            // Add the current dependent
            Dependent currentDependent = context.getDependent();

            // Check if already registered
            boolean found = false;
            for (WeakReference<Dependent> ref : dependents) {
                Dependent dep = ref.get();
                if (dep == currentDependent) {
                    found = true;
                    break;
                }
            }

            if (!found) {
                dependents.add(new WeakReference<>(currentDependent));
            }
        }
    }

    /**
     * Notifies all dependents that a dependency has changed.
     */
    public void notifyDependents(Object dependency) {
        Set<WeakReference<Dependent>> dependents = dependentsMap.get(dependency);
        if (dependents != null) {
            // Create a copy to avoid concurrent modification
            List<Dependent> activeDependents = new ArrayList<>();

            // Collect active dependents and clean up stale references
            Iterator<WeakReference<Dependent>> it = dependents.iterator();
            while (it.hasNext()) {
                WeakReference<Dependent> ref = it.next();
                Dependent dependent = ref.get();
                if (dependent != null) {
                    activeDependents.add(dependent);
                } else {
                    it.remove();
                }
            }

            // Notify all active dependents
            for (Dependent dependent : activeDependents) {
                try {
//                    System.out.println("Notifying dependent: " + dependent.getId());
                    dependent.onDependencyChanged();
                } catch (Exception e) {
                    System.err.println("Error notifying dependent: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Removes a dependent from all its dependencies.
     */
    void cleanupDependent(Dependent dependent) {
        Set<Object> oldDeps = dependentToDepsMap.remove(dependent);
        if (oldDeps != null) {
            for (Object dep : oldDeps) {
                Set<WeakReference<Dependent>> dependents = dependentsMap.get(dep);
                if (dependents != null) {
                    dependents.removeIf(ref -> {
                        Dependent d = ref.get();
                        return d == null || d == dependent;
                    });
                }
            }
        }
    }

    /**
     * Context for tracking dependencies during a computation.
     */
    private static class ComputationContext {

        private final Dependent dependent;

        private final Set<Object> dependencies = new HashSet<>();

        ComputationContext(Dependent dependent) {
            this.dependent = dependent;
        }

        void addDependency(Object dependency) {
            dependencies.add(dependency);
        }

        Set<Object> getDependencies() {
            return new HashSet<>(dependencies);
        }

        Dependent getDependent() {
            return dependent;
        }

    }

    /**
     * Interface for objects that depend on reactive values.
     */
    public interface Dependent {

        /**
         * Called when a dependency of this dependent has changed.
         */
        void onDependencyChanged();

        /**
         * Gets a unique identifier for this dependent.
         */
        default Object getId() {
            return this;
        }

    }

}
