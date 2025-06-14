package jsignals.async;

import jsignals.runtime.DependencyTracker;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.*;

class ResourceRefExample1 {

    private DependencyTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = DependencyTracker.getInstance();
    }

    @Test
    void testFetchSuccess() throws ExecutionException, InterruptedException {
        Supplier<CompletableFuture<String>> fetcher = () -> CompletableFuture.completedFuture("Test Data");
        ResourceRef<String> resource = new ResourceRef<>(fetcher);

        CompletableFuture<String> fetchResult = resource.fetch();
        assertEquals("Test Data", fetchResult.get());
        assertTrue(resource.isSuccess());
        assertEquals("Test Data", resource.getData());
    }

    @Test
    void testFetchError() {
        Supplier<CompletableFuture<String>> fetcher = () -> CompletableFuture.failedFuture(new RuntimeException("Fetch failed"));
        ResourceRef<String> resource = new ResourceRef<>(fetcher);

        CompletableFuture<String> fetchResult = resource.fetch();
        assertThrows(ExecutionException.class, fetchResult::get);
        assertTrue(resource.hasError());
        assertEquals("Fetch failed", resource.getError().getMessage());
    }

    @Test
    void testAutoFetch() {
        AtomicBoolean fetchCalled = new AtomicBoolean(false);
        Supplier<CompletableFuture<String>> fetcher = () -> {
            fetchCalled.set(true);
            return CompletableFuture.completedFuture("Auto Fetch Data");
        };

        ResourceRef<String> resource = new ResourceRef<>(fetcher, true);
        assertTrue(fetchCalled.get());
        assertTrue(resource.isSuccess());
        assertEquals("Auto Fetch Data", resource.getData());
    }

    @Test
    void testDependencyTracking() {
        Supplier<CompletableFuture<String>> fetcher = () -> CompletableFuture.completedFuture("Dependency Data");
        ResourceRef<String> resource = new ResourceRef<>(fetcher);

        tracker.trackAccess(resource);
        resource.onDependencyChanged();

        assertTrue(resource.isLoading());
    }

    @Test
    void testCancelPreviousFetch() {
        CompletableFuture<String> longRunningFetch = new CompletableFuture<>();
        Supplier<CompletableFuture<String>> fetcher = () -> longRunningFetch;

        ResourceRef<String> resource = new ResourceRef<>(fetcher);
        resource.fetch(); // Start the first fetch

        CompletableFuture<String> newFetch = resource.fetch(); // Start a new fetch
        assertTrue(longRunningFetch.isCancelled());
        assertFalse(newFetch.isDone());
    }

}