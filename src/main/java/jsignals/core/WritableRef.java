package jsignals.core;

import java.util.function.Function;

/**
 * A reactive reference that can be written to.
 */
public interface WritableRef<T> extends ReadableRef<T> {
    /**
     * Sets a new value and triggers updates to dependents.
     */
    void set(T value);

    /**
     * Updates the value using a function and triggers updates.
     */
    void update(Function<T, T> updater);
}
