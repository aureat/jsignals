package jsignals.core;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A reactive reference to an immutable list.
 * <p>
 * ListRef provides methods to manage a list reactively. All modification
 * operations result in a new immutable list, and changes trigger notifications
 * to subscribers and dependents.
 *
 * @param <T> The type of elements in the list.
 */
public class ListRef<T> implements ReadableRef<List<T>> {

    private final Ref<List<T>> listRef;

    /**
     * Creates a new ListRef initialized with an empty immutable list.
     */
    public ListRef() {
        this.listRef = new Ref<>(Collections.emptyList());
    }

    /**
     * Creates a new ListRef initialized with a copy of the provided list.
     * The provided list is copied to ensure immutability.
     *
     * @param initialList The initial list of elements. Must not be null.
     */
    public ListRef(Collection<? extends T> initialList) {
        Objects.requireNonNull(initialList, "Initial list cannot be null");
        this.listRef = new Ref<>(List.copyOf(initialList));
    }

    /**
     * Creates a new ListRef initialized with the provided elements.
     *
     * @param elements The initial elements for the list.
     */
    @SafeVarargs
    public ListRef(T... elements) {
        Objects.requireNonNull(elements, "Initial elements cannot be null");
        this.listRef = new Ref<>(List.of(elements));
    }

    @Override
    public List<T> get() {
        return listRef.get();
    }

    @Override
    public List<T> getValue() {
        return listRef.getValue();
    }

    @Override
    public Disposable watch(Consumer<List<T>> listener) {
        return listRef.watch(listener);
    }

    @Override
    public Disposable watch(BiConsumer<List<T>, List<T>> listener) {
        return listRef.watch(listener);
    }

    // Modification methods that ensure immutability

    /**
     * Adds an element to the end of the list.
     *
     * @param element The element to add.
     */
    public void add(T element) {
        List<T> currentList = listRef.getValue();
        List<T> newList = new ArrayList<>(currentList);
        newList.add(element);
        listRef.set(List.copyOf(newList));
    }

    /**
     * Inserts an element at the specified position in the list.
     *
     * @param index   The index at which the specified element is to be inserted.
     * @param element The element to be inserted.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index > size()}).
     */
    public void add(int index, T element) {
        List<T> currentList = listRef.getValue();
        List<T> newList = new ArrayList<>(currentList);
        newList.add(index, element);
        listRef.set(List.copyOf(newList));
    }

    /**
     * Adds all of the elements in the specified collection to the end of this list.
     *
     * @param elements Collection containing elements to be added to this list.
     *                 Must not be null.
     */
    public void addAll(Collection<? extends T> elements) {
        Objects.requireNonNull(elements, "Elements collection cannot be null");
        List<T> currentList = listRef.getValue();
        List<T> newList = new ArrayList<>(currentList);
        newList.addAll(elements);
        listRef.set(List.copyOf(newList));
    }

    /**
     * Removes the first occurrence of the specified element from this list, if it is present.
     *
     * @param element Element to be removed from this list, if present.
     * @return {@code true} if this list contained the specified element.
     */
    public boolean remove(T element) {
        List<T> currentList = listRef.getValue();
        List<T> newList = new ArrayList<>(currentList);
        boolean removed = newList.remove(element);
        if (removed) {
            listRef.set(List.copyOf(newList));
        }
        return removed;
    }

