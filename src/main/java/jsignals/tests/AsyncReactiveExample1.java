package jsignals.tests;

import jsignals.JSignals;
import jsignals.async.ResourceRef;
import jsignals.core.ComputedRef;
import jsignals.core.Disposable;
import jsignals.core.Ref;

import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static jsignals.JSignals.resource;

/**
 * Example demonstrating async features of the reactive signals library.
 */
public class AsyncReactiveExample1 {

    private static final Random random = new Random();

    public static void main(String[] args) throws Exception {
        System.out.println("=== Async Reactive JSignals Example ===\n");

        // 1. Basic ResourceRef usage
        System.out.println("1. ResourceRef Loading:");
        ResourceRef<String> userData = resource(() ->
                CompletableFuture.supplyAsync(() -> {
                    sleep(1000); // Simulate API delay
                    return "User: John Doe";
                })
        );

        System.out.println("ResourceRef state: " + userData.getData());
        System.out.println("Is loading: " + userData.isLoading());

        // Wait for resource to load
        Thread.sleep(1500);
        System.out.println("After loading - State: " + userData.getData());
        if (userData.isSuccess()) {
            System.out.println("Data: " + userData.get());
        }

        // 2. Async Computed Values
        System.out.println("\n2. Async Computed:");
        Ref<Integer> userId = JSignals.ref(1);

        ResourceRef<String> userProfile = resource(() ->
                CompletableFuture.supplyAsync(() -> {
                    int id = userId.get();
                    System.out.println("  [Fetching profile for user " + id + "...]");
                    sleep(500);
                    return "Profile for user #" + id;
                })
        );

        // Wait for initial computation
        Thread.sleep(600);
        System.out.println("Profile: " + userProfile.get());

        // Change userId - should trigger recomputation
        userId.set(2);
        Thread.sleep(600);
        System.out.println("Updated profile: " + userProfile.get());

        // 3. Async Effects with Debouncing
        System.out.println("\n3. Async Effects with Search:");
        Ref<String> searchQuery = JSignals.ref("");
        Ref<List<String>> searchResults = JSignals.ref(List.of());

        Disposable searchEffect = JSignals.effect(() -> {
            String query = searchQuery.get();
            if (query.isEmpty()) {
                CompletableFuture.completedFuture(null);
                return;
            }

            System.out.println("  [Searching for: " + query + "]");

            JSignals.delay(300, TimeUnit.MILLISECONDS) // Debounce
                    .thenCompose(v -> searchAPI(query))
                    .thenAccept(results -> {
                        searchResults.set(results);
                        System.out.println("  [Found " + results.size() + " results]");
                    });
        });

        // Simulate typing
        searchQuery.set("Java");
        Thread.sleep(100);
        searchQuery.set("JavaScript"); // Should cancel previous search
        Thread.sleep(500);

        // 4. Multiple Resources with Dependencies
        System.out.println("\n4. Dashboard with Multiple Resources:");
        Ref<Integer> selectedUserId = JSignals.ref(1);

        // User data resource
        ResourceRef<User> user = resource(() -> {
            int id = selectedUserId.get();
            return fetchUser(id);
        });

        // Posts resource (depends on user)
        ResourceRef<List<Post>> posts = resource(() -> {
            int id = selectedUserId.get();
            return fetchUserPosts(id);
        });

        // Combined dashboard state
        ComputedRef<DashboardState> dashboard = JSignals.computed(() -> {
            System.out.println("  [Computing dashboard state...]");

            if (user.isLoading() || posts.isLoading()) {
                return new DashboardState("Loading...", null, null);
            }

            if (user.isError() || posts.isError()) {
                return new DashboardState("Error loading data", null, null);
            }

            var resourceUser = user.get().getData();
            var resourcePosts = posts.get().getData();

            return new DashboardState("Ready", resourceUser, resourcePosts);
        });

        // Watch dashboard state
        JSignals.effect(() -> {
            DashboardState state = dashboard.get();
            System.out.println("  [Dashboard] Status: " + state.status);
            if (state.user != null) {
                System.out.println("  [Dashboard] User: " + state.user.name);
                System.out.println("  [Dashboard] Posts: " +
                        (state.posts != null ? state.posts.size() : 0));
            }
        });

        // Wait for initial load
        Thread.sleep(1500);

        // Change user - both resources should reload
        System.out.println("\nChanging to user 2...");
        selectedUserId.set(2);
        Thread.sleep(1500);

        // 5. Error Handling and Retry
        System.out.println("\n5. Error Handling:");
        ResourceRef<String> flakeyResource = resource(() ->
                CompletableFuture.supplyAsync(() -> {
                    if (random.nextBoolean()) {
                        throw new RuntimeException("Random failure!");
                    }
                    return "Success!";
                })
        );

        // Retry logic
        int retries = 0;
        while (retries < 3 && !flakeyResource.isSuccess()) {
            Thread.sleep(500);
            if (flakeyResource.isError()) {
                System.out.println("  Retry " + (retries + 1) + " - Error: " +
                        flakeyResource.getError().getMessage());
                flakeyResource.fetch();
                retries++;
            }
        }

        if (flakeyResource.isSuccess()) {
            System.out.println("  Final result: " + flakeyResource.get());
        }

        // Cleanup
        searchEffect.dispose();

        System.out.println("\n=== Async Example Complete ===");

        // Shutdown thread pool
        Thread.sleep(100);
        System.exit(0);
    }

    // Helper methods
    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static CompletableFuture<List<String>> searchAPI(String query) {
        return CompletableFuture.supplyAsync(() -> {
            sleep(200); // Simulate API delay
            return List.of(
                    query + " tutorial",
                    query + " documentation",
                    query + " examples"
            );
        });
    }

    private static CompletableFuture<User> fetchUser(int id) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("    [API] Fetching user " + id + "...");
            sleep(1000);
            return new User(id, "User #" + id);
        });
    }

    private static CompletableFuture<List<Post>> fetchUserPosts(int userId) {
        return CompletableFuture.supplyAsync(() -> {
            System.out.println("    [API] Fetching posts for user " + userId + "...");
            sleep(800);
            return List.of(
                    new Post("Post 1 by user " + userId),
                    new Post("Post 2 by user " + userId)
            );
        });
    }

    // Data classes
    static class User {

        final int id;

        final String name;

        User(int id, String name) {
            this.id = id;
            this.name = name;
        }

    }

    static class Post {

        final String title;

        Post(String title) {
            this.title = title;
        }

    }

    static class DashboardState {

        final String status;

        final User user;

        final List<Post> posts;

        DashboardState(String status, User user, List<Post> posts) {
            this.status = status;
            this.user = user;
            this.posts = posts;
        }

    }

}
