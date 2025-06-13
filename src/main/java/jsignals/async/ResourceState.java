package jsignals.async;

import java.util.Objects;

/**
 * Represents the state of an asynchronous resource.
 * This class is immutable.
 *
 * @param <T> The type of the data when the resource is successfully loaded.
 */
public final class ResourceState<T> {

    private enum Status {
        IDLE,
        LOADING,
        SUCCESS,
        ERROR
    }

    private final Status status;
    private final T data;
    private final Throwable error;

    // Private constructor to enforce usage of static factory methods
    private ResourceState(Status status, T data, Throwable error) {
        this.status = status;
        this.data = data;
        this.error = error;
    }

    // --- Static Factory Methods ---

    private static final ResourceState<?> IDLE_INSTANCE = new ResourceState<>(Status.IDLE, null, null);
    private static final ResourceState<?> LOADING_INSTANCE = new ResourceState<>(Status.LOADING, null, null);

    /**
     * Creates an idle state.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> idle() {
        return (ResourceState<T>) IDLE_INSTANCE;
    }

    /**
     * Creates a loading state.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> loading() {
        return (ResourceState<T>) LOADING_INSTANCE;
    }

    /**
     * Creates a success state with the given data.
     * @param data The successfully loaded data.
     */
    public static <T> ResourceState<T> success(T data) {
        // It's generally good practice to require data for a success state,
        // but null data might be a valid success scenario depending on the use case.
        // If data must not be null for success, add:
        // Objects.requireNonNull(data, "Data cannot be null for success state");
        return new ResourceState<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates an error state with the given error.
     * @param error The error that occurred.
     */
    public static <T> ResourceState<T> error(Throwable error) {
        Objects.requireNonNull(error, "Error cannot be null for error state");
        return new ResourceState<>(Status.ERROR, null, error);
    }

    // --- Getter Methods ---

    /**
     * @return The data if the state is SUCCESS, otherwise null.
     */
    public T getData() {
        return data; // Returns null if not in SUCCESS state or if data was null
    }

    /**
     * @return The error if the state is ERROR, otherwise null.
     */
    public Throwable getError() {
        return error; // Returns null if not in ERROR state
    }

    // --- State Check Methods ---

    public boolean isIdle() {
        return status == Status.IDLE;
    }

    public boolean isLoading() {
        return status == Status.LOADING;
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isError() {
        return status == Status.ERROR;
    }

    // --- Overrides ---

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ResourceState<?> that = (ResourceState<?>) o;
        return status == that.status &&
                Objects.equals(data, that.data) &&
                Objects.equals(error, that.error);
    }

    @Override
    public int hashCode() {
        return Objects.hash(status, data, error);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("ResourceState{status=");
        sb.append(status);
        if (data != null) {
            sb.append(", data=").append(data);
        }
        if (error != null) {
            sb.append(", error=").append(error);
        }
        sb.append('}');
        return sb.toString();
    }
}
