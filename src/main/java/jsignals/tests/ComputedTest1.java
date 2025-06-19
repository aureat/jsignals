package jsignals.tests;

import jsignals.core.ComputedRef;
import jsignals.core.Ref;

import static jsignals.JSignals.computed;
import static jsignals.JSignals.ref;

public class ComputedTest1 {

    public static void main(String[] args) {
        System.out.println("=== ComputedRef Example ===\n");

        Ref<Integer> baseValue = ref(10);
        ComputedRef<Integer> computedValue = computed(() -> {
            System.out.println("  [Computing] Base value is: " + baseValue.get());
            return baseValue.get() * 2;
        });

        System.out.println("Initial computed value: " + computedValue.get()); // Should print 20

        baseValue.set(20);
        System.out.println("After changing base value, computed value: " + computedValue.get()); // Should print 40
    }

}
