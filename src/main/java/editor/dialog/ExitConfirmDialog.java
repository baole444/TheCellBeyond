package editor.dialog;

import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;

import javax.swing.*;
import java.awt.*;

/**
 * Editor dialogue for prompting save editing scene on exit.
 */
public final class ExitConfirmDialog {
    private ExitConfirmDialog() {}

    /**
     * Show the exit dialogue. If auto save on exit is enabled, this will skip showing the dialogue entirely.
     * @return true if the user choose exit (regardless of saving or not), false if cancelled
     */
    public static boolean exitDialog() {
        if (isAutoSaveOnExitOn()) {
            if (LogicServer.currentSceneName() != null) return true;
            SaveSceneAsDialog.show(() -> {
                EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
                Window.get().forceClose();
            });
            return false;
        }
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
        if (autoSave.isSelected()) setAutoSaveOn();
        if (confirm == 0) {
            if (LogicServer.currentSceneName() != null) {
                EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
                return true;
            }
            SaveSceneAsDialog.show(() -> {
                EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
                Window.get().forceClose();
            });
            return false;
        }
        return confirm == 1;
    }

    private static boolean isAutoSaveOnExitOn() {
        return UserPreference.editorPreferences().autoSaveOnExit();
    }

    private static void setAutoSaveOn() {
        EditorPreferences current = UserPreference.reloadEditorPreferences();
        if (current.autoSaveOnExit()) return;
        EditorPreferences update = new EditorPreferences(true, current.autoSaveOnChangeScene(), current.showGridLine());
        UserPreference.updateEditorPreferences(update);
    }
}
