package editor.dialogs;

import TheCellBeyond.Window;
import TheCellBeyond.internal.LogicServer;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import eventviewer.EngineEventCallback;
import eventviewer.event.EditorEvent;
import utility.AssetReference;
import utility.Settings;
import utility.UnifiedPaths;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.io.InputStream;
import java.util.Objects;

/**
 * Editor dialogue for prompting save editing scene on exit.
 */
public final class ExitConfirmDialog {
    private static final Color DarkBackground = new Color(30, 30, 30);
    private static final Color DarkText = new Color(210, 210, 210);
    private static final Color DarkTextSecondary = new Color(150, 150, 150);
    private static final Color AccentRed = new Color(200, 90, 90);
    private static final Color AccentBlue = new Color(70, 130, 200);
    private static final Color ButtonBackground = new Color(60, 60, 60);
    private static final Color ButtonHover = new Color(75, 75, 75);

    private static final Color LightBackground = new Color(243, 243, 243);
    private static final Color LightText = new Color(30, 30, 30);
    private static final Color LightTextSecondary = new Color(100, 100, 100);
    private static final Color LightButtonBackground = new Color(225, 225, 225);
    private static final Color LightButtonHover = new Color(210, 210, 210);

    private static final int ChoiceSaveExit = 0;
    private static final int ChoiceExit = 1;
    private static final int ChoiceCancel = 2;

    private static Image appIcon;
    private static volatile JDialog activeDialog;

    private record Result(int choice, boolean autoSave) {}

    private ExitConfirmDialog() {}

    /**
     * Show the exit dialogue. If auto save on exit is enabled, this will skip showing the dialogue entirely.
     * @return true if the user choose exit (regardless of saving or not), false if cancelled
     */
    public static boolean exitDialog() {
        if (activeDialog != null) {
            activeDialog.toFront();
            activeDialog.requestFocus();
            return false;
        }
        if (isAutoSaveOnExitOn()) {
            if (LogicServer.currentSceneName() != null) return true;
            SaveSceneAsDialog.show(() -> {
                EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
                Window.get().forceClose();
            });
            return false;
        }
        Result result = showDialog();
        if (result.autoSave) enableAutosave();
        if (result.choice != ChoiceSaveExit) return result.choice == ChoiceExit;
        if (LogicServer.currentSceneName() != null) {
            EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
            return true;
        }
        SaveSceneAsDialog.show(() -> {
            EngineEventCallback.emit(new EditorEvent(EditorEvent.Type.SaveEditingSceneToDisk));
            Objects.requireNonNull(Window.get()).forceClose();
        });
        return false;
    }

    private static boolean isAutoSaveOnExitOn() {
        return UserPreference.preferences().autoSaveOnExit();
    }

    private static void enableAutosave() {
        EditorPreferences current = UserPreference.reloadPreferences();
        if (current.autoSaveOnExit()) return;
        UserPreference.updatePreferences(current.autoSaveOnExit(true));
    }

    private static Result showDialog() {
        boolean darkMode = isOSDarkMode();
        Color bg = darkMode ? DarkBackground : LightBackground;
        Color text = darkMode ? DarkText : LightText;
        Color textSecondary = darkMode ? DarkTextSecondary : LightTextSecondary;
        Color btnBg = darkMode ? ButtonBackground : LightButtonBackground;
        Color btnHover = darkMode ? ButtonHover : LightButtonHover;
        JDialog dialog = new JDialog(null, "Close TCB Editor?", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        dialog.setAlwaysOnTop(true);
        dialog.setResizable(false);
        Image icon = appIcon();
        if (icon != null) dialog.setIconImage(icon);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        root.add(getHeader(textSecondary), BorderLayout.NORTH);
        JCheckBox autoSave = new JCheckBox("Enable auto save on exit");
        autoSave.setOpaque(false);
        autoSave.setForeground(text);
        autoSave.setFocusPainted(false);
        autoSave.setBorder(new EmptyBorder(16, 0, 16, 0));
        root.add(autoSave, BorderLayout.CENTER);
        int[] resolution = {ChoiceCancel};
        JButton saveExit = createFlatButton("Save & Exit", AccentBlue, AccentBlue.darker(), Color.WHITE);
        JButton exit = createFlatButton("Exit", AccentRed, AccentRed.darker(), Color.WHITE);
        JButton cancel = createFlatButton("Cancel", btnBg, btnHover, text);
        saveExit.addActionListener(_ -> {
            resolution[0] = ChoiceSaveExit;
            dialog.dispose();
        });
        exit.addActionListener(_ -> {
            resolution[0] = ChoiceExit;
            dialog.dispose();
        });
        cancel.addActionListener(_ -> {
            resolution[0] = ChoiceCancel;
            dialog.dispose();
        });
        JPanel buttonGroup = new JPanel(new GridLayout(1, 3, 10, 0));
        buttonGroup.setOpaque(false);
        buttonGroup.add(saveExit);
        buttonGroup.add(exit);
        buttonGroup.add(cancel);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        buttons.setOpaque(false);
        buttons.add(buttonGroup);
        root.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(root);
        dialog.pack();
        dialog.setLocationRelativeTo(null);
        dialog.getRootPane().setDefaultButton(cancel);
        activeDialog = dialog;
        try {
            dialog.setVisible(true);
        } finally {
            activeDialog = null;
        }
        return new Result(resolution[0], autoSave.isSelected());
    }

    private static JPanel getHeader(Color textSecondary) {
        JPanel header = new JPanel(new BorderLayout());
        header.setOpaque(false);
        JLabel message = new JLabel("All current progress before save will be lost.");
        message.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        message.setForeground(textSecondary);
        header.add(message, BorderLayout.SOUTH);
        return header;
    }

    private static JButton createFlatButton(String label, Color bg, Color hover, Color fg) {
        JButton button = new JButton(label) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2D = (Graphics2D) g.create();
                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setColor(getBackground());
                g2D.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2D.dispose();
                super.paintComponent(g);
            }
        };
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setForeground(fg);
        button.setBackground(bg);
        button.setContentAreaFilled(false);
        button.setOpaque(false);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                button.setBackground(hover);
                button.repaint();
            }
            public void mouseExited(MouseEvent e) {
                button.setBackground(bg);
                button.repaint();
            }
        });
        return button;
    }

    private static Image appIcon() {
        if (appIcon != null) return appIcon;
        AssetReference assetRef = new AssetReference(Settings.TexturePath.TCBIcon);
        try (InputStream stream = UnifiedPaths.getAssetStream(assetRef.resolvedPath())) {
            appIcon = ImageIO.read(stream);
        } catch (Exception _) {}
        return appIcon;
    }

    private static boolean isOSDarkMode() {
        String os = System.getProperty("os.name", "").toLowerCase();
        if (os.contains("win")) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{
                        "reg", "query",
                        "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
                        "/v", "AppsUseLightTheme"
                });
                String output = new String(process.getInputStream().readAllBytes());
                return output.contains("0x0");
            } catch (Exception _) {
                return true;
            }
        }
        if (os.contains("mac")) {
            try {
                Process process = Runtime.getRuntime().exec(new String[]{
                        "defaults", "read", "-g", "AppleInterfaceStyle"
                });
                String output = new String(process.getInputStream().readAllBytes()).trim();
                return output.equalsIgnoreCase("Dark");
            } catch (Exception _) {}
        }
        return true;
    }
}
