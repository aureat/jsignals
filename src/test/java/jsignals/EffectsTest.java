package jsignals;

import jsignals.core.ComputedRef;
import jsignals.core.Disposable;
import jsignals.core.Ref;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import static jsignals.JSignals.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("core")
public class EffectsTest {

    @Test
    void testEffects() {
        Ref<Integer> count = ref(0);
        StringBuilder effectLog = new StringBuilder();

        Disposable effect1 = effect(() -> {
            System.out.println("  [Effect] Count changed to: " + count.get());
            effectLog.append("Count changed to: ").append(count.get()).append("\n");
        });

        count.set(5);
        count.set(10);

        assertEquals("""
                Count changed to: 0
                Count changed to: 5
                Count changed to: 10
                """, effectLog.toString(), "Effect log should match changes");

        effect1.dispose();
        count.set(15);

        assertEquals("""
                Count changed to: 0
                Count changed to: 5
                Count changed to: 10
                """, effectLog.toString(), "Effect should not trigger after disposal");
    }

    @Test
    void testWatch() {
        Ref<Integer> count = ref(0);
        StringBuilder watchLog = new StringBuilder();

        var watcher = count.watch(value -> {
            System.out.println("  [Watch] Count is now: " + value);
            watchLog.append("Count is now: ").append(value).append("\n");
        });

        count.set(5);
        count.set(10);

        assertEquals("Count is now: 5\nCount is now: 10\n", watchLog.toString(), "Watch log should match changes");

        watcher.dispose();
        count.set(15);

        assertEquals("Count is now: 5\nCount is now: 10\n", watchLog.toString(), "Watch should not trigger after disposal");
    }

    @Test
    void testMultipleDependencies() {
        Ref<String> firstName = ref("John");
        Ref<String> lastName = ref("Doe");
        ComputedRef<String> fullName = computed(() -> firstName.get() + " " + lastName.get());

        StringBuilder effectLog = new StringBuilder();
        effect(() -> {
            System.out.println("  [Effect] Full name changed to: " + fullName.get());
            effectLog.append("Full name is: ").append(fullName.get()).append("\n");
        });

        firstName.set("Jane");
        lastName.set("Smith");

        assertEquals("""
                        Full name is: John Doe
                        Full name is: Jane Doe
                        Full name is: Jane Smith
                        """, effectLog.toString(),
                "Effect log should match full name changes");
    }

    @Test
    void testWatchAndEffect() {
        Ref<Integer> value = ref(0);
        StringBuilder log = new StringBuilder();

        var watcher = value.watch(v -> {
            System.out.println("  [Watch] Watched value: " + v);
            log.append("Watched value: ").append(v).append("\n");
        });

        // Create an effect that depends on the value
        var effect = effect(() -> {
            System.out.println("  [Effect] Effect triggered with value: " + value.get());
            log.append("Effect triggered with value: ").append(value.get()).append("\n");
        });

        // Change the value
        value.set(1);
        value.set(2);

        // Check logs
        assertEquals("""
                Effect triggered with value: 0
                Watched value: 1
                Effect triggered with value: 1
                Watched value: 2
                Effect triggered with value: 2
                """, log.toString());

        // Dispose watchers and effects
        watcher.dispose();
        effect.dispose();

        // Change the value again, should not trigger any logs
        value.set(3);

        assertEquals("""
                Effect triggered with value: 0
                Watched value: 1
                Effect triggered with value: 1
                Watched value: 2
                Effect triggered with value: 2
                """, log.toString(), "Logs should not change after disposal");
    }

}
