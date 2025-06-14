package jsignals.examples;

import java.util.concurrent.CompletableFuture;

import static jsignals.JSignals.*;

public class ResourceExample1 {

    record User(int id, String name, int age) {

        @Override
        public String toString() {
            return "User {" +
                    "id=" + id +
                    ", name='" + name + '\'' +
                    ", age=" + age +
                    '}';
        }

    }

    static class UserAPI {

        CompletableFuture<User> getUser(int userId) {
            return CompletableFuture.supplyAsync(() -> {
                System.out.println("  [API] Fetching user " + userId);

                // Simulate network delay
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Return a dummy user
                return new User(userId, "User" + userId, 20 + userId);
            });
        }

    }

    public static void main(String[] args) throws Exception {
        var userAPI = new UserAPI();

        System.out.println("  [Code] Creating ref");
        var currentUserIdRef = ref(0);

        System.out.println("  [Code] Creating resource");
        var currentUserResource = resource(() -> {
            var currentUserId = currentUserIdRef.get();
            System.out.println("  [App] Fetching user information for ID " + currentUserId);

            return userAPI.getUser(currentUserId)
                    .thenApply(user -> {
                        System.out.println("  [App] Fetched " + user);
                        return user;
                    });
        }, true);

        System.out.println("  [Code] Creating effect");
        effect(() -> {
            var currentUserResourceState = currentUserResource.get();
            if (currentUserResourceState.isLoading()) {
                System.out.println("  [UI] Loading user information...");
            } else if (currentUserResourceState.isError()) {
                System.err.println("  [UI] Error fetching user information: " + currentUserResourceState.getError().getMessage());
            } else if (currentUserResourceState.isSuccess()) {
                System.out.println("  [UI] Successfully fetched user: " + currentUserResourceState.getData());
            }
        });

        currentUserIdRef.set(1);

        Thread.sleep(1000);

        currentUserIdRef.set(2);

        Thread.sleep(1000);

    }

}
