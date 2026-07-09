package editor;

import editor.widgets.SelectableTextView;
import editor.widgets.SelectableTextView.Row;
import imgui.ImGui;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImBoolean;
import utility.RingBuffer;
import utility.log.*;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;

/**
 * Panel for displaying engine's log in the Editor UI.
 */
public final class ConsoleOutput implements EngineLogListener {
    private static ConsoleOutput instance;

    private static final float IconSize = 28.0f;
    private static final String DebugId = "Debug##enable_debug_log_history";
    private static final String InfoId = "Info##enable_info_log_history";
    private static final String WarningId = "Warning##enable_warning_log_history";
    private static final String ErrorId = "Error##enable_error_log_history";

    private final ImBoolean enableDebug = new ImBoolean(false);
    private final ImBoolean enableInfo = new ImBoolean(true);
    private final ImBoolean enableWarning = new ImBoolean(true);
    private final ImBoolean enableError = new ImBoolean(true);
    private final EnumMap<Level, ImBoolean> logFilter;
    private final RingBuffer<LogEntry> entries;

    private final SelectableTextView logView = new SelectableTextView();
    private final IdentityHashMap<LogEntry, Row> rowByEntry = new IdentityHashMap<>();
    private final List<Row> rowCache = new ArrayList<>();
    private volatile boolean dirty = true;
    private int lastFilterMask = -1;

    static {
        init();
    }

    /**
     * Create the module and register it with {@link EngineLogCallback}.
     */
    private ConsoleOutput() {
        entries = new RingBuffer<>(1024);
        logFilter = new EnumMap<>(Level.class);
        logFilter.put(Level.Debug, enableDebug);
        logFilter.put(Level.Info, enableInfo);
        logFilter.put(Level.Warning, enableWarning);
        logFilter.put(Level.Error, enableError);
        logView.stickToBottom(true);
        List<LogEntry> backlogs = EngineLog.logs();
        for (LogEntry entry : backlogs) entries.add(entry);
        EngineLogCallback.register(this);
    }

    private static void init() {
        if (instance != null) EngineLogCallback.unregister(instance);
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
        refreshFilterDirty();
        if (dirty) rebuildRows();
        logView.render("##Scrolling_Logs", rowCache, this::logContextMenu);
    }

    private void drawLogFilter() {
        EditorWidget.selectableIcon(DebugId, EditorIcons.LogLevelIcons.Debug, "Show/hide debug log level", enableDebug, IconSize, IconSize);
        EditorWidget.selectableIcon(InfoId, EditorIcons.LogLevelIcons.Info, "Show/hide info log level", enableInfo, IconSize, IconSize);
        EditorWidget.selectableIcon(WarningId, EditorIcons.LogLevelIcons.Warning, "Show/hide warning log level", enableWarning, IconSize, IconSize);
        EditorWidget.selectableIcon(ErrorId, EditorIcons.LogLevelIcons.Error, "Show/hide error log level", enableError, IconSize, IconSize);
        ImGui.separator();
        if (!EditorWidget.iconButton("Clear##Clear_log_history", EditorIcons.Icons.Delete, "Click to clear log history", IconSize, IconSize)) return;
        EngineLog.clear();
        entries.clear();
        dirty = true;
    }

    private void logContextMenu(Row row) {
        if (ImGui.menuItem("Copy all")) ImGui.setClipboardText(allText());
    }

    private void refreshFilterDirty() {
        int mask = (enableDebug.get() ? 1 : 0) | (enableInfo.get() ? 2 : 0) | (enableWarning.get() ? 4 : 0) | (enableError.get() ? 8 : 0);
        if (mask == lastFilterMask) return;
        lastFilterMask = mask;
        dirty = true;
    }

    private void rebuildRows() {
        List<LogEntry> all = entries.toList();
        IdentityHashMap<LogEntry, Boolean> present = new IdentityHashMap<>();
        for (LogEntry entry : all) present.put(entry, Boolean.TRUE);
        rowByEntry.keySet().retainAll(present.keySet());
        rowCache.clear();
        for (LogEntry entry : all) {
            if (!isLogLevelEnable(entry)) continue;
            rowCache.add(rowByEntry.computeIfAbsent(entry, e -> new Row(formatEntry(e), colorFor(e.level()))));
        }
        dirty = false;
    }

    private String allText() {
        StringBuilder sb = new StringBuilder();
        for (Row row : rowCache) {
            sb.append(row.text);
            sb.append('\n');
        }
        return sb.toString();
    }

    private static int colorFor(Level level) {
        return switch (level) {
            case Debug -> EditorColors.DebugLogColor;
            case Info -> EditorColors.InfoLogColor;
            case Warning -> EditorColors.WarningLogColor;
            case Error -> EditorColors.ErrorLogColor;
        };
    }

    private static String formatEntry(LogEntry entry) {
        if (entry == null) return "NULL LOG ENTRY";
        return "[" + entry.formatedTimeStamp() + "]["
                + entry.source() + "]["
                + entry.level().prefix + "]: "
                + entry.message();
    }

    private boolean isLogLevelEnable(LogEntry entry) {
        if (entry == null) return false;
        Level level = entry.level();
        return logFilter.get(level).get();
    }

    @Override
    public void onNewLog(LogEntry entry) {
        if (entry == null) return;
        entries.add(entry);
        dirty = true;
    }
}
