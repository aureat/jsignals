package jsignals.examples;

import jsignals.JSignals;
import jsignals.async.ResourceRef;
import jsignals.core.ReadableRef;
import jsignals.core.WritableRef;
import jsignals.swing.SwingTools;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import static jsignals.JSignals.*;

/**
 * A simple Swing application demonstrating debounced search functionality
 * using JSignals reactive primitives.
 */
public class SwingDebouncedSearchApp extends JFrame {

    WritableRef<String> queryRef;
    ResourceRef<String> searchResource;
    ReadableRef<Boolean> searchResourceIsLoading;
    ReadableRef<String> searchResourceData;
    ReadableRef<Boolean> searchResourceHasError;
    ReadableRef<String> searchResourceErrorMessage;
    ReadableRef<String> resultText;
    ReadableRef<String> errorText;
    ReadableRef<Boolean> isSearchEnabled;
    ReadableRef<String> statusText;

    public SwingDebouncedSearchApp() {
        setTitle("Reactive Search with JSignals");
        setSize(500, 250);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Shutting down JSignals runtime...");
                JSignals.shutdownRuntime();
                super.windowClosing(e);
            }
        });

        // The query text, two-way bound to the text field.
        queryRef = ref("java");

        // The resource that performs the search. It depends on queryRef.
        // It's debounced to prevent API spam while the user is typing.
        searchResource = resource(
                () -> {
                    String query = queryRef.get(); // Creates the dependency!
                    System.out.println("Fetching for: " + query);
                    return simulateApiFetch(query);
                },
                true, // autoFetch on creation
                Duration.ofMillis(300) // debounce duration
        );

        searchResourceIsLoading = computed(() -> {
            System.out.println("Computed isLoading");
            return searchResource.get().isLoading();
        });

        searchResourceData = computed(() -> {
            System.out.println("Computed searchResourceData");
            return searchResource.get().getData();
        });

        searchResourceHasError = computed(() -> {
            System.out.println("Computed hasError");
            return searchResource.get().isError();
        });

        searchResourceErrorMessage = computed(() -> {
            Throwable error = searchResource.get().getError();
            System.out.println("Computed searchResourceError");
            return error != null ? error.getMessage() : "";
        });

        resultText = computed(() -> {
            System.out.println("Computed resultText");
            var data = searchResourceData.get();
            var isLoading = searchResourceIsLoading.get();
            var hasError = searchResourceHasError.get();

            if (isLoading) {
                return "Searching...";
            } else if (hasError) {
                return "Error: " + searchResourceErrorMessage.get();
            } else {
                return data != null ? "Result: " + data : "No results yet";
            }
        });

        errorText = computed(() -> {
            Throwable error = searchResource.get().getError();
            System.out.println("Computed errorText");
            return error != null ? "Error: " + error.getMessage() : "";
        });

        isSearchEnabled = computed(() -> {
            System.out.println("Computed isSearchEnabled");
            return !searchResourceIsLoading.get() && !queryRef.get().trim().isEmpty();
        });

        statusText = computed(() -> {
            System.out.println("Computed statusText");
            return searchResourceHasError.get() ? "Error: " + searchResourceErrorMessage.get() :
                    (searchResourceIsLoading.get() ? "Loading..." : "Ready");
        });

        JTextField searchField = new JTextField();
        JButton searchButton = new JButton("Search");
        JLabel resultLabel = new JLabel();
        JLabel statusLabel = new JLabel();

        statusLabel.setForeground(Color.GREEN);

        searchResourceHasError.watch(hasError ->
                statusLabel.setForeground(hasError ? Color.RED : Color.GREEN));

        // Manual fetch on button click (in addition to automatic refetching)
        searchButton.addActionListener(_ -> searchResource.fetch());


        // Create bindings using our utility class
        SwingTools.bindTextField(searchField, queryRef);
        SwingTools.bindText(resultLabel, resultText);
        SwingTools.bindEnabled(searchButton, isSearchEnabled);
        SwingTools.bindText(statusLabel, statusText);


        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Search Query:"), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);
        topPanel.add(searchButton, BorderLayout.EAST);

        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel);
        mainPanel.add(resultLabel);
        mainPanel.add(statusLabel);

        setContentPane(mainPanel);
    }

    // Helper to simulate a network call
    private CompletableFuture<String> simulateApiFetch(String query) {
        return submitTask(() -> {
            try {
                Thread.sleep(1000); // Simulate latency
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            if (query.equalsIgnoreCase("error")) {
                throw new RuntimeException("The API call failed!");
            }
            return String.format("'%s' was found successfully at %s.", query, java.time.LocalTime.now());
        });
    }

    public static void main(String[] args) {
        var runtime = initRuntime();
        SwingUtilities.invokeLater(() -> new SwingDebouncedSearchApp().setVisible(true));
    }

}
