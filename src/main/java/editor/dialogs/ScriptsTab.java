package editor.dialogs;

import editor.EditorColors;
import editor.EditorIcons;
import editor.EditorWidget;
import editor.preference.EditorPreferences;
import editor.preference.UserPreference;
import editor.widgets.SelectableTextView;
import editor.widgets.SelectableTextView.Row;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.*;
import imgui.type.ImString;
import project.Project;
import scripting.ScriptLoader;
import scripting.ScriptProjectGenerator;
import scripting.builder.BuildPhase;
import scripting.builder.BuildStatus;
import scripting.builder.ScriptBuilder;
import utility.log.EngineLog;

import java.util.ArrayList;
import java.util.List;

final class ScriptsTab {
    private enum HoveredControl {
        None,
        BuildScript,
        UpdateScriptProject,
        GenerateScriptProject,
    }

    private static final ImString newScanDirName = new ImString(256);
    private static final ImVec2 sizeCache = new ImVec2();

    private static final String BuildScriptLabel = "Build Script##ST_Build_Script_Control_Button";
    private static final String UpdateScriptProjectLabel = "Update Script Project##ST_Update_Script_Project_Control_Button";
    private static final String GenerateScriptProjectButton = "Generate Script Project##ST_Generate_Script_Project_Control_Button";
    private static final float Padding = 4.0f;
    private static final float ButtonWidth = Math.max(ImGui.calcTextSizeX("Update Script Project"), ImGui.calcTextSizeX("Generate Script Project")) + Padding + ImGui.getStyle().getFramePaddingX();
    private static final float ButtonHeight = 30.0f;
    private static final float ErrorReserveRegionHeight = ImGui.getTextLineHeightWithSpacing() * 5.0f;
    private static final float IconSize = 28.0f;

    private static float scanDirLayoutInnerHeight = IconSize;
    private static float mainScriptControlLayoutInnerHeight = ButtonHeight * 3.0f;
    private static HoveredControl hoveredControl = HoveredControl.None;

    private static String message = "";
    private static final SelectableTextView ErrorView = new SelectableTextView();
    private static final List<Row> ErrorRows = new ArrayList<>();
    private static BuildStatus lastStatus;
    private static boolean hasError = false;

    static void clear() {
        hoveredControl = HoveredControl.None;
        newScanDirName.clear();
        message = "";
        hasError = false;
    }

    static void imgui() {
        ImGui.spacing();
        ImGui.textWrapped("The engine scans these directories for .jar files for custom GameObject and Component classes.");
        ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.YellowHighLight);
        ImGui.textWrapped("You can organize the .jars into sub-directories for better organization.");
        ImGui.popStyleColor(1);
        ImGui.spacing();
        ImGui.separator();
        if (!ImGui.beginTable("##ST_New_ScanDir_Layout_Table", 5, ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvailX(), scanDirLayoutInnerHeight)) return;
        ImGui.tableSetupColumn("##ST_Build_Script_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##ST_Reload_Script_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##ST_New_ScanDir_Input_Layout_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("ST_Clear_New_ScanDir_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##ST_Add_New_ScanDir_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Build Script##ST_Build_Script_Icon_Button", EditorIcons.ScriptIcons.BuildScript, "Click to build scripts", IconSize, IconSize)) requestBuildScript();
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Reload Script##ST_Reload_Script_Icon_Button", EditorIcons.Icons.Reset, "Click to reload scripts", IconSize, IconSize)) ScriptLoader.reload();
        scanDirLayoutInnerHeight = ImGui.getItemRectSizeY();
        ImGui.tableNextColumn();
        float newCursorY = ImGui.getCursorPosY() + (scanDirLayoutInnerHeight - ImGui.getFrameHeight()) / 2.0f;
        ImGui.setCursorPosY(newCursorY);
        ImGui.setNextItemWidth(ImGui.getContentRegionAvailX());
        ImGui.inputTextWithHint("##ST_New_ScanDir_Input", "Add new Scan directory...", newScanDirName);
        ImGui.tableNextColumn();
        renderClearButton(newScanDirName::clear, scanDirLayoutInnerHeight);
        ImGui.tableNextColumn();
        boolean canAdd = canAddNewScanDir(newScanDirName.get());
        if (!canAdd) ImGui.beginDisabled();
        if (ImGui.button("Add Directory##ST_Add_New_ScanDir_Button", 120.0f, scanDirLayoutInnerHeight)) {
            String dirName = newScanDirName.get().trim();
            if (Project.addScriptScanDir(dirName)) newScanDirName.clear();
        }
        if (!canAdd) ImGui.endDisabled();
        ImGui.endTable();
        ImGui.separator();
        refreshStatus();
        float remainHeight = ImGui.getContentRegionAvailY();
        float buildStatusReserve = hasError ? ErrorReserveRegionHeight : ImGui.getTextLineHeightWithSpacing();
        if (ImGui.beginChild("##ST_ScanDir_List_Region", 0.0f, remainHeight - sizeCache.y - buildStatusReserve - Padding)) renderScanDirList();
        ImGui.endChild();
        ImGui.beginGroup();
        ImGui.separator();
        ImGui.beginDisabled();
        ImGui.textWrapped(String.format("Loaded %d object type(s) and %d component type(s)", ScriptLoader.gameObjectTypes().size(), ScriptLoader.componentTypes().size()));
        ImGui.endDisabled();
        ImGui.separator();
        renderScriptControl();
        ImGui.endGroup();
        ImGui.getItemRectSize(sizeCache);
        if (!hasError) {
            EditorWidget.textCenterAlign(message);
            return;
        }
        if (ImGui.beginChild("##ST_Build_Error_View_Region", ImGuiChildFlags.Borders)) ErrorView.render("##ST_Build_Error_View", ErrorRows);
        ImGui.endChild();
    }

