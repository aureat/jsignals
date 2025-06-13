package jsignals.examples;

import jsignals.JSignals;
import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple test to verify computed values update correctly.
 */
public class SimpleTodoTest {

    public static void main(String[] args) {
        System.out.println("=== Simple Todo Reactivity Test ===\n");

        // Create reactive state
        Ref<List<String>> todos = JSignals.ref(Arrays.asList("Task 1", "Task 2"));
        Ref<String> filter = JSignals.ref("");

        // Create computed filtered list
        ComputedRef<List<String>> filtered = JSignals.computed(() -> {
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

        // Watch the filtered list
        JSignals.effect(() -> {
            List<String> items = filtered.get();
            System.out.println("\nFiltered todos (" + items.size() + "):");
            items.forEach(item -> System.out.println("  - " + item));
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

        System.out.println("\n=== Test Complete ===");
    }

}
