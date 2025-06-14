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
        ERROR,
        CANCELLED
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

    private static final ResourceState<?> IDLE_INSTANCE = new ResourceState<>(Status.IDLE, null, null);

    private static final ResourceState<?> LOADING_INSTANCE = new ResourceState<>(Status.LOADING, null, null);

    private static final ResourceState<?> SUCCESS_INSTANCE = new ResourceState<>(Status.SUCCESS, null, null);

    private static final ResourceState<?> CANCELLED_INSTANCE = new ResourceState<>(Status.CANCELLED, null, null);

    /**
     * Creates an idle state.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> idle() {
        return (ResourceState<T>) IDLE_INSTANCE;
    }

    /**
     * Creates an idle state with the given data.
     *
     * @param data The data that was previously loaded, can be null.
     */
    public static <T> ResourceState<T> idle(T data) {
        if (data == null) {
            return idle();
        }

        return new ResourceState<>(Status.IDLE, data, null);
    }

    /**
     * Creates a loading state without previous data.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> loading() {
        return (ResourceState<T>) LOADING_INSTANCE;
    }

    /**
     * Creates a loading state with previous data.
     *
     * @param data The data that was previously loaded, can be null.
     */
    public static <T> ResourceState<T> loading(T data) {
        if (data == null) {
            return loading();
        }

        return new ResourceState<>(Status.LOADING, data, null);
    }


    /**
     * Creates a success state with the given data.
     *
     * @param data The successfully loaded data, can be null.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> success(T data) {
        if (data == null) {
            return (ResourceState<T>) SUCCESS_INSTANCE;
        }

        return new ResourceState<>(Status.SUCCESS, data, null);
    }

    /**
     * Creates an error state with the given error.
     *
     * @param data  The data that was previously loaded, can be null.
     * @param error The error that occurred.
     */
    public static <T> ResourceState<T> error(T data, Throwable error) {
        Objects.requireNonNull(error, "Error cannot be null for error state");
        return new ResourceState<>(Status.ERROR, data, error);
    }

    /**
     * Creates an error state without data.
     *
     * @param error The error that occurred.
     */
    public static <T> ResourceState<T> error(Throwable error) {
        return error(null, error);
    }

    /**
     * Creates an cancelled state without an error.
     */
    @SuppressWarnings("unchecked")
    public static <T> ResourceState<T> cancelled() {
        return (ResourceState<T>) CANCELLED_INSTANCE;
    }

    /**
     * Creates an cancelled state with the given error.
     *
     * @param data  The data that was previously loaded, can be null.
     * @param error The error that occurred.
     */
    public static <T> ResourceState<T> cancelled(T data, Throwable error) {
        if (error == null && data == null) {
            return cancelled();
        }

        return new ResourceState<>(Status.CANCELLED, data, error);
    }

    /**
     * Creates a cancelled state with the given error.
     *
     * @param error The error that occurred.
     */
    public static <T> ResourceState<T> cancelled(Throwable error) {
        return cancelled(null, error);
    }

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

    /**
     * @return The current status of the resource state.
     */
    public boolean isIdle() {
        return status == Status.IDLE;
    }

    /**
     * @return true if the state is LOADING, false otherwise.
     */
    public boolean isLoading() {
        return status == Status.LOADING;
    }

    /**
     * @return true if the state is SUCCESS, false otherwise.
     */
    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    /**
     * @return true if the state is SUCCESS and data is not null, false otherwise.
     */
    public boolean isError() {
        return status == Status.ERROR;
    }

    /**
     * @return true if the state is CANCELLED, false otherwise.
     */
    public boolean isCancelled() {
        return status == Status.CANCELLED;
    }

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
