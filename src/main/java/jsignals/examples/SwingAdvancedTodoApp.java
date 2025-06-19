package jsignals.examples;

import jsignals.JSignals;
import jsignals.core.ComputedRef;
import jsignals.core.ReadableRef;
import jsignals.core.WritableRef;
import jsignals.swing.SwingTools;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.*;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static jsignals.JSignals.computed;
import static jsignals.JSignals.ref;

public class SwingAdvancedTodoApp extends JFrame {

    // --- Enums for State ---
    enum StatusFilter { ALL, ACTIVE, COMPLETED }

    // --- 1. STATE DEFINITION (The "Model") ---
    private final WritableRef<List<TodoItem>> todosRef = ref(new ArrayList<>());

    private final WritableRef<String> newTaskInputRef = ref("");

    private final WritableRef<String> searchQueryRef = ref("");

    private final WritableRef<StatusFilter> statusFilterRef = ref(StatusFilter.ALL);

    private final WritableRef<Set<String>> selectedLabelsRef = ref(new HashSet<>());

    private final ReadableRef<List<TodoItem>> filteredTodosRef;

    private final ReadableRef<Set<String>> allLabelsRef;

    public SwingAdvancedTodoApp() {
        setTitle("JSignals Todo App");
        setSize(600, 800);
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

        // --- 2. DERIVED STATE (ComputedRefs) ---
        // This is where the magic happens. We derive data from our primary state.

        // The list of todos that should be visible after applying all filters.
        filteredTodosRef = computed(() -> {
            List<TodoItem> allTodos = todosRef.get();
            String query = searchQueryRef.get().toLowerCase();
            StatusFilter statusFilter = statusFilterRef.get();
            Set<String> labels = selectedLabelsRef.get();

            return allTodos.stream()
                    .filter(item -> query.isEmpty() || item.getText().getValue().toLowerCase().contains(query))
                    .filter(item -> statusFilter == StatusFilter.ALL ||
                            (statusFilter == StatusFilter.COMPLETED && item.getCompleted().getValue()) ||
                            (statusFilter == StatusFilter.ACTIVE && !item.getCompleted().getValue()))
                    .filter(item -> labels.isEmpty() || !Collections.disjoint(item.getLabels(), labels))
                    .collect(Collectors.toList());
        });

        // A computed set of all unique labels across all todos.
        allLabelsRef = new ComputedRef<>(() ->
                todosRef.get().stream()
                        .flatMap(item -> item.getLabels().stream())
                        .collect(Collectors.toSet())
        );

        // --- 3. UI COMPONENTS ---
        JPanel mainPanel = new JPanel(new BorderLayout(10, 10));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // Input Panel
        JTextField newTaskField = new JTextField();
        mainPanel.add(newTaskField, BorderLayout.NORTH);

        // Filters Panel
        JPanel filtersPanel = createFiltersPanel(allLabelsRef);
        mainPanel.add(filtersPanel, BorderLayout.WEST);

        // Todo List Panel
        JPanel todoListPanel = new JPanel();
        todoListPanel.setLayout(new BoxLayout(todoListPanel, BoxLayout.Y_AXIS));
        JScrollPane scrollPane = new JScrollPane(todoListPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        // --- 4. BINDINGS AND LOGIC ---
        SwingTools.bindTextField(newTaskField, newTaskInputRef);

        // Add task logic
        newTaskField.addActionListener(e -> {
            String text = newTaskInputRef.getValue().trim();
            if (!text.isEmpty()) {
                addTask(text);
                newTaskInputRef.set("");
            }
        });

        // The core UI update logic: when the filtered list changes, re-render the UI panels.
        filteredTodosRef.watch(visibleItems -> SwingUtilities.invokeLater(() -> {
            todoListPanel.removeAll();
            for (int i = 0; i < visibleItems.size(); i++) {
                TodoItem item = visibleItems.get(i);
                TodoItemPanel itemPanel = new TodoItemPanel(item, i);
                todoListPanel.add(itemPanel);
            }
            todoListPanel.revalidate();
            todoListPanel.repaint();
        }));

        // This is needed to trigger the initial render
        todosRef.set(createInitialTodos());

        setContentPane(mainPanel);
    }

    private JPanel createFiltersPanel(ReadableRef<Set<String>> allLabelsRef) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createTitledBorder("Filters"));

        // Search Filter
        JTextField searchField = new JTextField();
        SwingTools.bindTextField(searchField, searchQueryRef);
        panel.add(new JLabel("Search:"));
        panel.add(searchField);

        JButton clearSearchButton = new JButton("Clear Search");
        clearSearchButton.addActionListener(e -> searchQueryRef.set(""));
        panel.add(clearSearchButton);