    private static void renderClearButton(Runnable onClear, float height) {
        ImGui.beginGroup();
        ImGui.pushStyleColor(ImGuiCol.Button, EditorColors.TransparentColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, EditorColors.TransparentColor);
        if (ImGui.button("X" + "##ST_Clear_New_ScanDir_Name", 0.0f, height)) onClear.run();
        ImGui.setItemTooltip("Clear new scan directory name");
        ImGui.popStyleColor(2);
        ImGui.endGroup();
    }

    private static void renderScanDirList() {
        if (!ImGui.beginTable("##ST_ScanDirName_List_Table", 2, ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvailX())) return;
        ImGui.tableSetupColumn("##ST_ScanDirName_Content_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##ST_ScanDirName_Delete_Column", ImGuiTableColumnFlags.WidthFixed);
        List<String> scanDirs = Project.scriptScanDirs();
        for (String dir : scanDirs) {
            ImGui.tableNextColumn();
            ImGui.text(dir);
            ImGui.tableNextColumn();
            String deleteDirId = "Remove##ST_Remove_ScanDir_" + dir;
            if (!EditorWidget.iconButton(deleteDirId, EditorIcons.Icons.Delete, "Remove scan for this directory")) continue;
            if (Project.removeScriptScanDir(dir)) break;
        }
        ImGui.endTable();
    }

