package jsignals.core;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Tag;

/**
 * Basic test for Ref implementation
 */
@Tag("core")
public class RefTest {

    @Test
    void testBasicGetSet() {
        Ref<Integer> count = new Ref<>(0);
        assertEquals(0, count.get(), "Initial value should be 0");

        count.set(5);
        assertEquals(5, count.get(), "Value should be 5 after set");
    }

    @Test
    void testUpdateFunction() {
        Ref<Integer> count = new Ref<>(0);
        count.update(v -> v + 1);
        assertEquals(1, count.get(), "Value should be 1 after increment");
    }

    @Test
    void testSubscriptions() {
        Ref<Integer> count = new Ref<>(0);
        List<Integer> values = new ArrayList<>();
        Disposable sub = count.subscribe((v) -> values.add(v));

        count.set(10);
        count.set(20);

        assertEquals(2, values.size(), "Should have received 2 updates");
        assertEquals(10, values.get(0), "First update should be 10");
        assertEquals(20, values.get(1), "Second update should be 20");
    }

    @Test
    void testDisposal() {
        Ref<Integer> count = new Ref<>(0);
        List<Integer> values = new ArrayList<>();
        Disposable sub = count.subscribe((v) -> values.add(v));

        count.set(10);
        sub.dispose();
        count.set(20);

        assertEquals(1, values.size(), "Should not receive updates after disposal");
        assertEquals(10, values.getFirst(), "First update should be 10");
    }

    @Test
    void testNoNotificationOnSameValue() {
        Ref<String> text = new Ref<>("hello");
        AtomicInteger notificationCount = new AtomicInteger();
        text.subscribe(v -> notificationCount.getAndIncrement());

        text.set("hello"); // Same value
        assertEquals(0, notificationCount.get(), "Should not notify on same value");
    }
}
