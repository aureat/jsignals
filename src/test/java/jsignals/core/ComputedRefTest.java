package jsignals.core;

import jsignals.JSignals;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("core")
public class ComputedRefTest {

    @Test
    public void testInitialValue() {
        Ref<Integer> count = new Ref<>(5);
        assertEquals(5, count.get(), "Initial count should be 5");

        ComputedRef<Integer> doubled = new ComputedRef<>(() -> count.get() * 2);
        assertEquals(10, doubled.get(), "Doubled value should be 10 initially");
    }

    @Test
    public void testCachedValue() {
        Ref<Integer> count = new Ref<>(5);
        ComputedRef<Integer> doubled = new ComputedRef<>(() -> {
            System.out.println(" [Computing doubled...]");
            return count.get() * 2;
        });

        // Access twice to ensure caching works
        assertEquals(10, doubled.get(), "First access should compute doubled value");
        assertEquals(10, doubled.get(), "Second access should use cached value");
    }

    @Test
    void testNestedComputedValues() {
        Ref<Integer> count = JSignals.ref(0);
        ComputedRef<Integer> doubled = JSignals.computed(() -> count.get() * 2);
        ComputedRef<String> message = JSignals.computed(() -> "Count is " + count.get() + ", doubled is " + doubled.get());

        assertEquals("Count is 0, doubled is 0", message.get(), "Initial message should match");

        count.set(5);
        assertEquals("Count is 5, doubled is 10", message.get(), "Message should update after count.set(5)");
    }

    @Test
    public void testRecomputationOnChange() {
        Ref<Integer> count = new Ref<>(5);
        ComputedRef<Integer> doubled = new ComputedRef<>(() -> count.get() * 2);

        // Change the value of count
        count.set(10);

        // Ensure recomputation happens
        assertEquals(20, doubled.get(), "Doubled value should recompute after count changes");
    }

    @Test
    public void testCachedAfterRecomputation() {
        Ref<Integer> count = new Ref<>(5);
        ComputedRef<Integer> doubled = new ComputedRef<>(() -> {
            System.out.println(" [Computing doubled...]");
            return count.get() * 2;
        });

        // Change the value of count
        count.set(10);

        // Access twice to ensure caching works after recomputation
        assertEquals(20, doubled.get(), "First access after change should recompute doubled value");
        assertEquals(20, doubled.get(), "Second access after change should use cached value");
    }

}
