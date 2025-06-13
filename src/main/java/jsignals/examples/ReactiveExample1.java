package jsignals.examples;

import jsignals.core.ComputedRef;
import jsignals.core.Disposable;
import jsignals.core.Ref;

import static jsignals.JSignals.*;

/**
 * Example demonstrating the reactive signals library.
 */
public class ReactiveExample1 {

    public static void main(String[] args) {

        // Basic reactivity
        System.out.println("1. Basic Ref:");
        Ref<Integer> count = ref(0);
        System.out.println("Initial count: " + count.get()); // Initial count: 0

        count.set(5);
        System.out.println("After set(5): " + count.get()); // After set(5): 5

        // Computed values
        System.out.println("\n2. Computed Values:");
        ComputedRef<Integer> doubled = computed(() -> {
            System.out.println("  [Computing doubled value...] This won't run again unless count changes.");
            return count.get() * 2;
        });

        System.out.println("Doubled value: " + doubled.get()); // Doubled value: 10
        System.out.println("Doubled again (cached): " + doubled.get()); // Doubled again (cached): 10

        count.set(10);
        System.out.println("After count.set(10), doubled: " + doubled.get()); // After count.set(10), doubled: 20

        // Nested computed values
        System.out.println("\n3. Nested Computed:");
        ComputedRef<String> message = computed(() ->
                "Count is " + count.get() + ", doubled is " + doubled.get());
        System.out.println("Message: " + message.get());

        count.set(12);
        System.out.println("After count.set(12), message: " + message.get()); // After count.set(12), message: Count is 12, doubled is 24

        // Effects
        System.out.println("\n4. Effects:");
        Disposable effect1 = effect(() ->
                System.out.println("  [Effect] Count changed to: " + count.get()));

        count.set(15);
        count.set(20);

        // Dispose the effect
        effect1.dispose();
        count.set(25); // This won't trigger the effect

        // Watch specific value
        System.out.println("\n5. Watch:");
        Disposable watcher = count.subscribe(value ->
                System.out.println("  [Watch] Count is now: " + value));

        count.set(30);

        // Complex example with multiple dependencies
        System.out.println("\n6. Multiple Dependencies:");
        Ref<String> firstName = ref("John");
        Ref<String> lastName = ref("Doe");

        ComputedRef<String> fullName = computed(() -> {
            System.out.println("  [Computing fullName...]");
            return firstName.get() + " " + lastName.get();
        });

        effect(() ->
                System.out.println("  [Effect] Full name is: " + fullName.get()));

        firstName.set("Jane");
        lastName.set("Smith");
    }

}
