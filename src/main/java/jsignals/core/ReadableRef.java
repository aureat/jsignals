package jsignals.core;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * A reactive reference that can be read.
 */
public interface ReadableRef<T> extends BaseRef {

    /**
     * Gets the current value and tracks this access for reactivity.
     */
    T get();

    /**
     * Gets the current value without tracking (peek).
     */
    T getValue();

    /**
     * Watches for changes to the value and executes the listener when it changes.
     * The listener will be called with the new value.
     *
     * @param listener The consumer that will be called with the new value.
     * @return A disposable that can be used to unsubscribe from changes.
     */
    Disposable watch(Consumer<T> listener);

    /**
     * @param listener The bi-consumer that will be called with the old and new values.
     * @see ReadableRef#watch(Consumer)
     */
    Disposable watch(BiConsumer<T, T> listener);

}
