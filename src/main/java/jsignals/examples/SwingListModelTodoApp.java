package jsignals.examples;

import jsignals.JSignals;
import jsignals.core.Disposable;
import jsignals.core.ReadableRef;
import jsignals.core.TrackableRef;
import jsignals.core.WritableRef;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.util.ArrayList;
import java.util.List;

import static jsignals.JSignals.*;

public class SwingListModelTodoApp {

    private static WritableRef<List<String>> todoList;

    private static ReadableRef<String> refreshedAt;

    private static TrackableRef updateTrigger;

    private static Disposable todoListUIEffect;

    private static Disposable refreshedLabelUIEffect;

    public static void main(String[] args) {
        var runtime = JSignals.initRuntime();

        todoList = ref(new ArrayList<>(List.of(
                "Buy groceries",
                "Walk the dog",
                "Finish homework"
        )));

        // Trigger to update the UI
        updateTrigger = trigger();

        // Refreshed time
        refreshedAt = computed(() -> {
            updateTrigger.track();
            return "Last refreshed at: " + java.time.LocalTime.now();
        });

        // Create the Swing UI
        JFrame frame = new JFrame("Reactive Todo App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                todoListUIEffect.dispose();
                refreshedLabelUIEffect.dispose();
                JSignals.shutdownRuntime();
                super.windowClosing(e);
            }
        });

        // Todo list display
        DefaultListModel<String> listModel = new DefaultListModel<>();
        JList<String> todoJList = new JList<>(listModel);
        frame.add(new JScrollPane(todoJList), BorderLayout.CENTER);

        // Input field and buttons to add, edit, and delete todos
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField todoInput = new JTextField();
        JButton addButton = new JButton("Add");
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");
        JLabel refreshedLabel = new JLabel();

        JPanel buttonPanel = new JPanel(new FlowLayout());
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);

        inputPanel.add(todoInput, BorderLayout.CENTER);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        inputPanel.add(refreshedLabel, BorderLayout.SOUTH);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Reactivity: Update the JList whenever the todoList changes
        todoListUIEffect = effect(() -> {
            listModel.clear();
            todoList.get().forEach(listModel::addElement);
            updateTrigger.trigger();
        });

        // Reactivity: Update the refreshed label whenever the time changes
        refreshedLabelUIEffect = effect(() -> refreshedLabel.setText(refreshedAt.get()));

        // Add button action: Add a new todo
        addButton.addActionListener(e -> {
            String newTodo = todoInput.getText().trim();
            if (!newTodo.isEmpty()) {
                todoList.set(new ArrayList<>(todoList.get()) {{
                    add(newTodo);
                }});
                todoInput.setText("");
            }
        });

        // Edit button action: Edit the selected todo
        editButton.addActionListener(e -> {
            int selectedIndex = todoJList.getSelectedIndex();
            if (selectedIndex != -1) {
                String updatedTodo = JOptionPane.showInputDialog(
                        frame,
                        "Edit Todo:",
                        todoList.get().get(selectedIndex)
                );
                if (updatedTodo != null && !updatedTodo.trim().isEmpty()) {
                    todoList.set(new ArrayList<>(todoList.get()) {{
                        set(selectedIndex, updatedTodo.trim());
                    }});
                }
            }
        });

        // Delete button action: Delete the selected todo
        deleteButton.addActionListener(e -> {
            int selectedIndex = todoJList.getSelectedIndex();
            if (selectedIndex != -1) {
                todoList.set(new ArrayList<>(todoList.get()) {{
                    remove(selectedIndex);
                }});
            }
        });

        // Show the frame
        frame.setVisible(true);
    }

}
