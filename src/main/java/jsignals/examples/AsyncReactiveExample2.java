//package jsignals.examples;
//
//import io.signals.Signals;
//import io.signals.core.*;
//import java.util.concurrent.CompletableFuture;
//
///**
// * Fixed async example with proper dependency tracking.
// */
//public class AsyncReactiveExample2 {
//    public static void main(String[] args) throws Exception {
//        System.out.println("=== Fixed Async Reactive JSignals Example ===\n");
//
//        // Test 1: Async Computed with Dependencies
//        testAsyncComputed();
//        Thread.sleep(1500);
//
//        // Test 2: Resources with Dependencies
//        testResourceDependencies();
//        Thread.sleep(2000);
//
//        // Test 3: Async Effects with Debouncing
//        testAsyncEffects();
//        Thread.sleep(2000);
//
//        System.out.println("\n=== Fixed Async Example Complete ===");
//        System.exit(0);
//    }
//
//    private static void testAsyncComputed() {
//        System.out.println("1. Async Computed with Dependencies:");
//
//        RefFixed<Integer> userId = new RefFixed<>(1);
//
//        AsyncComputedRef<String> userProfile = new AsyncComputedRef<>(() -> {
//            int id = userId.get();
//            System.out.println("  [Fetching profile for user " + id + "...]");
//
//            return CompletableFuture.supplyAsync(() -> {
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    return "Fetch cancelled";
//                }
//                return "Profile for user #" + id;
//            });
//        });
//
//        // First access
//        userProfile.get().thenAccept(profile ->
//            System.out.println("  Initial profile: " + profile)
//        );
//
//        // Wait for completion
//        try { Thread.sleep(600); } catch (InterruptedException e) {}
//
//        // Change user ID - should trigger refetch
//        System.out.println("\n  Changing to user 2...");
//        userId.set(2);
//
//        // Access again - should get new profile
//        userProfile.get().thenAccept(profile ->
//            System.out.println("  Updated profile: " + profile)
//        );
//    }
//
//    private static void testResourceDependencies() {
//        System.out.println("\n2. Resources with Dependencies:");
//
//        RefFixed<Integer> selectedUserId = new RefFixed<>(1);
//
//        // User resource that depends on selectedUserId
//        ResourceFixed<String> userResource = new ResourceFixed<>(() -> {
//            int id = selectedUserId.get();
//            System.out.println("    [API] Fetching user " + id + "...");
//
//            return CompletableFuture.supplyAsync(() -> {
//                try {
//                    Thread.sleep(300);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException("Interrupted");
//                }
//                return "User #" + id;
//            });
//        }, true);
//
//        // Posts resource that also depends on selectedUserId
//        ResourceFixed<Integer> postsResource = new ResourceFixed<>(() -> {
//            int id = selectedUserId.get();
//            System.out.println("    [API] Fetching posts for user " + id + "...");
//
//            return CompletableFuture.supplyAsync(() -> {
//                try {
//                    Thread.sleep(400);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException("Interrupted");
//                }
//                return id * 10; // Fake post count
//            });
//        }, true);
//
//        // Watch the resources
//        Signals.watchEffect(() -> {
//            ResourceState<String> userState = userResource.get();
//            ResourceState<Integer> postsState = postsResource.get();
//
//            if (userState.isLoading() || postsState.isLoading()) {
//                System.out.println("  [Dashboard] Loading...");
//            } else if (userState.isSuccess() && postsState.isSuccess()) {
//                System.out.println("  [Dashboard] User: " + userState.getData() +
//                                 ", Posts: " + postsState.getData());
//            }
//        });
//
//        // Wait for initial load
//        try { Thread.sleep(500); } catch (InterruptedException e) {}
//
//        // Change user - both resources should refetch
//        System.out.println("\n  Changing to user 3...");
//        selectedUserId.set(3);
//    }
//
//    private static void testAsyncEffects() {
//        System.out.println("\n3. Async Effects with Debouncing:");
//
//        RefFixed<String> searchQuery = new RefFixed<>("");
//
//        // Debounced search effect
//        Signals.watchEffectAsync(() -> {
//            String query = searchQuery.get();
//            if (query.isEmpty()) return CompletableFuture.completedFuture(null);
//
//            System.out.println("  [Searching for: " + query + "]");
//
//            return CompletableFuture.supplyAsync(() -> {
//                try {
//                    Thread.sleep(300);
//                    System.out.println("  [Found results for: " + query + "]");
//                } catch (InterruptedException e) {
//                    System.out.println("  [Search cancelled for: " + query + "]");
//                }
//                return null;
//            });
//        });
//
//        // Rapid changes - only last one should execute
//        searchQuery.set("J");
//        searchQuery.set("Ja");
//        searchQuery.set("Jav");
//        searchQuery.set("Java");
//
//        try { Thread.sleep(100); } catch (InterruptedException e) {}
//
//        // Another change after delay
//        searchQuery.set("JavaScript");
//    }
//}
