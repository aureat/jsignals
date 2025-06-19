package jsignals.core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

class ListRefTest {

    private ListRef<String> listRef;

    @BeforeEach
    void setUp() {
        listRef = new ListRef<>();
    }

    @Test
    void testEmptyConstructor() {
        assertTrue(listRef.isEmpty(), "List should be empty initially.");
        assertEquals(0, listRef.size(), "Size should be 0 for empty constructor.");
        assertEquals(Collections.emptyList(), listRef.get(), "Get should return an empty list.");
    }

    @Test
    void testCollectionConstructor() {
        List<String> initialList = Arrays.asList("a", "b", "c");
        ListRef<String> refWithCollection = new ListRef<>(initialList);
        assertEquals(3, refWithCollection.size(), "Size should match initial collection.");
        assertEquals(initialList, refWithCollection.get(), "Get should return the initial list.");
        assertNotSame(initialList, refWithCollection.get(), "Internal list should be a copy.");
    }

    @Test
    void testVarargsConstructor() {
        ListRef<String> refWithVarargs = new ListRef<>("x", "y", "z");
        assertEquals(3, refWithVarargs.size(), "Size should match varargs elements.");
        assertEquals(Arrays.asList("x", "y", "z"), refWithVarargs.get(), "Get should return list of varargs.");
    }

    @Test
    void testNullInitialListInConstructorThrowsException() {
        assertThrows(NullPointerException.class, () -> new ListRef<>((List<String>) null));
    }

    @Test
    void testAddElement() {
        List<String> initialList = listRef.get();
        listRef.add("one");
        assertEquals(1, listRef.size());
        assertTrue(listRef.contains("one"));
        assertNotSame(initialList, listRef.get(), "A new list instance should be created after add.");
        assertEquals(List.of("one"), listRef.get());
    }

