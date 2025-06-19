package jsignals.swing;

import jsignals.core.ReadableRef;
import jsignals.core.WritableRef;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.util.function.Consumer;

public class SwingTools {

    private SwingTools() { }

    /**
     * Binds the text of a JLabel to a readable reference.
     * The label's text will automatically update when the ref's value changes.
     */
    public static void bindText(JLabel label, ReadableRef<String> textRef) {
        bind(label, textRef, label::setText);
    }

    /**
     * Binds the enabled state of a JComponent to a readable boolean reference.
     */
    public static void bindEnabled(JComponent component, ReadableRef<Boolean> enabledRef) {
        bind(component, enabledRef, component::setEnabled);
    }

    /**
     * Binds the visibility of a JComponent to a readable boolean reference.
     */
    public static void bindVisible(JComponent component, ReadableRef<Boolean> visibleRef) {
        bind(component, visibleRef, component::setVisible);
    }

    /**
     * A generic method to bind any property of a Swing component to a ref.
     * Ensures that the UI update happens on the Event Dispatch Thread (EDT).
     *
     * @param component The Swing component (used for initial value setting).
     * @param ref       The reactive reference to subscribe to.
     * @param updater   The action to perform on the component (e.g., label::setText).
     */
    public static <C extends JComponent, T> void bind(C component, ReadableRef<T> ref, Consumer<T> updater) {
        // Subscribe to the ref. The callback will update the component.
        ref.watch(newValue -> {
            // All Swing UI updates MUST happen on the Event Dispatch Thread.
            if (SwingUtilities.isEventDispatchThread()) {
                updater.accept(newValue);
            } else {
                SwingUtilities.invokeLater(() -> updater.accept(newValue));
            }
        });

        // Set the initial value of the component from the ref.
        SwingUtilities.invokeLater(() -> updater.accept(ref.getValue()));
    }

    /**
     * Creates a two-way binding between a JTextField and a writable string reference.
     * Changes in the ref update the text field, and user typing updates the ref.
     */
    public static void bindTextField(JTextField textField, WritableRef<String> textRef) {
        // 1. Model -> View: Update the text field when the ref changes.
        bind(textField, textRef, (text) -> {
            // Only update if the text is actually different, to avoid feedback loops.
            if (!textField.getText().equals(text)) {
                textField.setText(text);
            }
        });

        // 2. View -> Model: Update the ref when the user types in the text field.
        textField.getDocument().addDocumentListener(new DocumentListener() {
            private void updateRef() {
                // Ensure this update also happens on the EDT, though DocumentListener usually does.
                SwingUtilities.invokeLater(() -> textRef.set(textField.getText()));
            }

            @Override
            public void insertUpdate(DocumentEvent e) { updateRef(); }

            @Override
            public void removeUpdate(DocumentEvent e) { updateRef(); }

            @Override
            public void changedUpdate(DocumentEvent e) { updateRef(); }
        });
    }

}
