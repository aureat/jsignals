package jsignals.core;

/**
 * Represents an operation that accepts two input arguments and returns no result.
 */
@FunctionalInterface
public interface BiConsumer<T, U> {
    void accept(T t, U u);
}
