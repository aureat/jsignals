package jsignals.tests;

import jsignals.async.ResourceRef;
import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static jsignals.JSignals.*;

/**
 * Simple test to verify computed values update correctly.
 */
public class SimpleTodoTest {

    public static void main(String[] args) throws InterruptedException {
        System.out.println("=== Simple Todo Reactivity Test ===\n");

        // Create reactive state
        Ref<List<String>> todos = ref(Arrays.asList("Task 1", "Task 2"));
        Ref<String> filter = ref("");

        // Create computed filtered list
        ComputedRef<List<String>> filtered = computed(() -> {
            System.out.println("[Computing filtered list...]");
            List<String> allTodos = todos.get();
            String filterText = filter.get();

            if (filterText.isEmpty()) {
                return allTodos;
            }

            return allTodos.stream()
                    .filter(todo -> todo.toLowerCase().contains(filterText.toLowerCase()))
                    .toList();
        });

        ResourceRef<List<String>> expensiveResource = resource(() -> {
            System.out.println("[Fetching expensive resource...]");
            var list = todos.get(); // Return current todos

            return CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                return list;
            });
        });

        // Watch the filtered list
        effect(() -> {
            List<String> items = filtered.get();
            System.out.println("\nFiltered todos (" + items.size() + "):");
            for (String item : items) {
                System.out.println(" - " + item);
            }
        });

        // Test reactivity
        System.out.println("\n>>> Adding 'Task 3'");
        todos.update(list -> {
            List<String> newList = new ArrayList<>(list);
            newList.add("Task 3");
            return newList;
        });

        System.out.println("\n>>> Setting filter to '2'");
        filter.set("2");

        System.out.println("\n>>> Clearing filter");
        filter.set("");

        System.out.println("\n>>> Adding 'Another task'");
        todos.update(list -> {
            List<String> newList = new ArrayList<>(list);
            newList.add("Another task");
            return newList;
        });

        Thread.sleep(2000);

        System.out.println("\n=== Test Complete ===");
    }

}