        // Status Filter
        ButtonGroup statusGroup = new ButtonGroup();
        for (StatusFilter sf : StatusFilter.values()) {
            JRadioButton radio = new JRadioButton(sf.name());
            radio.setSelected(sf == StatusFilter.ALL);
            radio.addActionListener(e -> statusFilterRef.set(sf));
            statusGroup.add(radio);
            panel.add(radio);
        }

        // Label Filter
        panel.add(new JLabel("Labels:"));
        DefaultListModel<String> labelListModel = new DefaultListModel<>();
        JList<String> labelList = new JList<>(labelListModel);
        labelList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        allLabelsRef.watch(labels -> SwingUtilities.invokeLater(() -> {
            Set<String> selected = new HashSet<>(labelList.getSelectedValuesList());
            labelListModel.clear();
            labels.stream().sorted().forEach(labelListModel::addElement);
            // Restore selection
            for (int i = 0; i < labelListModel.getSize(); i++) {
                if (selected.contains(labelListModel.getElementAt(i))) {
                    labelList.addSelectionInterval(i, i);
                }
            }
        }));

        labelList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                selectedLabelsRef.set(new HashSet<>(labelList.getSelectedValuesList()));
            }
        });

        panel.add(new JScrollPane(labelList));

        JButton clearLabelFilterButton = new JButton("Clear Label Filter");
        clearLabelFilterButton.addActionListener(e -> {
            labelList.clearSelection();
            selectedLabelsRef.set(new HashSet<>()); // Explicitly update the ref
        });
        panel.add(clearLabelFilterButton);

        return panel;
    }

    private void addTask(String rawText) {
        Pattern pattern = Pattern.compile("#(\\w+)");
        Matcher matcher = pattern.matcher(rawText);
        Set<String> labels = new HashSet<>();
        while (matcher.find()) {
            labels.add(matcher.group(1));
        }
        String cleanText = rawText.replaceAll("#\\w+", "").trim();
        TodoItem newItem = new TodoItem(cleanText, labels);
        todosRef.update(list -> {
            List<TodoItem> newList = new ArrayList<>(list);
            newList.add(newItem);
            return newList;
        });
    }

    private void deleteTask(String id) {
        todosRef.update(list -> list.stream()
                .filter(item -> !item.getId().equals(id))
                .collect(Collectors.toList()));
    }

    private class TodoItemPanel extends JPanel {

        private final TodoItem item;

        TodoItemPanel(TodoItem item, int index) {
            this.item = item;
            this.setLayout(new BorderLayout(5, 5));
            this.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, Color.LIGHT_GRAY),
                    BorderFactory.createEmptyBorder(5, 5, 5, 5)
            ));

            // Checkbox for completion status
            JCheckBox checkBox = new JCheckBox();
            SwingTools.bind(checkBox, item.getCompleted(), checkBox::setSelected);
            checkBox.addActionListener(e -> item.getCompleted().set(checkBox.isSelected()));

            // Editable label for the text
            JLabel textLabel = new JLabel();
            textLabel.setOpaque(true); // Important for background if ever set

            // Create a computed ref for the display text, handling strikethrough
            ReadableRef<String> displayTextRef = computed(() -> {
                String currentText = item.getText().getValue();
                boolean isCompleted = item.getCompleted().getValue();
                // Basic HTML escaping could be added here if 'currentText' might contain '<', '>', '&'
                // For example: currentText = currentText.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
                return isCompleted ? "<html><strike>" + currentText + "</strike></html>" : currentText;
            });

            // Bind the computed display text to the label
            SwingTools.bindText(textLabel, displayTextRef);

            // Bind the foreground color based on completion status
            // Initialize and watch for changes
            Runnable updateTextColor = () -> textLabel.setForeground(item.getCompleted().getValue() ? Color.GRAY : Color.BLACK);
            item.getCompleted().watch(completed -> SwingUtilities.invokeLater(updateTextColor));
            SwingUtilities.invokeLater(updateTextColor); // Set initial color


            // Delete button
            JButton deleteButton = new JButton("X");
            deleteButton.addActionListener(e -> deleteTask(item.getId()));

            // Labels Display
            if (!item.getLabels().isEmpty()) {
                String labelText = item.getLabels().stream().map(l -> "#" + l).collect(Collectors.joining(" "));
                JLabel labelsLabel = new JLabel(labelText);
                labelsLabel.setForeground(Color.BLUE);
                this.add(labelsLabel, BorderLayout.SOUTH);
            }

            // Inline editing functionality
            makeLabelEditable(textLabel, item.getText());

            // Drag and Drop Handler
            this.setTransferHandler(new TodoTransferHandler(item.getId())); // Pass item ID

            // Create a shared MouseAdapter for initiating the drag
            MouseAdapter dragInitiatorListener = new MouseAdapter() {
                @Override
                public void mouseDragged(MouseEvent e) {
                    // Get the TransferHandler from the TodoItemPanel itself
                    TransferHandler handler = TodoItemPanel.this.getTransferHandler();
                    if (handler != null) {
                        // Initiate the drag for the TodoItemPanel.
                        // The MouseEvent 'e' contains the original source and coordinates,
                        // but the drag operation is for TodoItemPanel.this.
                        handler.exportAsDrag(TodoItemPanel.this, e, TransferHandler.MOVE);
                    }
                }
            };

            // Add this listener to the panel itself
            this.addMouseMotionListener(dragInitiatorListener);
            // Add this listener to the textLabel as well, since it covers a large area
            // and mouse events on it might otherwise not be caught by the panel's listener.
            textLabel.addMouseMotionListener(dragInitiatorListener);

            this.add(checkBox, BorderLayout.WEST);
            this.add(textLabel, BorderLayout.CENTER);
            this.add(deleteButton, BorderLayout.EAST);

            // Set maximum height to preferred height to prevent vertical stretching in BoxLayout
            // This should be done after all components are added and preferred size is stable.
            // Deferring with SwingUtilities.invokeLater to ensure layout has been done once.
            SwingUtilities.invokeLater(() -> {
                if (isDisplayable()) { // Check if component is part of a displayable hierarchy
                    Dimension prefSize = getPreferredSize();
                    setMaximumSize(new Dimension(Integer.MAX_VALUE, prefSize.height));
                } else {
                    // Fallback or re-attempt if not yet displayable, though usually it is by this point
                    // For simplicity, we'll assume it's displayable or will be soon.
                    // A more robust solution might involve a ComponentListener for ancestor changes.
                    // However, for BoxLayout, setting it once based on initial preferred size is often sufficient.
                    Dimension prefSize = getPreferredSize(); // Calculate based on current components
                    setMaximumSize(new Dimension(Integer.MAX_VALUE, prefSize.height));
                }
            });
        }

        private void makeLabelEditable(JLabel label, WritableRef<String> textRef) {
            JTextField editor = new JTextField();
            editor.setText(textRef.getValue());

            label.addMouseListener(new MouseAdapter() {
                public void mouseClicked(MouseEvent e) {
                    if (e.getClickCount() == 2) {
                        // Populate editor with the latest text from the ref when starting edit
                        editor.setText(textRef.getValue());
                        // Replace label with text field
                        TodoItemPanel.this.remove(label);
                        TodoItemPanel.this.add(editor, BorderLayout.CENTER);
                        editor.requestFocusInWindow();
                        editor.selectAll(); // Select all text for easier editing
                        TodoItemPanel.this.revalidate();
                        TodoItemPanel.this.repaint();
                    }
                }
            });

            // Helper to switch back to the label view
            Runnable switchToLabel = () -> {
                String newText = editor.getText();

                // First update the ref (this is critical!)
                textRef.set(newText);

                // Now update the UI
                TodoItemPanel.this.remove(editor);
                TodoItemPanel.this.add(label, BorderLayout.CENTER);
                TodoItemPanel.this.revalidate();
                TodoItemPanel.this.repaint();

                // Find the TodoItem in the list and replace it with a new instance
                // This ensures the update is detected by the reactive system
                todosRef.update(todos -> {
                    List<TodoItem> newList = new ArrayList<>();
                    for (TodoItem todo : todos) {
                        if (todo.getId().equals(item.getId())) {
                            // Create a new TodoItem with the updated text
                            TodoItem updatedItem = new TodoItem(
                                    newText,  // Use the new text
                                    todo.getLabels()  // Keep the same labels
                            );
                            // Copy over the completion state
                            updatedItem.getCompleted().set(todo.getCompleted().getValue());
                            // Set the same ID to maintain identity for reactions
                            updatedItem.setId(todo.getId());
                            newList.add(updatedItem);
                        } else {
                            newList.add(todo);
                        }
                    }
                    return newList;
                });
            };

            editor.addActionListener(e -> switchToLabel.run()); // On Enter
            editor.addFocusListener(new FocusAdapter() {
                public void focusLost(FocusEvent e) {
                    switchToLabel.run();
                }
            });
        }

    }

    // --- Drag and Drop Handler ---
    private class TodoTransferHandler extends TransferHandler {

        private final String sourceItemId;

        public TodoTransferHandler(String sourceItemId) {
            this.sourceItemId = sourceItemId;
        }

        @Override
        public int getSourceActions(JComponent c) {
            return MOVE;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            // Transfer the ID of the source item
            return new StringSelection(sourceItemId);
        }

        @Override
        public boolean canImport(TransferSupport support) {
            if (!support.isDrop()) {
                return false;
            }
            // Check for String data flavor and if the target is a TodoItemPanel
            return support.isDataFlavorSupported(DataFlavor.stringFlavor) &&
                    support.getComponent() instanceof TodoItemPanel;
        }

        @Override
        public boolean importData(TransferSupport support) {
            if (!canImport(support)) {
                return false;
            }

            Transferable transferable = support.getTransferable();
            String draggedItemId;
            try {
                draggedItemId = (String) transferable.getTransferData(DataFlavor.stringFlavor);
            } catch (UnsupportedFlavorException | IOException e) {
                // Log error or handle
                e.printStackTrace();
                return false;
            }

            Component targetComponent = support.getComponent();
            if (!(targetComponent instanceof TodoItemPanel)) {
                return false;
            }

            TodoItemPanel targetPanel = (TodoItemPanel) targetComponent;
            String targetItemId = targetPanel.item.getId();

            // Prevent dropping onto itself
            if (draggedItemId.equals(targetItemId)) {
                return false;
            }

            todosRef.update(list -> {
                List<TodoItem> newList = new ArrayList<>(list);

                TodoItem draggedItem = null;
                int draggedItemOriginalIndex = -1;
                for (int i = 0; i < newList.size(); i++) {
                    if (newList.get(i).getId().equals(draggedItemId)) {
                        draggedItem = newList.get(i);
                        draggedItemOriginalIndex = i;
                        break;
                    }
                }

                if (draggedItem == null) {
                    return list; // Should not happen if IDs are consistent
                }

                // Remove the dragged item from its original position
                newList.remove(draggedItemOriginalIndex);

                // Find the new index for the dragged item (before the target item)
                int targetItemNewIndex = -1;
                for (int i = 0; i < newList.size(); i++) {
                    if (newList.get(i).getId().equals(targetItemId)) {
                        targetItemNewIndex = i;
                        break;
                    }
                }

                if (targetItemNewIndex != -1) {
                    // If the target item is found in the list (after removing the dragged one),
                    // insert the dragged item at the target's current position.
                    newList.add(targetItemNewIndex, draggedItem);
                } else {
                    // Fallback: if target item somehow not found (e.g., list changed unexpectedly)
                    // or if we want to support dropping at the end explicitly.
                    // For now, if targetItem was the draggedItem (already handled) or not found,
                    // this might indicate an issue or a need for more complex drop zone handling.
                    // A simple fallback is to add to the end, but ideally, this case is rare.
                    // Given the `draggedItemId.equals(targetItemId)` check, targetItem should be found.
                    // If it's not, it implies an inconsistent state. For safety, we can just add the dragged item to the end if this anomaly occurs.
                    newList.add(draggedItem);
                }
                return newList;
            });
            return true;
        }

    }

    private List<TodoItem> createInitialTodos() {
        List<TodoItem> initial = new ArrayList<>();
        initial.add(new TodoItem("Buy milk and bread", Set.of("shopping")));
        initial.add(new TodoItem("Write JSignals documentation #work", Set.of("work")));
        initial.add(new TodoItem("Call the dentist #personal", Set.of("personal")));
        initial.add(new TodoItem("Prepare for the meeting #work", Set.of("work")));
        initial.add(new TodoItem("Read a book on Java #personal", Set.of("personal", "learning")));
        return initial;
    }

    public static void main(String[] args) {
        var runtime = JSignals.initRuntime();
        SwingUtilities.invokeLater(() -> new SwingAdvancedTodoApp().setVisible(true));
    }

    /**
     * Represents the data model for a single Todo item.
     * Its properties are reactive to allow for fine-grained updates.
     */
    private static final class TodoItem {

        private String id;  // Changed from final to allow setting in the copy operation

        private final WritableRef<String> text;

        private final WritableRef<Boolean> completed;

        private final Set<String> labels;

        public TodoItem(String initialText, Set<String> labels) {
            this.id = UUID.randomUUID().toString();
            this.text = ref(initialText);
            this.completed = ref(false);
            this.labels = Set.copyOf(labels); // Immutable set
        }

        // Public getters for the reactive properties
        public String getId() { return id; }

        // Add setter for ID to support our item replacement strategy
        public void setId(String id) { this.id = id; }

        public WritableRef<String> getText() { return text; }

        public WritableRef<Boolean> getCompleted() { return completed; }

        public Set<String> getLabels() { return labels; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            return id.equals(((TodoItem) o).id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }

}
