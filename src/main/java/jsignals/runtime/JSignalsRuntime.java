package jsignals.runtime;

/**
 * Manages the lifecycle of shared resources for the JSignals library,
 * primarily the executor service.
 * <p>
 * This class is AutoCloseable, allowing for easy resource cleanup using a try-with-resources block.
 */
public final class JSignalsRuntime implements AutoCloseable {

    private final JSignalsExecutor executor;

    /**
     * Creates a new runtime, initializing all necessary services.
     */
    public JSignalsRuntime() {
        this.executor = new JSignalsExecutor();
    }

    /**
     * Gets the executor service managed by this runtime.
     *
     * @return The active JSignalsExecutor instance.
     */
    public JSignalsExecutor getExecutor() {
        return executor;
    }

    /**
     * Shuts down all services managed by this runtime.
     * Implements the AutoCloseable interface.
     */
    @Override
    public void close() {
        executor.close();
        System.out.println("[JSignals] Runtime closed.");
    }

}
