package jsignals.core;

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

}
