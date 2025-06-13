package jsignals.examples;

import jsignals.JSignals;
import jsignals.core.Ref;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

public class SwingCustomTodoApp {

    static Ref<List<String>> todoListRef;

    static Todos todos;

    static JPanel todoContainer;

    static JScrollPane todoScrollPane;

    public static void main(String[] args) {
        todoListRef = JSignals.ref(new ArrayList<>(List.of(
                "Buy groceries",
                "Walk the dog",
                "Finish homework"
        )));

        todos = new Todos(todoListRef);

        // Create the Swing UI
        JFrame frame = new JFrame("Reactive Todo App");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(500, 400);
        frame.setLayout(new BorderLayout());

        // Todo list container
        todoContainer = new JPanel();
        todoContainer.setLayout(new BoxLayout(todoContainer, BoxLayout.Y_AXIS));
        todoScrollPane = new JScrollPane(todoContainer);
        frame.add(todoScrollPane, BorderLayout.CENTER);

        // Input field and add button
        JPanel inputPanel = new JPanel(new BorderLayout());
        JTextField todoInput = new JTextField();
        JButton addButton = new JButton("Add");
        inputPanel.add(todoInput, BorderLayout.CENTER);
        inputPanel.add(addButton, BorderLayout.EAST);
        frame.add(inputPanel, BorderLayout.SOUTH);

        // Reactivity: Update the todoContainer whenever the todoList changes
        JSignals.effect(() -> {
            todoContainer.removeAll();
            List<String> todos = todoListRef.get();
            for (int i = 0; i < todos.size(); i++) {
                todoContainer.add(createTodoPanel(i));
            }
            todoContainer.revalidate();
            todoContainer.repaint();
        });

        // Add button action: Add a new todo
        addButton.addActionListener(e -> {
            String newTodo = todoInput.getText().trim();
            if (!newTodo.isEmpty()) {
                todoInput.setText("");
                todos.add(newTodo);
            }
        });

        // Show the frame
        frame.setVisible(true);
    }

    private static JPanel createTodoPanel(int index) {
        var todoList = todoListRef.getValue();
        var currentTodo = todoList.get(index);

        JPanel todoPanel = new JPanel(new BorderLayout());
        todoPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        JLabel todoLabel = new JLabel(currentTodo);
        JButton editButton = new JButton("Edit");
        JButton deleteButton = new JButton("Delete");

        // Inline editing
        editButton.addActionListener(e -> {
            JTextField editField = new JTextField(currentTodo);
            JButton saveButton = new JButton("Save");
            todoPanel.removeAll();
            todoPanel.add(editField, BorderLayout.CENTER);
            todoPanel.add(saveButton, BorderLayout.EAST);
            todoPanel.revalidate();
            todoPanel.repaint();

            saveButton.addActionListener(saveEvent -> {
                String updatedTodo = editField.getText().trim();
                if (!updatedTodo.isEmpty()) {
                    todos.edit(index, updatedTodo);
                }
                todoPanel.removeAll();
                todoPanel.add(todoLabel, BorderLayout.CENTER);
                JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
                buttonPanel.add(editButton);
                buttonPanel.add(deleteButton);
                todoPanel.add(buttonPanel, BorderLayout.EAST);
                todoPanel.revalidate();
                todoPanel.repaint();
            });
        });

        // Delete action
        deleteButton.addActionListener(e -> {
            todos.remove(index);
        });

        // Add components to the todo panel
        todoPanel.add(todoLabel, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        todoPanel.add(buttonPanel, BorderLayout.EAST);

        return todoPanel;
    }

    static class Todos {

        private final Ref<List<String>> listRef;

        public Todos(Ref<List<String>> todoListRef) {
            this.listRef = todoListRef;
        }

        public Ref<List<String>> getRef() {
            return listRef;
        }

        public List<String> getList() {
            return listRef.getValue();
        }

        public int size() {
            return listRef.getValue().size();
        }

        public String get(int index) {
            return listRef.get().get(index);
        }

        public void add(String todo) {
            listRef.update(list -> new ArrayList<>(list) {{
                add(todo);
            }});
        }

        public void remove(int index) {
            listRef.update(list -> {
                List<String> newList = new ArrayList<>(list);
                newList.remove(index);
                return newList;
            });
        }

        public void edit(int index, String newTodo) {
            listRef.update(list -> {
                List<String> newList = new ArrayList<>(list);
                newList.set(index, newTodo);
                return newList;
            });
        }

    }

}
