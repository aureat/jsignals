# JSignals
Fine-grained, thread-safe reactive programming library for Java, inspired by state management patterns of Javascript and libraries like Vue and SolidJS.
- Reactivity with minimal recomputation, using dependency tracking at the value level.
- Direct subscription to state changes with efficient observer pattern implementation.
- Thread-safe state management, designed for concurrent environments, leveraging Java concurrency primitives for safe updates.
- Asynchronous resource management with automatic cleanup, allowing for safe resource handling in reactive contexts.

```java
import static jsignals.JSignals.*;

import jsignals.core.Ref;
import jsignals.core.ComputedRef;
import jsignals.runtime.JSignalsRuntime;

class JSignalsExample() {

    public static void main(String[] args) {
        try (JSignalsRuntime runtime = initRuntime()) {
            // Create a writable state holder for a name.
            Ref<String> name = ref("Jane");

            // Create a derived, read-only value using `.map()`.
            // This `ComputedRef` automatically updates when `name` changes.
            ComputedRef<String> greeting = name.map(n -> "Hello, " + n + "!");

            // Directly subscribe to changes in the `greeting`.
            greeting.watch((_, value) -> System.out.println("Greeting changed: " + value));

            // Change the name, which will trigger the `greeting` update.
            name.set("Victor"); // Greeting changed: Hello, Victor!
        }
    }

}
```

## 🧩 Core Primitives
JSignals provides a few key building blocks for your reactive state graph.
- **`Ref<T>`**: The fundamental readable and writable state holder. This is the root of your reactive data. Any Ref can have direct subscribers that are notified of changes, and it can be a dependency for other computations.
- **`ComputedRef<T>`**: A read-only signal whose value is computed from other signals. It is "smart"—lazy by default to save resources, but becomes eager (updates proactively) as soon as it has active subscribers, making it perfect for UI updates.
- **`Trigger`**: A stateless signal used for event-like notifications that don't carry a value, such as a manual refresh signal.
- **`ResourceRef<T>`**: A specialized signal for managing the lifecycle of asynchronous operations (like API calls). It automatically handles loading, success, and error states, and can be configured to automatically fetch data and debounce requests.

## 🔄 Side Effects with `effect()`
An effect is a special type of computation that is designed to run side effects, like logging, saving to a database, or updating non-reactive parts of your application. It runs once immediately to register its dependencies and then re-runs whenever any of those dependencies change.

```java
// Create a writable state holder for a counter.
Ref<Integer> counter = ref(0);

// This effect will run whenever `counter` changes.
effect(() -> updateText(counter.get()));

// Increment the counter, triggering the subscription.
counter.update(v -> v + 1);
```

## ⚡ Asynchronous Operations with `ResourceRef`

```java
import jsignals.core.Ref;
import static jsignals.JSignals.*;

class JSignalsExample() {
    public static void main(String[] args) {
        // Create a writable state holder for a counter.
        Ref<Integer> counter = ref(0);

        // This effect will run whenever `counter` changes.
        effect(() -> updateUI(counter.get()));

        // Increment the counter, triggering the subscription.
        counter.update(v -> v + 1);
    }
}
```

### Dynamic dependencies with `Ref.flatMap()`

```java
// State: A Ref holding the currently selected user ID.
Ref<Integer> currentUserId = new Ref<>(101);

// A signal that automatically shows the data for the current user.
// When currentUserId changes, it stops listening to the old resource
// and starts listening to the new one.
ComputedRef<ResourceState<User>> currentUserData = currentUserId.flatMap(
        id -> resource(() -> fetchUserData(id), Duration.ofMillis(500)) // Fetch user data with a debounce of 500ms
);

// Subscribe to changes in the current user data.
effect(() -> {
    var state = currentUserData.get();
    if (state.isLoading()) {
        System.out.println("Loading user data...");
    } else if (state.isError()) {
        System.err.println("Error loading user data: " + state.getError());
    } else {
        System.out.println("Current User: " + state.getData());
    }
});
```

