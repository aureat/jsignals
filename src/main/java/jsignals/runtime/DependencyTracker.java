package jsignals.runtime;

import jsignals.util.JSignalsLogger;
import org.slf4j.Logger;

import java.lang.ref.WeakReference;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks dependencies between reactive values and their dependents.
 */
public class DependencyTracker {

    private static final DependencyTracker INSTANCE = new DependencyTracker();

    private final ThreadLocal<Deque<ComputationContext>> contextStack = ThreadLocal.withInitial(ArrayDeque::new);

    // Thread-local to track the current computation being executed
//    private final ThreadLocal<ComputationContext> currentContext = new ThreadLocal<>();

    // Maps dependencies to their dependents
    private final ConcurrentHashMap<Object, Set<WeakReference<Dependent>>> dependentsMap =
            new ConcurrentHashMap<>();

    // Maps dependents to their current dependencies (for cleanup)
    private final ConcurrentHashMap<Dependent, Set<Object>> dependentToDepsMap =
            new ConcurrentHashMap<>();

    private static final Logger log = JSignalsLogger.getLogger(DependencyTracker.class);

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

        // Push a new context onto the stack
        ComputationContext context = new ComputationContext(dependent);
        contextStack.get().push(context);
    }

    /**
     * Stops tracking dependencies for the current computation.
     */
    public Set<Object> stopTracking() {
        Deque<ComputationContext> stack = contextStack.get();
        if (stack.isEmpty()) {
            log.error("No computation context to stop tracking for.");
            return Collections.emptySet();
        }

        ComputationContext context = stack.pop();
        if (context == null) {
            log.error("Null computation context found when stopping tracking.");
            return Collections.emptySet();
        }

        Set<Object> newDeps = context.getDependencies();
        Dependent dependent = context.getDependent();

        // Update the dependent's dependency set
        dependentToDepsMap.put(dependent, newDeps);

        return newDeps;
    }

    /**
     * Records that the current computation accessed a dependency.
     */
    public void trackAccess(Object dependency) {
        Deque<ComputationContext> stack = contextStack.get();
        if (stack.isEmpty()) {
            log.warn("No computation context found when tracking access to dependency: {}", dependency);
            return;
        }

        ComputationContext context = stack.peek();
        log.trace("Tracking access to dependency {}. Current context: {}", dependency, context);

        if (context == null) {
            log.warn("Null computation context found when tracking access to dependency: {}", dependency);
            return;
        }

        context.addDependency(dependency);

        // Register this computation as a dependent of the accessed value
        Set<WeakReference<Dependent>> dependents = dependentsMap.computeIfAbsent(
                dependency,
                k -> ConcurrentHashMap.newKeySet()
        );

        // Add the current dependent
        Dependent currentDependent = context.getDependent();

        // Check if already registered
        for (WeakReference<Dependent> ref : dependents) {
            if (ref.get() == currentDependent) {
                return; // Already registered.
            }
        }
        dependents.add(new WeakReference<>(currentDependent));
    }

    /**
     * Notifies all dependents that a dependency has changed.
     */
    public void notifyDependents(Object dependency) {
        Set<WeakReference<Dependent>> dependents = dependentsMap.get(dependency);

        var dependentList = dependents != null ? dependents.stream()
                .map(WeakReference::get)
                .filter(Objects::nonNull)
                .map(Dependent::getName)
                .toList() : Collections.emptyList();
        log.trace("Notifying dependents for dependency {}. Current dependents: {}", dependency, dependentList);

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
                    log.debug("Notifying dependent {}", dependent.getName());
                    dependent.onDependencyChanged();
                } catch (Exception e) {
                    log.error("Error notifying dependent {}: {}", dependent.getId(), e.getMessage(), e);
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

        @Override
        public String toString() {
            return "ComputationContext" + "@" + Integer.toHexString(System.identityHashCode(this)) + "{" +
                    "dependent=" + dependent.getName() +
                    ", dependencies=" + dependencies +
                    '}';
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
        default int getId() {
            return System.identityHashCode(this);
        }

        default String getName() {
            return "Dependent@" + Integer.toHexString(getId());
        }

    }

}
