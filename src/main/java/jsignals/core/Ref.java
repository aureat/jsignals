package jsignals.core;

import jsignals.runtime.DependencyTracker.Dependent;
import jsignals.util.JSignalsLogger;
import jsignals.util.WeakLRUCache;
import org.slf4j.Logger;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A reactive reference holding a value of type T.
 * Thread-safe implementation supporting concurrent reads and writes.
 */
public class Ref<T> implements WritableRef<T>, Dependent {

    private final AtomicReference<T> value;
    private final DependentNotifier dependentNotifier;
    private final SubscriptionNotifier<BiConsumer<T, T>> subscriptions;

    private final Logger log = JSignalsLogger.getLogger(getName());

    /**
     * Creates a new Ref with an initial value.
     */
    public Ref(T initialValue) {
        this.value = new AtomicReference<>(initialValue);
        this.dependentNotifier = new DependentNotifier(this);
        this.subscriptions = new SubscriptionNotifier<>();
    }

    /**
     * Creates a new Ref with null initial value.
     */
    public Ref() {
        this(null);
    }

    @Override
    public T get() {
        dependentNotifier.trackAccess();
        return value.get();
    }

    @Override
    public T getValue() {
        return value.get();
    }

    @Override
    public void set(T newValue) {
        T oldValue = value.getAndSet(newValue);

        // Notify dependents only if the value has changed
        if (!Objects.equals(oldValue, newValue)) {
            notifyDependents(oldValue, newValue);
        }
    }

    @Override
    public void update(Function<T, T> updater) {
        Objects.requireNonNull(updater, "Updater function cannot be null");

        T oldValue;
        T newValue;

        do {
            oldValue = value.get();
            newValue = updater.apply(oldValue);
        } while (!value.compareAndSet(oldValue, newValue));

        // Notify dependents only if the value has changed
        if (!Objects.equals(oldValue, newValue)) {
            notifyDependents(oldValue, newValue);
        }
    }

    @Override
    public void onDependencyChanged() {
        // This method is called by the DependencyTracker when a dependency changes.
        // In this case, we do not need to do anything here because Ref does not have dependencies.
        // All changes are handled through set() or update() methods.
        log.trace("Dependency changed, but Ref does not track dependencies directly.");
    }

    /**
     * Notifies all dependents that the value has changed.
     * This method is called internally when the value is set or updated.
     * It ensures that all subscribers are notified of the change.
     *
     * @param oldValue The previous value
     * @param newValue The new value
     */
    void notifyDependents(final T oldValue, final T newValue) {
        log.debug("Value changed from {} to {}, notifying dependents...", oldValue, newValue);
        dependentNotifier.notifyDependents(() ->
                subscriptions.notify(listener -> listener.accept(oldValue, newValue))
        );
    }

    /**
     * Subscribes to value changes.
     *
     * @param listener Called when the value changes
     * @return A Disposable to unsubscribe
     */
    public Disposable watch(Consumer<T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        BiConsumer<T, T> listenerAdapter = (_, newValue) -> listener.accept(newValue);
        return subscriptions.add(listenerAdapter);
    }

    /**
     * Subscribes to value changes with access to old and new values.
     */
    public Disposable watch(BiConsumer<T, T> listener) {
        Objects.requireNonNull(listener, "Listener cannot be null");
        return subscriptions.add(listener);
    }

    public <U> U with(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return mapper.apply(get());
    }

    public <U> U withValue(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return mapper.apply(getValue());
    }

    public <U> ComputedRef<U> map(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return new ComputedRef<>(() -> mapper.apply(get()));
    }

    public <U> ComputedRef<U> flatMap(Function<? super T, ? extends ReadableRef<U>> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");

        final WeakLRUCache<T, ReadableRef<U>> cache = new WeakLRUCache<>();

        // Create a ComputedRef that performs the "flattening".
        return new ComputedRef<>(() -> {
            // 1. Get the current value of the outer Ref. This establishes a dependency.
            //    When this outer Ref changes, this whole computation will re-run.
            T outerValue = this.get();

            // 2. Apply the mapper to get the currently active inner Ref.
            ReadableRef<U> innerRef = cache.computeIfAbsent(outerValue, mapper);

            // 3. Get the value of that inner Ref. This establishes a second, dynamic dependency.
            //    When the *inner* Ref changes, this computation will also re-run.
            return innerRef.get();
        });
    }

    public Ref<T> copy() {
        return new Ref<>(value.get());
    }

    public <U> Ref<U> copy(Function<T, U> mapper) {
        Objects.requireNonNull(mapper, "Mapper function cannot be null");
        return new Ref<>(mapper.apply(value.get()));
    }

    public String getName() {
        return "Ref@" + Integer.toHexString(getId());
    }

    @Override
    public String toString() {
        return getName() + "{" +
                "value=" + value.get() +
                '}';
    }

}