    /**
     * Removes the element at the specified position in this list.
     *
     * @param index The index of the element to be removed.
     * @return The element previously at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()}).
     */
    public T removeAt(int index) {
        List<T> currentList = listRef.getValue();
        if (index < 0 || index >= currentList.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + currentList.size());
        }
        List<T> newList = new ArrayList<>(currentList);
        T removedElement = newList.remove(index);
        listRef.set(List.copyOf(newList));
        return removedElement;
    }

    /**
     * Removes all of this collection's elements that satisfy the given predicate.
     *
     * @param filter A predicate which returns {@code true} for elements to be removed.
     *               Must not be null.
     * @return {@code true} if any elements were removed.
     */
    public boolean removeIf(Predicate<? super T> filter) {
        Objects.requireNonNull(filter, "Filter predicate cannot be null");
        List<T> currentList = listRef.getValue();
        List<T> newList = new ArrayList<>(currentList);
        boolean removed = newList.removeIf(filter);
        if (removed) {
            listRef.set(List.copyOf(newList));
        }
        return removed;
    }

    /**
     * Replaces the element at the specified position in this list with the specified element.
     *
     * @param index   Index of the element to replace.
     * @param element Element to be stored at the specified position.
     * @return The element previously at the specified position.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()}).
     */
    public T set(int index, T element) {
        List<T> currentList = listRef.getValue();
        if (index < 0 || index >= currentList.size()) {
            throw new IndexOutOfBoundsException("Index: " + index + ", Size: " + currentList.size());
        }
        List<T> newList = new ArrayList<>(currentList);
        T oldElement = newList.set(index, element);
        listRef.set(List.copyOf(newList));
        return oldElement;
    }

    /**
     * Replaces each element of this list with the result of applying the operator to that element.
     *
     * @param operator The operator to apply to each element. Must not be null.
     */
    public void replaceAll(UnaryOperator<T> operator) {
        Objects.requireNonNull(operator, "Operator cannot be null");
        List<T> currentList = listRef.getValue();
        List<T> newList = currentList.stream()
                .map(operator)
                .collect(Collectors.toList());
        listRef.set(List.copyOf(newList));
    }

    /**
     * Removes all of the elements from this list. The list will be empty after this call returns.
     */
    public void clear() {
        listRef.set(Collections.emptyList());
    }

    // Query methods that track access

    /**
     * Returns the number of elements in this list.
     * Access to this method is tracked for reactivity.
     *
     * @return The number of elements in this list.
     */
    public int size() {
        return get().size();
    }

    /**
     * Returns {@code true} if this list contains no elements.
     * Access to this method is tracked for reactivity.
     *
     * @return {@code true} if this list contains no elements.
     */
    public boolean isEmpty() {
        return get().isEmpty();
    }

    /**
     * Returns {@code true} if this list contains the specified element.
     * Access to this method is tracked for reactivity.
     *
     * @param o Element whose presence in this list is to be tested.
     * @return {@code true} if this list contains the specified element.
     */
    public boolean contains(Object o) {
        return get().contains(o);
    }

    /**
     * Returns the element at the specified position in this list.
     * Access to this method is tracked for reactivity.
     *
     * @param index Index of the element to return.
     * @return The element at the specified position in this list.
     * @throws IndexOutOfBoundsException if the index is out of range
     *                                   ({@code index < 0 || index >= size()}).
     */
    public T get(int index) {
        return get().get(index);
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     * Access to this method is tracked for reactivity.
     *
     * @param o Element to search for.
     * @return The index of the first occurrence of the specified element, or -1.
     */
    public int indexOf(Object o) {
        return get().indexOf(o);
    }

    /**
     * Returns the index of the last occurrence of the specified element in this list,
     * or -1 if this list does not contain the element.
     * Access to this method is tracked for reactivity.
     *
     * @param o Element to search for.
     * @return The index of the last occurrence of the specified element, or -1.
     */
    public int lastIndexOf(Object o) {
        return get().lastIndexOf(o);
    }

    /**
     * Returns an immutable list containing the portion of this list between the specified
     * {@code fromIndex}, inclusive, and {@code toIndex}, exclusive.
     * Access to this method is tracked for reactivity.
     *
     * @param fromIndex Low endpoint (inclusive) of the subList.
     * @param toIndex   High endpoint (exclusive) of the subList.
     * @return An immutable view of the specified range within this list.
     * @throws IndexOutOfBoundsException for an illegal endpoint index value
     *                                   ({@code fromIndex < 0 || toIndex > size || fromIndex > toIndex}).
     */
    public List<T> subList(int fromIndex, int toIndex) {
        return List.copyOf(get().subList(fromIndex, toIndex));
    }

    /**
     * Returns a sequential {@code Stream} with this list as its source.
     * Access to this method is tracked for reactivity.
     *
     * @return A sequential {@code Stream} over the elements in this list.
     */
    public Stream<T> stream() {
        return get().stream();
    }

    /**
     * Returns a possibly parallel {@code Stream} with this list as its source.
     * Access to this method is tracked for reactivity.
     *
     * @return A possibly parallel {@code Stream} over the elements in this list.
     */
    public Stream<T> parallelStream() {
        return get().parallelStream();
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     * The returned iterator is immutable.
     * Access to this method is tracked for reactivity.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator<T> iterator() {
        return get().iterator(); // List.copyOf().iterator() is already immutable
    }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence).
     * The returned list iterator is immutable.
     * Access to this method is tracked for reactivity.
     *
     * @return a list iterator over the elements in this list (in proper sequence)
     */
    public ListIterator<T> listIterator() {
        return get().listIterator(); // List.copyOf().listIterator() is already immutable
    }

    /**
     * Returns a list iterator over the elements in this list (in proper sequence),
     * starting at the specified position in the list.
     * The returned list iterator is immutable.
     * Access to this method is tracked for reactivity.
     *
     * @param index index of the first element to be returned from the list iterator (by a call to next)
     * @return a list iterator over the elements in this list (in proper sequence), starting at the specified position in the list
     * @throws IndexOutOfBoundsException if the index is out of range (index < 0 || index > size())
     */
    public ListIterator<T> listIterator(int index) {
        return get().listIterator(index); // List.copyOf().listIterator() is already immutable
    }


    @Override
    public String toString() {
        return "ListRef(" + listRef.getValue() + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ListRef<?> listRef1 = (ListRef<?>) o;
        return Objects.equals(listRef.getValue(), listRef1.listRef.getValue());
    }

    @Override
    public String getName() {
        return String.format("ListRef@%s", Integer.toHexString(hashCode()));
    }

}

