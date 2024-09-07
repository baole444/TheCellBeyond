package utility;

import eventviewer.EventSystem;
import eventviewer.event.Event;
import eventviewer.event.EventType;
import org.lwjgl.glfw.GLFWWindowCloseCallback;

import javax.swing.*;

import java.awt.*;
import java.io.*;

import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;

public class ExitConfirmDialog {
    private boolean dialogPref;

    public ExitConfirmDialog() {
        this.dialogPref = loadDialogPref();

    }

    public void reloadDialog() {
        this.dialogPref = loadDialogPref();
    }

    public String getDialogPref() {
        return Boolean.toString(loadDialogPref());
    }

    public void setDialogPref(boolean b) {
        saveDialogPref(b);
    }

    private boolean loadDialogPref() {
        File config = new File("./Pref/closeConfirm.config");
        if (config.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(config))) {
                return Boolean.parseBoolean(reader.readLine());
            } catch (IOException e) {
                System.out.println("Failed to read from config file");
            }
        }
        return true; // Always show if pref not set.
    }

    private void saveDialogPref(boolean show) {
        File config = new File("./Pref/closeConfirm.config");
        config.getParentFile().mkdirs(); // Check directory existence
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(config))) {
            writer.write(Boolean.toString(show));
        } catch (IOException e) {
            System.out.println("Failed to save to config file");
        }
    }

    public boolean exitDialog() {
        if (!dialogPref) {
            return true;
        }
        // Set JFrame to force always on top for confirm dialog
        JFrame frame = new JFrame();
        frame.setAlwaysOnTop(true);
        frame.setUndecorated(true);
        frame.setVisible(true);
        frame.setLocationRelativeTo(null);

        JCheckBox hide = new JCheckBox("Do not show this again (Exit without save.)");

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(new JLabel("All current progress before save will be lost."), BorderLayout.CENTER);
        panel.add(hide, BorderLayout.SOUTH);

        Object[] options = {"Save & Exit", "Exit", "Cancel"};

        int confirm = JOptionPane.showOptionDialog(frame,
                panel, "Close TCB Editor?",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.QUESTION_MESSAGE,
                null,
                options,
                options[2]);

        frame.dispose();

        if (hide.isSelected()) {
            saveDialogPref(false);
        }
        if (confirm == 0) {
            return true;
        } else if (confirm == 1) {
            return true;
        } else {
            return false;
        }
    }
}
