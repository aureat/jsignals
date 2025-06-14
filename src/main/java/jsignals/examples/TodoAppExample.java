package jsignals.examples;

import jsignals.async.ResourceRef;
import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static jsignals.JSignals.*;

/**
 * Real-world example: A reactive to-do list application with search and filtering.
 */
public class TodoAppExample {

    private static final Ref<List<Todo>> todos = ref(new ArrayList<>());

    private static final Ref<String> searchQuery = ref("");

    private static final Ref<FilterType> filterType = ref(FilterType.ALL);

    private static final Ref<Boolean> isLoading = ref(false);

    private static final ComputedRef<List<Todo>> filteredTodos = computed(() -> {
        List<Todo> allTodos = todos.get();
        String query = searchQuery.get().toLowerCase();
        FilterType filter = filterType.get();

        return allTodos.stream()
                .filter(todo -> {
                    // Apply search filter
                    if (!query.isEmpty() && !todo.title.toLowerCase().contains(query)) {
                        return false;
                    }

                    // Apply status filter
                    return switch (filter) {
                        case ALL -> true;
                        case ACTIVE -> !todo.completed;
                        case COMPLETED -> todo.completed;
                    };
                })
                .toList();
    });

    private static final ComputedRef<TodoStats> stats = computed(() -> {
        List<Todo> allTodos = todos.get();
        long completed = allTodos.stream().filter(t -> t.completed).count();
        long active = allTodos.size() - completed;

        return new TodoStats((int) active, (int) completed, allTodos.size());
    });

    // Resources for server sync
    private static final ResourceRef<List<Todo>> serverTodos = resource(
            TodoAppExample::fetchTodosFromServer,
            false
    );

    private static ResourceRef<Void> saveToServerResource;

    public static void main(String[] args) throws Exception {
        System.out.println("=== Todo App Example ===\n");

        // Set up effects
        setupEffects();
        Thread.sleep(50);

        // Initial todos
        addTodo("Learn Java JSignals");
        Thread.sleep(50);

        addTodo("Build a reactive app");
        Thread.sleep(50);

        addTodo("Deploy to production");
        Thread.sleep(50);

        // Demo interactions
        System.out.println("\n--- Marking first todo as completed ---");
        toggleTodo(0);
        Thread.sleep(1000);

        System.out.println("\n--- Adding a new todo ---");
        addTodo("Write documentation");

        System.out.println("\n--- Searching for 'reactive' ---");
        searchQuery.set("reactive");
        Thread.sleep(100);

        System.out.println("\n--- Filtering completed todos ---");
        searchQuery.set("");
        filterType.set(FilterType.COMPLETED);
        Thread.sleep(100);

        System.out.println("\n--- Syncing with server ---");
        syncWithServer();
        Thread.sleep(2000);

        System.out.println("\n=== Todo App Example Complete ===");
        System.exit(0);
    }

    private static void setupEffects() {

        // Effect to display filtered todos
        effect(() -> {
            List<Todo> filtered = filteredTodos.get();
            System.out.println("\nFiltered Todos (" + filtered.size() + "):");
            for (int i = 0; i < filtered.size(); i++) {
                Todo todo = filtered.get(i);
                System.out.printf("  %d. [%s] %s%n",
                        i + 1,
                        todo.completed ? "✓" : " ",
                        todo.title
                );
            }
        });

        // Effect to display stats
        effect(() -> {
            TodoStats s = stats.get();
            System.out.printf("Stats: %d active, %d completed, %d total%n",
                    s.active, s.completed, s.total);
        });

        // Effect to show loading state
        isLoading.watch(loading -> {
            if (loading) {
                System.out.println("⏳ Loading...");
            }
        });

        // Auto-save effect (debounced)
        saveToServerResource = resource(() -> {
            todos.get();

            // Debounce saves, on virtual threads
            return delay(500, TimeUnit.MILLISECONDS)
                    .thenRun(() -> {
                        System.out.println("💾 Auto-saving todos...");
                    });
        });

        saveToServerResource.watch(state -> {
            if (state.isLoading()) {
                System.out.println("⏳ Saving to server in progress...");
            } else if (state.isSuccess()) {
                System.out.println("✅ Saving to server completed!");
            } else if (state.isError()) {
                System.err.println("❌ Error saving to server: " + state.getError());
            }
        });
    }

    // Actions
    private static void addTodo(String title) {
        todos.update(list -> {
            List<Todo> newList = new ArrayList<>(list);
            newList.add(new Todo(
                    UUID.randomUUID().toString(),
                    title,
                    false,
                    new Date()
            ));
            return newList;
        });
    }

    private static void toggleTodo(int index) {
        todos.update(list -> {
            if (index >= 0 && index < list.size()) {
                List<Todo> newList = new ArrayList<>(list);
                Todo todo = newList.get(index);
                newList.set(index, new Todo(
                        todo.id,
                        todo.title,
                        !todo.completed,
                        todo.createdAt
                ));
                return newList;
            }
            return list;
        });
    }

    private static void syncWithServer() {
        isLoading.set(true);

        serverTodos.fetch().thenAccept(serverData -> {
            // Merge server data with local data
            todos.update(localTodos -> {
                Set<String> localIds = localTodos.stream()
                        .map(t -> t.id)
                        .collect(HashSet::new, Set::add, Set::addAll);

                List<Todo> merged = new ArrayList<>(localTodos);
                for (Todo serverTodo : serverData) {
                    if (!localIds.contains(serverTodo.id)) {
                        merged.add(serverTodo);
                    }
                }

                return merged;
            });

            isLoading.set(false);
            System.out.println("✅ Sync complete!");
        }).exceptionally(error -> {
            isLoading.set(false);
            System.err.println("❌ Sync failed: " + error.getMessage());
            return null;
        });
    }

    private static CompletableFuture<List<Todo>> fetchTodosFromServer() {
        return CompletableFuture.supplyAsync(() -> {
            // Simulate server delay
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // Return some server todos
            return List.of(
                    new Todo(UUID.randomUUID().toString(),
                            "Server todo 1", true, new Date()),
                    new Todo(UUID.randomUUID().toString(),
                            "Server todo 2", false, new Date())
            );
        });
    }

    // Data types
    enum FilterType {
        ALL, ACTIVE, COMPLETED
    }

    static class Todo {

        final String id;

        final String title;

        final boolean completed;

        final Date createdAt;

        Todo(String id, String title, boolean completed, Date createdAt) {
            this.id = id;
            this.title = title;
            this.completed = completed;
            this.createdAt = createdAt;
        }

    }

    static class TodoStats {

        final int active;

        final int completed;

        final int total;

        TodoStats(int active, int completed, int total) {
            this.active = active;
            this.completed = completed;
            this.total = total;
        }

    }

}
