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

public class SwingSimpleDebounceApp extends JFrame {

    private final WritableRef<String> queryRef;

    private final ResourceRef<String> searchResource;

    private final ReadableRef<Boolean> isLoading;

    private final ReadableRef<String> statusText;

    public SwingSimpleDebounceApp() {
        setTitle("Simple Todo App");
        setSize(400, 300);
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

        isLoading = computed(() -> {
            System.out.println("Computed isLoading");
            return searchResource.get().isLoading();
        });

        statusText = computed(() -> {
            System.out.println("Computed statusText");
            return isLoading.get() ? "Loading..." : "Ready";
        });

        JTextField searchField = new JTextField();
        JLabel statusLabel = new JLabel();

        SwingTools.bindTextField(searchField, queryRef);
        SwingTools.bindText(statusLabel, statusText);

        JPanel topPanel = new JPanel(new BorderLayout(5, 5));
        topPanel.add(new JLabel("Search Query:"), BorderLayout.WEST);
        topPanel.add(searchField, BorderLayout.CENTER);

        JPanel mainPanel = new JPanel(new GridLayout(3, 1, 5, 5));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        mainPanel.add(topPanel);
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

            return String.format("'%s' was found successfully at %s.", query, java.time.LocalTime.now());
        });
    }

    public static void main(String[] args) {
        var runtime = JSignals.initRuntime();
        SwingUtilities.invokeLater(() -> {
            SwingSimpleDebounceApp app = new SwingSimpleDebounceApp();
            app.setVisible(true);
        });
    }

}
