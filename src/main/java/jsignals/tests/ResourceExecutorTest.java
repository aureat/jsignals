package jsignals.tests;

import jsignals.async.ResourceRef;
import jsignals.core.Ref;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static jsignals.JSignals.*;

public class ResourceExecutorTest {

    private static final ExecutorService customExecutor = Executors.newFixedThreadPool(3, new ThreadFactory() {
        private final AtomicInteger counter = new AtomicInteger(0);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "custom-executor-" + counter.incrementAndGet());
        }
    });

    public static void main(String[] args) throws InterruptedException {
        try (var runtime = initRuntime()) {

            System.out.println("🚀 Starting ResourceRef Test Application...");

            // --- Test 1: Basic Resource Fetch ---
            testBasicResource();
            Thread.sleep(2000); // Wait for the fetch to complete

            // --- Test 2: Dependency-Driven Fetch with Debouncing ---
            testDebouncedResource();
            Thread.sleep(4000); // Wait for debounce and fetch to complete

            // --- Test 3: Resource with Custom Executor ---
            testCustomExecutorResource();
            Thread.sleep(2000); // Wait for the fetch to complete

            System.out.println("✅ All tests complete. Shutting down.");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            // Ensure the custom executor is properly shut down
            System.out.println("Shutting down custom executor...");
            customExecutor.shutdown();
            try {
                if (!customExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                    customExecutor.shutdownNow();
                }
            } catch (InterruptedException e) {
                customExecutor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    /**
     * Tests a basic resource that auto-fetches on creation using default settings.
     */
    private static void testBasicResource() {
        System.out.println("\n--- 🧪 Test 1: Basic Resource ---");
        System.out.println("Creating a simple resource that fetches user 'Alice'.");

        ResourceRef<String> userResource = resource(
                () -> simulateApiFetch("Alice", 1000)
        );

        watchAndPrint(userResource, "UserResource");
    }

    /**
     * Tests a resource that refetches based on a dependency and debounces rapid changes.
     */
    private static void testDebouncedResource() throws InterruptedException {
        System.out.println("\n--- 🧪 Test 2: Dependency-Driven & Debounced Resource ---");
        Ref<String> queryRef = ref("initial-query");

        System.out.println("Creating a debounced resource (500ms) that depends on a query Ref.");
        ResourceRef<String> searchResource = resource(
                () -> {
                    String currentQuery = queryRef.get(); // Establish dependency
                    System.out.printf("[Fetcher] ==> Fetching for query: '%s'\n", currentQuery);
                    return simulateApiFetch(currentQuery, 1000);
                },
                true,
                Duration.ofMillis(500)
        );

        watchAndPrint(searchResource, "SearchResource");

        // Wait for initial fetch
        Thread.sleep(2000);

        System.out.println("\n>>> Rapidly changing the query Ref 5 times...");
        for (int i = 0; i < 5; i++) {
            queryRef.set("query-" + i);
            Thread.sleep(100); // Change dependency faster than debounce delay
        }
        System.out.println(">>> Final query set to 'final-query'. Waiting for debounce...");
        queryRef.set("final-query");
        // The console should show only ONE fetch for "final-query" after the delay.
    }

    /**
     * Tests a resource configured to use a custom executor for its async callbacks.
     */
    private static void testCustomExecutorResource() {
        System.out.println("\n--- 🧪 Test 3: Resource with Custom Executor ---");
        System.out.println("Creating a resource that will process its result on our custom executor.");

        ResourceRef<String> customExecResource = resource(
                () -> simulateApiFetch("DataForCustomExec", 1000),
                customExecutor // Pass the custom executor
        );

        watchAndPrint(customExecResource, "CustomExecResource");
    }


    /**
     * A helper method to subscribe to a resource and print its state changes.
     */
    private static <T> void watchAndPrint(ResourceRef<T> resource, String resourceName) {
        resource.watch(state -> {
            String threadName = Thread.currentThread().getName();
            System.out.printf("[%s] State Changed on thread '%s': %s\n", resourceName, threadName, state);
        });
    }

    /**
     * Simulates a network request that takes some time to complete.
     *
     * @param query   The data being "fetched".
     * @param delayMs The simulated network latency.
     * @return A CompletableFuture that will complete with the result.
     */
    private static CompletableFuture<String> simulateApiFetch(String query, long delayMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new CompletionException(e);
            }
            if ("query-3".equals(query)) {
                // Simulate a failing API call for a specific query
                throw new RuntimeException("API Error: Invalid query!");
            }
            return String.format("Data for '%s'", query);
        });
    }

}
