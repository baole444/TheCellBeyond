package editor;

import imgui.ImGui;
import imgui.ImVec4;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import utility.RingBuffer;
import utility.log.EngineLog;
import utility.log.EngineLogCallback;
import utility.log.EngineLogListener;
import utility.log.LogEntry;

import java.util.EnumMap;
import java.util.List;

/**
 * Panel for displaying engine's log in the Editor UI.
 */
public final class ConsoleOutput implements EngineLogListener {
    private static ConsoleOutput instance;

    private static final ImVec4 debugColor = new ImVec4(0.35f, 0.35f, 0.35f, 1.0f);
    private static final ImVec4 infoColor = new ImVec4(0.85f, 0.85f, 0.85f, 1.0f);
    private static final ImVec4 warningColor = new ImVec4(0.85f, 0.85f, 0.25f, 1.0f);
    private static final ImVec4 errorColor = new ImVec4(0.85f, 0.25f, 0.25f, 1.0f);
    private static final float iconSize = 28.0f;
    private static final String debugId = "Debug##enable_debug_log_history";
    private static final String infoId = "Info##enable_info_log_history";
    private static final String warningId = "Warning##enable_warning_log_history";
    private static final String errorId = "Error##enable_error_log_history";

    private final ImBoolean enableDebug = new ImBoolean(false);
    private final ImBoolean enableInfo = new ImBoolean(true);
    private final ImBoolean enableWarning = new ImBoolean(true);
    private final ImBoolean enableError = new ImBoolean(true);
    private final EnumMap<EngineLog.Level, ImBoolean> logFilter;
    private final RingBuffer<LogEntry> entries;

    static {
        init();
    }

    /**
     * Create the module and register it with {@link EngineLogCallback}.
     */
    private ConsoleOutput() {
        entries = new RingBuffer<>(1024);
        logFilter = new EnumMap<>(EngineLog.Level.class);
        logFilter.put(EngineLog.Level.Debug, enableDebug);
        logFilter.put(EngineLog.Level.Info, enableInfo);
        logFilter.put(EngineLog.Level.Warning, enableWarning);
        logFilter.put(EngineLog.Level.Error, enableError);
        List<LogEntry> backlogs = EngineLog.logs();
        for (LogEntry entry : backlogs) {
            entries.add(entry);
        }
        EngineLogCallback.register(this);
    }

    private static void init() {
        if (instance != null) {
            EngineLogCallback.unregister(instance);
        }
        instance = new ConsoleOutput();
    }

    static void imgui() {
        if (instance == null) return;
        instance.render();
    }

    private void render() {
        if (!ImGui.beginTable("##Engine_log_panel", 2, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvail())) {
            ImGui.textDisabled("Failed to load console log table");
            return;
        }
        ImGui.tableSetupColumn("##Log_History_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##Log_Filter_Buttons_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        drawLogsRegion();
        ImGui.tableNextColumn();
        drawLogFilter();
        ImGui.endTable();
    }

    private void drawLogsRegion() {
        if (!ImGui.beginChild("##Scrolling_logs")) {
            ImGui.textDisabled("Cannot initialize region to display logs.");
            ImGui.endChild();
            return;
        }
        List<LogEntry> logs = entries.toList();
        for (LogEntry entry : logs) {
            printLog(entry);
        }
        if (ImGui.getScrollY() >= ImGui.getScrollMaxY() - 1.0f) ImGui.setScrollHereY(1.0f);
        ImGui.endChild();
    }

    private void drawLogFilter() {
        EditorWidget.selectableIcon(debugId, EditorIcons.LogLevelIcons.Debug, "Show/hide debug log level", enableDebug, iconSize, iconSize);
        EditorWidget.selectableIcon(infoId, EditorIcons.LogLevelIcons.Info, "Show/hide info log level", enableInfo, iconSize, iconSize);
        EditorWidget.selectableIcon(warningId, EditorIcons.LogLevelIcons.Warning, "Show/hide warning log level", enableWarning, iconSize, iconSize);
        EditorWidget.selectableIcon(errorId, EditorIcons.LogLevelIcons.Error, "Show/hide error log level", enableError, iconSize, iconSize);
        ImGui.separator();
        if (EditorWidget.iconButton("Clear##Clear_log_history", EditorIcons.Icons.Delete, "Click to clear log history", iconSize, iconSize)) {
            EngineLog.clear();
            entries.clear();
        }
    }

    private static String formatEntry(LogEntry entry) {
        if (entry == null) return "NULL LOG ENTRY";
        return "[" + entry.formatedTimeStamp() + "]["
                + entry.source() + "]["
                + entry.level().prefix + "]: "
                + entry.message();
    }

    private static void printLog(LogEntry entry) {
        if (instance == null || !instance.isLogLevelEnable(entry)) return;
        String log = formatEntry(entry);
        ImVec4 color = switch (entry.level()) {
            case Debug -> debugColor;
            case Info -> infoColor;
            case Warning -> warningColor;
            case Error -> errorColor;
        };
        ImGui.pushStyleColor(ImGuiCol.Text, color);
        ImGui.textWrapped(log);
        ImGui.popStyleColor(1);
    }

    private boolean isLogLevelEnable(LogEntry entry) {
        if (entry == null) return false;
        EngineLog.Level level = entry.level();
        return logFilter.get(level).get();
    }

    @Override
    public final void onNewLog(LogEntry entry) {
        if (entry == null) return;
        entries.add(entry);
    }
}