    private static void renderScriptControl() {
        if (!ImGui.beginTable("ST_Main_Script_Control_Table_Layout", 2, ImGuiTableFlags.SizingFixedFit, ImGui.getContentRegionAvailX(), mainScriptControlLayoutInnerHeight)) return;
        ImGui.tableSetupColumn("ST_Main_Script_Control_Buton_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("ST_Main_Script_Control_Description_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        ImGui.beginGroup();
        ImGui.spacing();
        if (ImGui.button(BuildScriptLabel, ButtonWidth, ButtonHeight)) requestBuildScript();
        if (ImGui.isItemHovered()) hoveredControl = HoveredControl.BuildScript;
        ImGui.spacing();
        if (ImGui.button(UpdateScriptProjectLabel, ButtonWidth, ButtonHeight)) {}
        if (ImGui.isItemHovered()) hoveredControl = HoveredControl.UpdateScriptProject;
        ImGui.spacing();
        if (ImGui.button(GenerateScriptProjectButton, ButtonWidth, ButtonHeight)) {
            if (!ScriptProjectGenerator.generate(Project.projectRoot())) EngineLog.error("Project Generator", "Failed to create script project");
        }
        if (ImGui.isItemHovered()) hoveredControl = HoveredControl.GenerateScriptProject;
        ImGui.spacing();
        ImGui.endGroup();
        mainScriptControlLayoutInnerHeight = ImGui.getItemRectSizeY();
        ImGui.tableNextColumn();
        if (ImGui.beginChild("##ST_Main_Script_Control_Description_Region", ImGui.getContentRegionAvailX(), mainScriptControlLayoutInnerHeight, ImGuiChildFlags.Borders)) renderDescription();
        ImGui.endChild();
        ImGui.endTable();
        ImGui.separator();
    }

    private static void renderDescription() {
        ImGui.pushStyleColor(ImGuiCol.Text,  EditorColors.InstructionHighLight);
        switch (hoveredControl) {
            case BuildScript -> {
                ImGui.textWrapped("Click \"Build Script\" to compile script classes into JAR and load them into the engine.");
                ImGui.textWrapped("All .tcbs script file will be translated to .java source first.");
                ImGui.textWrapped("This will also reload the current scene if that option is enabled in Editor Preferences.");
            }
            case UpdateScriptProject -> {
                ImGui.textWrapped("Click \"Update Script Project\" to update the API version to the latest version supported by the engine.");
                ImGui.textWrapped("This will also update the Gradle's wrapper. None of your build script will be affected.");
                ImGui.textWrapped("Please check for breaking change after updating if available.");
            }
            case GenerateScriptProject -> {
                ImGui.textWrapped("Click \"Generate Script Project\" to create a new script project in \"scripts-src\" directory.");
                ImGui.textWrapped("A fully functional Java project with Gradle will be created, along with the engine's API dependencies setup.");
                ImGui.textWrapped("Note: only missing template files will be created.");
            }
            default -> {
                ImGui.textWrapped("To get started on writing script for your game, click \"Generate Script Project\" to generate required directories and files");
                ImGui.textWrapped("If you already have a script project, you can click \"Build Script\" to build and import your script into the engine, or click \"Update Script Project\" to update your existing script project.");
            }
        }
        ImGui.popStyleColor(1);
    }

    private static boolean canAddNewScanDir(String newDirName) {
        if (newDirName == null || newDirName.isBlank()) return false;
        newDirName = newDirName.trim();
        List<String> dirs = Project.scriptScanDirs();
        return !dirs.contains(newDirName);
    }

    private static void requestBuildScript() {
        if (ScriptBuilder.inProgress()) return;
        message = "Starting script project build task...";
        EditorPreferences preferences = UserPreference.preferences();
        ScriptBuilder.build(UserPreference.selectedJDKHome(), preferences.cleanBuildScripts(), preferences.overrideGradleJVM(), preferences.reloadOnFinishBuildScripts());
    }

    private static void refreshStatus() {
        BuildStatus status = ScriptBuilder.status();
        if (status == lastStatus) return;
        lastStatus = status;
        hasError = status.phase() == BuildPhase.TranspilerFailed || status.phase() == BuildPhase.BuildFailed;
        if (hasError) {
            rebuildErrorRows(status);
            return;
        }
        message = switch (status.phase()) {
            case Transpiling -> "Translating .tcbs script files...";
            case Building -> String.format("Translated %d .tcbs files. Building script project...", status.scriptCount());
            case Succeeded -> String.format("Build script project finished in %s (Exit code %d)", formatDuration(status.buildMS()), status.exitCode());
            default -> message;
        };
    }

    private static void rebuildErrorRows(BuildStatus status) {
        ErrorRows.clear();
        ErrorView.clearSelection();
        if (status.errors().isEmpty()) {
            ErrorRows.add(new Row(String.format("Script build failed (exit code %d), check the Gradle output in the console", status.exitCode()), EditorColors.ErrorLogColor));
            return;
        }
        status.errors().forEach(e -> ErrorRows.add(new Row(e, EditorColors.ErrorLogColor)));
    }

    private static String formatDuration(long durationMs) {
        if (durationMs < 1000L) return durationMs + "ms";
        long hours = durationMs / 3_600_000L;
        long minutes = (durationMs / 60_000L) % 60L;
        long seconds = (durationMs / 1000L) % 60L;
        StringBuilder elapsed = new StringBuilder();
        if (hours > 0) elapsed.append(hours).append("h ");
        if (minutes > 0) elapsed.append(minutes).append("m ");
        if (seconds > 0) elapsed.append(seconds).append("s");
        return elapsed.toString().trim();
    }
}
