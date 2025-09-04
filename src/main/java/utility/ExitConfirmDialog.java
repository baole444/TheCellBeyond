package utility;

import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import eventviewer.event.EventType;

import javax.swing.*;
import java.awt.*;

public class ExitConfirmDialog {
    public static boolean exitDialog() {
        if (isAutoSaveOnExitOn()) {
            return true;
        }

        // Set JFrame to force always on top for confirm dialog
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        JCheckBox autoSave = new JCheckBox("Enable auto save on exit");

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("All current progress before save will be lost."), BorderLayout.CENTER);
        panel.add(autoSave, BorderLayout.SOUTH);

        Object[] options = {"Save & Exit", "Exit", "Cancel"};

        int confirm = JOptionPane.showOptionDialog(frame,
                panel, "Close TCB Editor?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);

        frame.dispose();

        if (autoSave.isSelected()) {
            setAutoSaveOn();
        }
        if (confirm == 0) {
            EngineEventCallback.emit(null, new Event(EventType.LEVEL_SAVE));
            return true;
        } else return confirm == 1;
    }

    private static boolean isAutoSaveOnExitOn() {
        return UserPreference.editorPreferences().autoSaveOnExit();
    }

    private static void setAutoSaveOn() {
        EditorPreferences current = UserPreference.reloadEditorPreferences();

        if (current.autoSaveOnExit()) return;

        EditorPreferences update = new EditorPreferences(true, current.autoSaveOnChangeScene());

        UserPreference.updateEditorPreferences(update);
    }
}
