package jsignals.tests;

import jsignals.JSignals;
import jsignals.async.ResourceRef;
import jsignals.async.ResourceState;
import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import java.util.concurrent.CompletableFuture;

import static jsignals.JSignals.*;

public class RefFlatMap {

    record User(int id, String name, int age) {

        static User dummy(int id) {
            return new User(id, "User" + id, 20 + id);
        }

        static User dummy() {
            return dummy(0);
        }

        @Override
        public String toString() {
            return String.format("User{id=%d, name='%s', age=%d}", id, name, age);
        }

    }

    static class UserAPI {

        CompletableFuture<User> getUser(int userId) {
            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                // Return a dummy user
                return User.dummy(userId);
            });
        }

    }

    private static final UserAPI api = new UserAPI();

    private static ResourceRef<User> fetchUser(int id) {
        return resource(() -> api.getUser(id));
    }

    public static void refFlatMap() throws InterruptedException {
        Ref<Integer> currentUserIdRef = ref(0);

        ComputedRef<User> currentUserData = currentUserIdRef.flatMap(id -> {
            System.out.println("  [App] Fetching user information for ID " + id);
            return ref(User.dummy(id));
        });

        effect(() -> {
            User user = currentUserData.get();
            System.out.println("  [UI] User data loaded successfully: " + user);
        });

        Thread.sleep(1000);

        currentUserIdRef.set(1);

        Thread.sleep(1000);
    }

    public static void resourceFlatMap() throws InterruptedException {
        Ref<Integer> currentUserIdRef = ref(0);

        ComputedRef<ResourceState<User>> currentUserData = currentUserIdRef.flatMap(id -> {
            System.out.println("  [App] Fetching user information for ID " + id);
            return fetchUser(id);
        });

        effect(() -> {
            var state = currentUserData.get();
            if (state.isLoading()) {
                System.out.println("  [UI] Loading user data...");
            } else if (state.isError()) {
                System.out.println("  [UI] Error fetching user data: " + state.getError());
            } else if (state.isSuccess()) {
                System.out.println("  [UI] User data loaded successfully: " + state.getData());
            }
        });

        System.out.println("  [Code] Data: " + currentUserData.get());

        Thread.sleep(1000);

        System.out.println("  [Code] Data: " + currentUserData.get());

        currentUserIdRef.set(1);

        Thread.sleep(1000);

        System.out.println("  [Code] Data: " + currentUserData.get());

    }

    public static void manualResourceFlatMap() throws InterruptedException {
        Ref<Integer> currentUserIdRef = ref(0);

        ComputedRef<ResourceRef<User>> currentResourceFetcherRef = computed(() -> {
            var id = currentUserIdRef.get();
            System.out.println("  [App] currentResourceFetcherRef: Creating/Selecting resource for user ID " + id);
            var res = resource(() -> api.getUser(id));
            res.watch(state -> System.out.println("  [App] Resource state changed for user ID " + id + ": " + state));
            return res;
        });

        ComputedRef<ResourceState<User>> currentUserData = computed(() -> {
            var activeResourceRef = currentResourceFetcherRef.get();
            var state = activeResourceRef.get();
            System.out.println("  [App] currentUserData: Getting state from activeResourceRef for ID " + state);
            return state;
        });

        effect(() -> {
            var state = currentUserData.get();
            if (state.isLoading()) {
                System.out.println("  [UI] Loading user data...");
            } else if (state.isError()) {
                System.out.println("  [UI] Error fetching user data: " + state.getError());
            } else if (state.isSuccess()) {
                System.out.println("  [UI] User data loaded successfully: " + state.getData());
            }
        });

        Thread.sleep(1000);
        System.out.println("  [Code] Data: " + currentUserData.get());

        JSignals.submitTask(() -> currentUserIdRef.set(1));
        Thread.sleep(1000);
        System.out.println("  [Code] Data: " + currentUserData.get());
    }

    public static void nestedComputed() throws InterruptedException {
        Ref<Integer> sourceRef = ref(1);

        var computedFactory = computed(() -> {
            int sourceValue = sourceRef.get();
            System.out.println("  [App] Computed factory: sourceValue = " + sourceValue);
            return computed(() -> {
                System.out.println("  [App] Nested computed: sourceValue = " + sourceValue);
                return "Computed value based on source: " + sourceValue;
            });
        });

        computedFactory.watch(nestedComputed -> {
            System.out.println("  [UI] Nested computed value changed: " + nestedComputed.get());
        });

        sourceRef.set(2);

        Thread.sleep(1000);

        sourceRef.set(3);

        Thread.sleep(1000);
    }

    public static void main(String[] args) throws InterruptedException {
        try (var _ = JSignals.initRuntime()) {
            resourceFlatMap();
        }
    }

}