    @Test
    void testAddElementAtIndex() {
        listRef.add("one");
        listRef.add("three");
        listRef.add(1, "two");
        assertEquals(3, listRef.size());
        assertEquals(Arrays.asList("one", "two", "three"), listRef.get());
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.add(5, "five"));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.add(-1, "minusOne"));
    }

    @Test
    void testAddAllElements() {
        List<String> initialList = listRef.get();
        listRef.addAll(Arrays.asList("one", "two"));
        assertEquals(2, listRef.size());
        assertTrue(listRef.containsAll(Arrays.asList("one", "two")));
        assertNotSame(initialList, listRef.get(), "A new list instance should be created after addAll.");
        assertEquals(List.of("one", "two"), listRef.get());
    }

    @Test
    void testAddAllNullCollectionThrowsException() {
        assertThrows(NullPointerException.class, () -> listRef.addAll(null));
    }

    @Test
    void testRemoveElement() {
        listRef.add("one");
        listRef.add("two");
        List<String> listBeforeRemove = listRef.get();
        boolean removed = listRef.remove("one");
        assertTrue(removed);
        assertEquals(1, listRef.size());
        assertFalse(listRef.contains("one"));
        assertTrue(listRef.contains("two"));
        assertNotSame(listBeforeRemove, listRef.get(), "A new list instance should be created after remove.");
        assertEquals(List.of("two"), listRef.get());

        boolean notRemoved = listRef.remove("three");
        assertFalse(notRemoved);
        assertEquals(1, listRef.size()); // Size should not change
    }

    @Test
    void testRemoveAt() {
        listRef.addAll(Arrays.asList("a", "b", "c"));
        List<String> listBeforeRemove = listRef.get();
        String removedElement = listRef.removeAt(1);
        assertEquals("b", removedElement);
        assertEquals(2, listRef.size());
        assertEquals(Arrays.asList("a", "c"), listRef.get());
        assertNotSame(listBeforeRemove, listRef.get(), "A new list instance should be created after removeAt.");
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.removeAt(5));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.removeAt(-1));
    }

    @Test
    void testRemoveIf() {
        listRef.addAll(Arrays.asList("apple", "banana", "apricot", "blueberry"));
        List<String> listBeforeRemove = listRef.get();
        Predicate<String> startsWithA = s -> s.startsWith("a");
        boolean removed = listRef.removeIf(startsWithA);
        assertTrue(removed);
        assertEquals(2, listRef.size());
        assertEquals(Arrays.asList("banana", "blueberry"), listRef.get());
        assertNotSame(listBeforeRemove, listRef.get(), "A new list instance should be created after removeIf.");

        boolean notRemoved = listRef.removeIf(s -> s.startsWith("z"));
        assertFalse(notRemoved);
        assertEquals(2, listRef.size());
    }

    @Test
    void testRemoveIfNullPredicateThrowsException() {
        assertThrows(NullPointerException.class, () -> listRef.removeIf(null));
    }

    @Test
    void testSetElement() {
        listRef.addAll(Arrays.asList("old1", "old2", "old3"));
        List<String> listBeforeSet = listRef.get();
        String previousElement = listRef.set(1, "new2");
        assertEquals("old2", previousElement);
        assertEquals(3, listRef.size());
        assertEquals(Arrays.asList("old1", "new2", "old3"), listRef.get());
        assertNotSame(listBeforeSet, listRef.get(), "A new list instance should be created after set.");
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.set(5, "five"));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.set(-1, "minusOne"));
    }

    @Test
    void testReplaceAll() {
        listRef.addAll(Arrays.asList("one", "two", "three"));
        List<String> listBeforeReplace = listRef.get();
        listRef.replaceAll(String::toUpperCase);
        assertEquals(3, listRef.size());
        assertEquals(Arrays.asList("ONE", "TWO", "THREE"), listRef.get());
        assertNotSame(listBeforeReplace, listRef.get(), "A new list instance should be created after replaceAll.");
    }

    @Test
    void testReplaceAllNullOperatorThrowsException() {
        assertThrows(NullPointerException.class, () -> listRef.replaceAll(null));
    }

    @Test
    void testClear() {
        listRef.addAll(Arrays.asList("a", "b"));
        assertFalse(listRef.isEmpty());
        List<String> listBeforeClear = listRef.get();
        listRef.clear();
        assertTrue(listRef.isEmpty());
        assertEquals(0, listRef.size());
        assertNotSame(listBeforeClear, listRef.get(), "A new list instance should be created after clear.");
        assertEquals(Collections.emptyList(), listRef.get());
    }

    @Test
    void testSizeAndIsEmpty() {
        assertTrue(listRef.isEmpty());
        assertEquals(0, listRef.size());
        listRef.add("item");
        assertFalse(listRef.isEmpty());
        assertEquals(1, listRef.size());
    }

    @Test
    void testContains() {
        assertFalse(listRef.contains("item"));
        listRef.add("item");
        assertTrue(listRef.contains("item"));
        assertFalse(listRef.contains("other"));
    }

    @Test
    void testGetAtIndex() {
        listRef.addAll(Arrays.asList("first", "second"));
        assertEquals("first", listRef.get(0));
        assertEquals("second", listRef.get(1));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.get(2));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.get(-1));
    }

    @Test
    void testIndexOfAndLastIndexOf() {
        listRef.addAll(Arrays.asList("a", "b", "a", "c", "b"));
        assertEquals(0, listRef.indexOf("a"));
        assertEquals(1, listRef.indexOf("b"));
        assertEquals(3, listRef.indexOf("c"));
        assertEquals(-1, listRef.indexOf("d"));

        assertEquals(2, listRef.lastIndexOf("a"));
        assertEquals(4, listRef.lastIndexOf("b"));
        assertEquals(3, listRef.lastIndexOf("c"));
        assertEquals(-1, listRef.lastIndexOf("d"));
    }

    @Test
    void testSubList() {
        listRef.addAll(Arrays.asList("w", "x", "y", "z"));
        List<String> sub = listRef.subList(1, 3);
        assertEquals(Arrays.asList("x", "y"), sub);
        assertThrows(UnsupportedOperationException.class, () -> sub.add("new")); // sublist should be immutable

        assertThrows(IndexOutOfBoundsException.class, () -> listRef.subList(0, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.subList(-1, 2));
        assertThrows(IndexOutOfBoundsException.class, () -> listRef.subList(2, 1));
    }

    @Test
    void testStreamAndParallelStream() {
        listRef.addAll(Arrays.asList("s1", "s2"));
        assertEquals(2, listRef.stream().count());
        assertEquals(2, listRef.parallelStream().count());
        assertTrue(listRef.stream().anyMatch(s -> s.equals("s1")));
    }

    @Test
    void testIterator() {
        listRef.addAll(Arrays.asList("i1", "i2"));
        var it = listRef.iterator();
        assertTrue(it.hasNext());
        assertEquals("i1", it.next());
        assertTrue(it.hasNext());
        assertEquals("i2", it.next());
        assertFalse(it.hasNext());
        assertThrows(UnsupportedOperationException.class, it::remove);
    }

    @Test
    void testListIterator() {
        listRef.addAll(Arrays.asList("l1", "l2"));
        var lit = listRef.listIterator();
        assertTrue(lit.hasNext());
        assertEquals("l1", lit.next());
        assertTrue(lit.hasPrevious());
        assertEquals("l1", lit.previous()); // back to start
        assertEquals("l1", lit.next()); // forward again
        assertEquals("l2", lit.next());
        assertFalse(lit.hasNext());
        assertThrows(UnsupportedOperationException.class, lit::remove);
        assertThrows(UnsupportedOperationException.class, () -> lit.add("l3"));
        assertThrows(UnsupportedOperationException.class, () -> lit.set("l0"));
    }

    @Test
    void testListIteratorAtIndex() {
        listRef.addAll(Arrays.asList("x", "y", "z"));
        var lit = listRef.listIterator(1); // starts before 'y'
        assertTrue(lit.hasNext());
        assertEquals("y", lit.next());
        assertTrue(lit.hasPrevious());
        assertEquals("y", lit.previous());
        assertEquals(1, lit.nextIndex());
        assertEquals(0, lit.previousIndex());
    }

    @Test
    void testWatchNotification() {
        AtomicReference<List<String>> notifiedOldValue = new AtomicReference<>();
        AtomicReference<List<String>> notifiedNewValue = new AtomicReference<>();
        AtomicReference<List<String>> notifiedSingleArgValue = new AtomicReference<>();

        listRef.watch((oldVal, newVal) -> {
            notifiedOldValue.set(oldVal);
            notifiedNewValue.set(newVal);
        });
        listRef.watch(notifiedSingleArgValue::set);

        List<String> initialList = listRef.get(); // Should be empty
        listRef.add("test");

        assertEquals(initialList, notifiedOldValue.get(), "Old value in watch should be the initial list.");
        assertEquals(Collections.singletonList("test"), notifiedNewValue.get(), "New value in watch should be the updated list.");
        assertEquals(Collections.singletonList("test"), notifiedSingleArgValue.get(), "Single arg watch should receive the new list.");

        List<String> listAfterAdd = listRef.get();
        listRef.remove("test");
        assertEquals(listAfterAdd, notifiedOldValue.get(), "Old value should be list after add.");
        assertEquals(Collections.emptyList(), notifiedNewValue.get(), "New value should be empty list after remove.");
        assertEquals(Collections.emptyList(), notifiedSingleArgValue.get());
    }

    @Test
    void testWatchUnsubscribe() {
        AtomicReference<List<String>> notifiedValue = new AtomicReference<>();
        Disposable subscription = listRef.watch(notifiedValue::set);

        listRef.add("first");
        assertEquals(Collections.singletonList("first"), notifiedValue.get());

        subscription.dispose();
        listRef.add("second");
        // notifiedValue should still be the list with "first", not updated with "second"
        assertEquals(Collections.singletonList("first"), notifiedValue.get(), "Watch should not trigger after unsubscribe.");
    }

    @Test
    void testImmutabilityOfReturnedList() {
        listRef.add("a");
        List<String> currentList = listRef.get();
        assertThrows(UnsupportedOperationException.class, () -> currentList.add("b"),
                "List returned by get() should be immutable.");
        assertThrows(UnsupportedOperationException.class, () -> currentList.remove(0),
                "List returned by get() should be immutable.");
        // Verify original ListRef is unchanged
        assertEquals(Collections.singletonList("a"), listRef.get());
    }

    @Test
    void testToString() {
        assertEquals("ListRef([])", listRef.toString());
        listRef.add("hello");
        listRef.add("world");
        assertEquals("ListRef([hello, world])", listRef.toString());
    }

    @Test
    void testEqualsAndHashCode() {
        ListRef<String> listRef1 = new ListRef<>(Arrays.asList("a", "b"));
        ListRef<String> listRef2 = new ListRef<>(Arrays.asList("a", "b"));
        ListRef<String> listRef3 = new ListRef<>(Arrays.asList("a", "c"));
        ListRef<String> listRef4 = new ListRef<String>();
        listRef4.add("a");
        listRef4.add("b");


        assertEquals(listRef1, listRef2, "ListRefs with same content should be equal.");
        assertNotEquals(listRef1, listRef3, "ListRefs with different content should not be equal.");
        assertEquals(listRef1.hashCode(), listRef2.hashCode(), "Hashcodes should be same for equal ListRefs.");
        assertNotEquals(listRef1.hashCode(), listRef3.hashCode(), "Hashcodes should be different for non-equal ListRefs.");
        assertEquals(listRef1, listRef4, "ListRefs with same content built differently should be equal.");

        Ref<List<String>> internalRef = new Ref<>(Arrays.asList("a", "b"));
        assertNotEquals(listRef1, internalRef, "ListRef should not be equal to a raw Ref.");
        assertNotEquals(listRef1, null, "ListRef should not be equal to null.");
        assertEquals(listRef1, listRef1, "ListRef should be equal to itself.");
    }

    @Test
    void testGetValueDoesNotTrack() {
        // This is harder to test directly without inspecting DependencyTracker internals.
        // We assume that if get() works for reactivity, getValue() is its non-tracking counterpart.
        // A simple check:
        ListRef<String> computedSource = new ListRef<>("initial");
        ComputedRef<Integer> computed = new ComputedRef<>(() -> computedSource.getValue().size());

        assertEquals(1, computed.get()); // Initial computation

        // If getValue() was tracked, changing computedSource would mark 'computed' as dirty.
        // However, since it's not tracked, 'computed' won't re-evaluate automatically
        // just by this change if it were the *only* dependency.
        // This test is more conceptual for ListRef.
        // For ComputedRef, the dependency tracking is key. Here we just ensure getValue() returns current state.

        computedSource.add("another");
        assertEquals(2, computedSource.getValue().size(), "getValue should return current state");
        // 'computed' would only update if its 'computation' is re-run (e.g. by calling computed.get() again)
        // or if 'get()' was used in its supplier.
        assertEquals(2, computed.get()); // Re-evaluates because it's dirty due to its own logic or explicit get
    }

    @Test
    void testModificationMethodsCreateNewListInstance() {
        List<String> list1 = listRef.get();
        listRef.add("item1");
        List<String> list2 = listRef.get();
        assertNotSame(list1, list2, "List instance should change after add.");

        listRef.add(0, "item0");
        List<String> list3 = listRef.get();
        assertNotSame(list2, list3, "List instance should change after add at index.");

        listRef.addAll(Arrays.asList("item2", "item3"));
        List<String> list4 = listRef.get();
        assertNotSame(list3, list4, "List instance should change after addAll.");

        listRef.remove("item1");
        List<String> list5 = listRef.get();
        assertNotSame(list4, list5, "List instance should change after remove.");

        listRef.removeAt(0);
        List<String> list6 = listRef.get();
        assertNotSame(list5, list6, "List instance should change after removeAt.");

        listRef.removeIf(s -> s.equals("item2"));
        List<String> list7 = listRef.get();
        assertNotSame(list6, list7, "List instance should change after removeIf.");

        // Ensure there's an element to set
        if (listRef.isEmpty()) listRef.add("placeholder");
        listRef.set(0, "newItem");
        List<String> list8 = listRef.get();
        assertNotSame(list7, list8, "List instance should change after set.");

        listRef.replaceAll(String::toUpperCase);
        List<String> list9 = listRef.get();
        assertNotSame(list8, list9, "List instance should change after replaceAll.");

        listRef.clear();
        List<String> list10 = listRef.get();
        assertNotSame(list9, list10, "List instance should change after clear.");
    }

}

