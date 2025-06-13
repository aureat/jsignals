package jsignals;

import jsignals.async.Resource;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import static jsignals.JSignals.resource;
import static org.junit.jupiter.api.Assertions.*;

class ResourcesTest {

    @Test
    void testFetchSuccess() throws ExecutionException, InterruptedException {

        var dataResource = resource(() -> {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            return CompletableFuture.completedFuture("Test Data");
        }, false);

        CompletableFuture<String> fetchResult = dataResource.fetch();
        assertEquals("Test Data", fetchResult.get());
        assertTrue(dataResource.isSuccess());
        assertEquals("Test Data", dataResource.getData());
    }

    @Test
    void testFetchError() {
        Resource<String> failedResource = resource(() -> CompletableFuture.failedFuture(new RuntimeException("Fetch failed")), false);

        CompletableFuture<String> fetchResult = failedResource.fetch();
        System.out.println("Fetch result: " + fetchResult);
        assertTrue(failedResource.hasError());
        assertNull(failedResource.getData());
        assertEquals("Fetch failed", failedResource.getError().getMessage());
    }

    @Test
    void testAutoFetch() {
        AtomicBoolean fetchCalled = new AtomicBoolean(false);
        Resource<String> resource = resource(() -> {
            fetchCalled.set(true);
            return CompletableFuture.completedFuture("Auto Fetch Data");
        }, true);

        assertTrue(fetchCalled.get());
        assertTrue(resource.isSuccess());
        assertEquals("Auto Fetch Data", resource.getData());
    }

    @Test
    void testCancelPreviousFetch() {
        CompletableFuture<String> longRunningFetch = new CompletableFuture<>();
        Resource<String> resource = resource(() -> longRunningFetch, false);

        CompletableFuture<String> firstFetch = resource.fetch();
        assertFalse(firstFetch.isDone(), "First fetch should not be done immediately");

        System.out.println("First fetch started, starting second fetch");

        CompletableFuture<String> newFetch = resource.fetch();
        assertTrue(newFetch.isDone(), "Second fetch is also cancelled immediately");
        assertTrue(longRunningFetch.isCancelled());
    }

    @Test
    void testCancelPreviousFetchRefetch() {
        Resource<String> resource = resource(() -> new CompletableFuture<String>(), false);

        CompletableFuture<String> firstFetch = resource.fetch();
        assertFalse(firstFetch.isDone(), "First fetch should not be done immediately");

        System.out.println("First fetch started, starting second fetch");

        CompletableFuture<String> newFetch = resource.fetch();
        System.out.println("Second fetch started, cancelled previous one");

        assertTrue(newFetch.isDone(), "Second fetch should");
    }

    @Test
    void testRefetch() throws ExecutionException, InterruptedException {
        Resource<String> resource = resource(() -> CompletableFuture.completedFuture("Initial Data"), false);

        // Initial fetch
        CompletableFuture<String> initialFetch = resource.fetch();
        assertEquals("Initial Data", initialFetch.get());

        // Refetch
        CompletableFuture<String> refetchResult = resource.refetch();
        assertEquals("Initial Data", refetchResult.get());
    }

}