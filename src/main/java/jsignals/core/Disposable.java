package jsignals.core;

/**
 * Represents a subscription or effect that can be disposed.
 */
@FunctionalInterface
public interface Disposable {

    /**
     * Disposes the subscription or stops the effect.
     */
    void dispose();

    /**
     * Creates a composite disposable from multiple disposables.
     */
    static Disposable composite(Disposable... disposables) {
        return () -> {
            for (Disposable d : disposables) {
                if (d != null) {
                    d.dispose();
                }
            }
        };
    }

}