## 🖥️ UI Example: Debounced Search with Swing
Here’s how to create a search box that fetches results as the user types, but debounces the API calls to prevent spamming the server.

Full example at [SwingDebouncedSearchApp.java](src/main/java/jsignals/examples/SwingDebouncedSearchApp.java).

```java
import jsignals.async.ResourceRef;
import jsignals.core.*;
import jsignals.swing.JSignalBinder;
import jsignals.swing.SwingTools;

import java.awt.*;
import javax.swing.*;
import java.time.Duration;

import static jsignals.JSignals.*;

public class DebouncedSearchApp extends JFrame {

    public DebouncedSearchApp() {
        Ref<String> queryRef = ref("java");

        // Create a debounced resource that fetches search results based on the query.
        ResourceRef<String> searchResource = resource(
                () -> simulateApiFetch(queryRef.get()), // Depends on the query
                Duration.ofMillis(300)                  // Debounce for 300ms
        );

        // Resource state handling
        ReadableRef<Boolean> isLoading = searchResource.map(state -> state.isLoading());
        ReadableRef<String> resultText = searchResource.map(state ->
                state.getData() != null ? "Result: " + state.getData() : "Type to search..."
        );

        // UI Bindings
        JTextField searchField = new JTextField();
        JLabel resultLabel = new JLabel("Initializing...");
        JProgressBar loadingSpinner = new JProgressBar();
        loadingSpinner.setIndeterminate(true);

        // Bind state directly to UI components
        SwingTools.bindTextField(searchField, queryRef);
        SwingTools.bindText(resultLabel, resultText);
        SwingTools.bindVisible(loadingSpinner, isLoading);

        // Layout...
        // ...
    }

}
```

## More UI Examples
- [SwingListModelTodoApp](src/main/java/jsignals/examples/SwingListModelTodoApp.java)
- [SwingCustomTodoApp](src/main/java/jsignals/examples/SwingCustomTodoApp.java)
- [SwingAdvancedTodoApp](src/main/java/jsignals/examples/SwingAdvancedTodoApp.java)

## Implementation Details

### 🧵 Thread-Safety & Concurrency

JSignals is designed for safe use in concurrent environments. All core primitives (`Ref`, `ComputedRef`, `ResourceRef`) use Java concurrency primitives (`AtomicReference`, locks) to ensure that reads and writes are thread-safe. Updates to state and notification of dependents are atomic, preventing race conditions even when accessed from multiple threads.

### ⚙️ Runtime & Executor

JSignals manages its own runtime (`JSignalsRuntime`), which encapsulates a custom executor (`JSignalsExecutor`). This executor uses Java virtual threads for lightweight, scalable concurrency, and a scheduled thread pool for delayed or debounced tasks. All async operations, effects, and recomputations are scheduled through this executor, ensuring that reactive updates do not block the main thread and are efficiently managed.

### 🔗 Reactive Graph Implementation

The reactive graph is built from `Ref` (state holders), `ComputedRef` (derived values), and `ResourceRef` (async state). Dependencies between these nodes are tracked at runtime using a `DependencyTracker`. When a value changes, only the directly affected dependents are notified and recomputed, minimizing unnecessary work.

### ⚡ Async Resources

`ResourceRef` wraps async operations in a reactive interface. It uses a `CompletableFuture`-based fetcher, tracks loading/error/success states, and can debounce fetches to avoid redundant requests. Cancellation is supported, and state transitions are thread-safe.

### 🕸️ Dependency Tracking

The `DependencyTracker` maintains a mapping between dependencies and their dependents using weak references. When a computation runs, it records all accessed dependencies. On updates, only affected dependents are notified, and the dependency graph is kept up-to-date automatically.

### ⏱️ Update Scheduling

All updates, recomputations, and effects are scheduled through the `JSignalsExecutor`. This ensures that:
- Computations run on virtual threads, avoiding blocking the main thread.
- Delayed/debounced updates use the scheduler for precise timing.
- Effects and async resource updates are isolated from each other, improving scalability and responsiveness.

---

## 📜 License
This project is licensed under the MIT License.