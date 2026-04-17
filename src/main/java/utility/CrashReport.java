package utility;

import utility.log.EngineLog;
import utility.log.LogEntry;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * CrashReport is a popup used for handling and show crash report outside the engine runtime (e.g. editor crashes.)
 */
public final class CrashReport {
    private static final DateTimeFormatter FileTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");
    private static final DateTimeFormatter LogTimestamp = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private static final Color DarkBackground = new Color(30, 30, 30);
    private static final Color DarkSurface = new Color(45, 45, 45);
    private static final Color DarkText = new Color(210, 210, 210);
    private static final Color DarkTextSecondary = new Color(150, 150, 150);
    private static final Color AccentRed = new Color(200, 90, 90);
    private static final Color ButtonBackground = new Color(60, 60, 60);
    private static final Color ButtonHover = new Color(75, 75, 75);

    private static final Color LightBackground = new Color(243, 243, 243);
    private static final Color LightSurface = new Color(255, 255, 255);
    private static final Color LightText = new Color(30, 30, 30);
    private static final Color LightTextSecondary = new Color(100, 100, 100);
    private static final Color LightButtonBackground = new Color(225, 225, 225);
    private static final Color LightButtonHover = new Color(210, 210, 210);

    private CrashReport() {}

    /**
     * Setup default uncaught exception handler to show the crash dialogue.
     */
    public static void install() {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            String report = buildReport(thread, throwable);
            String filePath = writeCrashLog(report);
            showCrashDialog(throwable, filePath, report);
        });
    }

    private static String buildReport(Thread thread, Throwable throwable) {
        StringBuilder builder = new StringBuilder("--- TheCellBeyond Crash Report ---\n");
        builder.append("Time: ").append(LocalDateTime.now().format(LogTimestamp)).append("\n");
        builder.append("Thread: ").append(thread.getName()).append("\n\n");
        builder.append("Exception: ").append(throwable.getClass().getName()).append("\n");
        builder.append("Message: ").append(throwable.getMessage()).append("\n\n");
        builder.append("Stack trace:\n");
        StringWriter writer = new StringWriter();
        throwable.printStackTrace(new PrintWriter(writer));
        builder.append(writer).append("\n");
        builder.append("--- Engine Log (last entries) ---\n");
        try {
            List<LogEntry> logs = EngineLog.logs();
            int start = Math.max(0, logs.size() - 50);
            for (int i = start; i < logs.size(); i++) {
                LogEntry entry = logs.get(i);
                builder.append(String.format("[%s] [%s] %s: %s", entry.formatedTimeStamp(), entry.level().prefix, entry.source(), entry.message()));
                builder.append("\n");
            }
        } catch (Exception e) {
            builder.append(String.format("Failed to get engine logs: %s", e.getMessage())).append("\n");
        }
        builder.append("\n--- System Info ---\n");
        builder.append("OS: ").append(System.getProperty("os.name"))
                .append(" ").append(System.getProperty("os.version"))
                .append(" ").append(System.getProperty("os.arch")).append("\n");
        builder.append("Java: ").append(System.getProperty("java.version"))
                .append(" ").append(System.getProperty("java.vendor")).append("\n");
        Runtime runtime = Runtime.getRuntime();
        long mb = 1024 * 1024;
        builder.append(String.format("Memory: %dMB / %dMB", runtime.totalMemory() / mb, runtime.maxMemory() / mb)).append("\n");
        return builder.toString();
    }

    private static String writeCrashLog(String report) {
        try {
            Path appDir = Paths.get("").toAbsolutePath();
            String fileName = "crash-" + LocalDateTime.now().format(FileTimestamp) + ".log";
            Path crashFile = appDir.resolve(fileName);
            Files.writeString(crashFile, report);
            return crashFile.toString();
        } catch (Exception e) {
            System.err.println("Failed to write crash log: " + e.getMessage());
            return null;
        }
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

    private static void showCrashDialog(Throwable throwable, String filePath, String fullReport) {
        boolean dark = isOSDarkMode();
        Color bg = dark ? DarkBackground : LightBackground;
        Color surface = dark ? DarkSurface : LightSurface;
        Color text = dark ? DarkText : LightText;
        Color textSecondary = dark ? DarkTextSecondary : LightTextSecondary;
        Color accent = AccentRed;
        Color btnBg = dark ? ButtonBackground : LightButtonBackground;
        Color btnHover = dark ? ButtonHover : LightButtonHover;
        JFrame frame = new JFrame("TheCellBeyond - Crash Report");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(520, 400);
        frame.setResizable(false);
        frame.setAlwaysOnTop(true);
        frame.setLocationRelativeTo(null);
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(bg);
        root.setBorder(new EmptyBorder(20, 24, 20, 24));
        JPanel header = getHeader(throwable, accent, textSecondary);
        root.add(header, BorderLayout.NORTH);
        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.setBorder(new EmptyBorder(16, 0, 16, 0));
        JTextArea detail = new JTextArea(fullReport);
        detail.setEditable(false);
        detail.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 11));
        detail.setBackground(surface);
        detail.setForeground(text);
        detail.setCaretColor(text);
        detail.setBorder(new EmptyBorder(10, 10, 10, 10));
        detail.setLineWrap(true);
        detail.setWrapStyleWord(true);
        JScrollPane scroll = new JScrollPane(detail);
        scroll.setBorder(BorderFactory.createLineBorder(dark ? new Color(60, 60, 60) : new Color(200, 200, 200)));
        scroll.getVerticalScrollBar().setUnitIncrement(12);
        center.add(scroll, BorderLayout.CENTER);
        if (filePath != null) {
            JLabel pathLabel = new JLabel("Saved to: " + filePath);
            pathLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 11));
            pathLabel.setForeground(textSecondary);
            center.add(pathLabel, BorderLayout.SOUTH);
        }
        root.add(center, BorderLayout.CENTER);
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        buttons.setOpaque(false);
        JButton copyBtn = createFlatButton("Copy to Clipboard", btnBg, btnHover, text);
        copyBtn.addActionListener(_ -> {
            StringSelection selection = new StringSelection(fullReport);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
            copyBtn.setText("Copied");
        });
        JButton closeBtn = createFlatButton("Close", accent, accent.darker(), Color.WHITE);
        closeBtn.addActionListener(_ -> System.exit(1));
        buttons.add(copyBtn);
        buttons.add(closeBtn);
        root.add(buttons, BorderLayout.SOUTH);
        frame.setContentPane(root);
        frame.setVisible(true);
    }

    private static JPanel getHeader(Throwable throwable, Color accent, Color textSecondary) {
        JPanel header = new JPanel(new BorderLayout(0, 6));
        header.setOpaque(false);
        JLabel title = new JLabel("Ops! Something when wrong...");
        title.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 16));
        title.setForeground(accent);
        header.add(title, BorderLayout.NORTH);
        JLabel subtitle = new JLabel(throwable.getClass().getSimpleName() + ": " + throwable.getMessage());
        subtitle.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        subtitle.setForeground(textSecondary);
        header.add(subtitle, BorderLayout.SOUTH);
        return header;
    }

    private static JButton createFlatButton(String label, Color bg, Color hover, Color fg) {
        JButton button = new JButton(label);
        button.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        button.setForeground(fg);
        button.setBackground(bg);
        button.setFocusPainted(false);
        button.setBorderPainted(false);
        button.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        button.setBorder(new EmptyBorder(8, 18, 8, 18));
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { button.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e) { button.setBackground(bg); }
        });
        return button;
    }
}
