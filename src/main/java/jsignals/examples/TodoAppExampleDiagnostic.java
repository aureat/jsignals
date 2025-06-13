package jsignals.examples;

import jsignals.JSignals;
import jsignals.async.Resource;
import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * Real-world example: A reactive todo list application with search and filtering.
 * Modified with diagnostic output to verify reactivity is working.
 */
public class TodoAppExampleDiagnostic {

    // State
    private static final Ref<List<Todo>> todos = JSignals.ref(new ArrayList<>());

    private static final Ref<String> searchQuery = JSignals.ref("");

    private static final Ref<FilterType> filterType = JSignals.ref(FilterType.ALL);

    private static final Ref<Boolean> isLoading = JSignals.ref(false);

    // Computed values
    private static final ComputedRef<List<Todo>> filteredTodos = JSignals.computed(() -> {
        System.out.println("  [COMPUTING filteredTodos...]");
        List<Todo> allTodos = todos.get();
        String query = searchQuery.get().toLowerCase();
        FilterType filter = filterType.get();

        System.out.println("    - Total todos: " + allTodos.size());
        System.out.println("    - Search query: '" + query + "'");
        System.out.println("    - Filter type: " + filter);

        List<Todo> result = allTodos.stream()
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

        System.out.println("    - Filtered result: " + result.size() + " todos");
        return result;
    });

    private static final ComputedRef<TodoStats> stats = JSignals.computed(() -> {
        System.out.println("  [COMPUTING stats...]");
        List<Todo> allTodos = todos.get();
        long completed = allTodos.stream().filter(t -> t.completed).count();
        long active = allTodos.size() - completed;

        return new TodoStats((int) active, (int) completed, allTodos.size());
    });

    // Resources for server sync
    private static final Resource<List<Todo>> serverTodos = JSignals.resource(
            TodoAppExampleDiagnostic::fetchTodosFromServer,
            false // Don't auto-fetch
    );

    public static void main(String[] args) throws Exception {
        System.out.println("=== Reactive Todo App Example (Diagnostic) ===\n");

        // Set up effects
        setupEffects();

        // Initial todos
        System.out.println("\n>>> Adding initial todos...");
        addTodo("Learn Java JSignals");
        Thread.sleep(100); // Give effects time to run

        addTodo("Build a reactive app");
        Thread.sleep(100);

        addTodo("Deploy to production");
        Thread.sleep(100);

        // Demo interactions
        System.out.println("\n>>> Marking first todo as completed...");
        toggleTodo(0);
        Thread.sleep(100);

        System.out.println("\n>>> Adding a new todo...");
        addTodo("Write documentation");
        Thread.sleep(100);

        System.out.println("\n>>> Searching for 'reactive'...");
        searchQuery.set("reactive");
        Thread.sleep(100);

        System.out.println("\n>>> Clearing search and filtering completed todos...");
        searchQuery.set("");
        filterType.set(FilterType.COMPLETED);
        Thread.sleep(100);

        System.out.println("\n>>> Switching back to ALL filter...");
        filterType.set(FilterType.ALL);
        Thread.sleep(100);

        System.out.println("\n>>> Syncing with server...");
        syncWithServer();
        Thread.sleep(2000);

        System.out.println("\n=== Todo App Example Complete ===");
        System.exit(0);
    }

    private static void setupEffects() {
        System.out.println("Setting up reactive effects...");

        // Effect to display filtered todos
        JSignals.effect(() -> {
            System.out.println("\n[EFFECT: Display Filtered Todos]");
            List<Todo> filtered = filteredTodos.get();
            System.out.println("Filtered Todos (" + filtered.size() + "):");
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
        JSignals.effect(() -> {
            System.out.println("\n[EFFECT: Display Stats]");
            TodoStats s = stats.get();
            System.out.printf("Stats: %d active, %d completed, %d total%n",
                    s.active, s.completed, s.total);
        });

        // Effect to show loading state
        isLoading.subscribe(loading -> {
            System.out.println("\n[EFFECT: Loading State Changed]");
            if (loading) {
                System.out.println("⏳ Loading...");
            } else {
                System.out.println("✅ Not loading");
            }
        });

        // Auto-save effect (debounced)
//        JSignals.watchEffectAsync(() -> {
//            System.out.println("\n[ASYNC EFFECT: Auto-save triggered]");
//            List<Todo> currentTodos = todos.get();
//            System.out.println("  - Will save " + currentTodos.size() + " todos after delay");
//
//            // Debounce saves
//            return JSignals.delay(500, TimeUnit.MILLISECONDS)
//                .thenRun(() -> {
//                    System.out.println("💾 Auto-saving " + currentTodos.size() + " todos...");
//                    // In real app, would save to localStorage or server
//                });
//        });

        System.out.println("All effects set up!\n");
    }

    // Actions
    private static void addTodo(String title) {
        System.out.println("\n[ACTION: Adding todo '" + title + "']");
        todos.update(list -> {
            List<Todo> newList = new ArrayList<>(list);
            newList.add(new Todo(
                    UUID.randomUUID().toString(),
                    title,
                    false,
                    new Date()
            ));
            System.out.println("  - Todos count: " + list.size() + " -> " + newList.size());
            return newList;
        });
    }

    private static void toggleTodo(int index) {
        System.out.println("\n[ACTION: Toggling todo at index " + index + "]");
        todos.update(list -> {
            if (index >= 0 && index < list.size()) {
                List<Todo> newList = new ArrayList<>(list);
                Todo todo = newList.get(index);
                Todo updatedTodo = new Todo(
                        todo.id,
                        todo.title,
                        !todo.completed,
                        todo.createdAt
                );
                newList.set(index, updatedTodo);
                System.out.println("  - Todo '" + todo.title + "' completed: " +
                        todo.completed + " -> " + updatedTodo.completed);
                return newList;
            }
            System.out.println("  - Invalid index!");
            return list;
        });
    }

    private static void syncWithServer() {
        System.out.println("\n[ACTION: Starting server sync]");
        isLoading.set(true);

        serverTodos.fetch().thenAccept(serverData -> {
            System.out.println("\n[SERVER RESPONSE: Received " + serverData.size() + " todos]");

            // Merge server data with local data
            todos.update(localTodos -> {
                // Simple merge strategy - in real app would be more sophisticated
                Set<String> localIds = localTodos.stream()
                        .map(t -> t.id)
                        .collect(HashSet::new, Set::add, Set::addAll);

                List<Todo> merged = new ArrayList<>(localTodos);
                int added = 0;
                for (Todo serverTodo : serverData) {
                    if (!localIds.contains(serverTodo.id)) {
                        merged.add(serverTodo);
                        added++;
                        System.out.println("  - Added from server: " + serverTodo.title);
                    }
                }

                System.out.println("  - Merge complete: " + localTodos.size() +
                        " local + " + added + " new = " + merged.size() + " total");
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
        System.out.println("  - Fetching from server (simulated 1.5s delay)...");
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
