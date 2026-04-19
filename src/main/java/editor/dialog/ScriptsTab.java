package editor.dialog;

import editor.EditorColors;
import editor.EditorIcons;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.type.ImString;
import project.Project;
import scripting.ScriptLoader;
import scripting.ScriptProjectGenerator;
import utility.log.EngineLog;

import java.util.List;


class ScriptsTab {
    private static final int transparentColor = ImGui.colorConvertFloat4ToU32(0.0f, 0.0f, 0.0f, 0.0f);
    private static final float padding = 4.0f;
    private static final ImString newScanDirName = new ImString(256);
    private static final ImVec2 sizeCache = new ImVec2();

    static void imgui() {
        ImGui.spacing();
        ImGui.textWrapped("The engine scans these directories for .jar files for custom GameObject and Component classes.");
        ImGui.pushStyleColor(ImGuiCol.Text, EditorColors.YellowHighLight);
        ImGui.textWrapped("You can organize the .jars into sub-directories for better organization.");
        ImGui.popStyleColor(1);
        ImGui.spacing();
        ImGui.separator();
        if (!ImGui.beginTable("##ST_New_ScanDir_Table", 4, ImGuiTableFlags.BordersInnerV | ImGuiTableFlags.SizingFixedFit)) return;
        ImGui.tableSetupColumn("##ST_Reload_Script_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##ST_Padding_Column", ImGuiTableColumnFlags.WidthFixed, padding);
        ImGui.tableSetupColumn("##ST_New_ScanDir_Input_Layout_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##ST_Add_New_ScanDir_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        if (EditorWidget.iconButton("Reload Script##ST_Reload_Script_Button", EditorIcons.Icons.Reset, "Click to reload scripts")) ScriptLoader.reload();
        ImGui.tableNextColumn();
        ImGui.text("\t");
        ImGui.tableNextColumn();
        if (ImGui.beginTable("##ST_New_ScanDir_Input_Layout", 2, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.tableSetupColumn("ST_New_ScanDir_Input_Column", ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("ST_Clear_New_ScanDir_Column", ImGuiTableColumnFlags.WidthFixed);
            ImGui.tableNextColumn();
            ImGui.pushItemWidth(ImGui.getContentRegionAvailX());
            ImGui.inputTextWithHint("##ST_New_ScanDir_Input", "Add new Scan directory...", newScanDirName);
            ImGui.popItemWidth();
            ImGui.tableNextColumn();
            renderClearButton("##ST_Clear_New_ScanDir_Name", "Clear new scan directory name", newScanDirName::clear);
            ImGui.endTable();
        }
        ImGui.tableNextColumn();
        boolean canAdd = canAddNewScanDir(newScanDirName.get());
        if (!canAdd) ImGui.beginDisabled();
        if (ImGui.button("Add Directory##ST_Add_New_ScanDir_Button")) {
            String dirName = newScanDirName.get().trim();
            if (Project.addScriptScanDir(dirName)) newScanDirName.clear();
        }
        if (!canAdd) ImGui.endDisabled();
        ImGui.endTable();
        ImGui.separator();
        ImVec2 remainSize = ImGui.getContentRegionAvail();
        if (!ImGui.beginChild("##ST_ScanDir_List_Region", 0.0f, remainSize.y - sizeCache.y - padding)) {
            ImGui.endChild();
            return;
        }
        renderScanDirList();
        ImGui.endChild();
        ImGui.beginGroup();
        ImGui.separator();
        ImGui.beginDisabled();
        ImGui.textWrapped(String.format("Loaded %d object type(s) and %d component type(s)", ScriptLoader.gameObjectTypes().size(), ScriptLoader.componentTypes().size()));
        ImGui.endDisabled();
        ImGui.separator();
        if (!ImGui.beginTable("##ST_Create_Script_Project_Layout", 2, ImGuiTableFlags.SizingFixedFit | ImGuiTableFlags.BordersInnerV)) return;
        ImGui.tableSetupColumn("##ST_Create_Script_Project_Instruction_Column", ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableSetupColumn("##ST_Create_Script_Project_Button_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableNextColumn();
        ImGui.beginGroup();
        ImGui.pushStyleColor(ImGuiCol.Text,  EditorColors.InstructionHighLight);
        ImGui.textWrapped("To get started, click \"Generate Script Project\" to create a Gradle project in scripts-src/.");
        ImGui.textWrapped("Write your classes there, annotate them with @RegisterGameObject or @RegisterComponent, " +
                "then build them with 'gradlew build'. The output JAR goes to scripts/ automatically.");
        ImGui.popStyleColor(1);
        ImGui.endGroup();
        ImVec2 size = new ImVec2();
        ImGui.getItemRectSize(size);
        ImGui.tableNextColumn();
        if (ImGui.button("Generate Script Project##ST_Generate_Script_Project_Button", 0.0f, size.y)) {
            if (!ScriptProjectGenerator.generate(Project.projectRoot())) EngineLog.error("Project Generator", "Failed to create script project");
        }
        ImGui.endTable();
        ImGui.endGroup();
        ImGui.getItemRectSize(sizeCache);
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
            if (EditorWidget.iconButton(deleteDirId, EditorIcons.Icons.Delete, "Remove scan for this directory")) {
                if (Project.removeScriptScanDir(dir)) break;
            }
        }
        ImGui.endTable();
    }

    private static void renderClearButton(String id, String hint, Runnable onClear) {
        ImGui.beginGroup();
        ImGui.pushStyleColor(ImGuiCol.Button, transparentColor);
        ImGui.pushStyleColor(ImGuiCol.ButtonHovered, transparentColor);
        if (ImGui.button("X" + id)) onClear.run();
        ImGui.popStyleColor(2);
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text(hint);
            ImGui.endTooltip();
        }
        ImGui.endGroup();
    }

    private static boolean canAddNewScanDir(String newDirName) {
        if (newDirName == null || newDirName.isBlank()) return false;
        newDirName = newDirName.trim();
        List<String> dirs = Project.scriptScanDirs();
        return !dirs.contains(newDirName);
    }
}
