package editor.dialogs;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import org.joml.Vector4f;
import physic2d.Physic2D;
import project.*;

final class ProjectPreferenceTab {
    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static final ImBoolean allowResize = new ImBoolean(false);
    private static final ImBoolean maintainAspectRatio = new ImBoolean(true);
    private static final ImFloat textureGlobalScale = new ImFloat(1.0f);
    private static final Vector4f clearColor = new Vector4f(0.027f, 0.122f, 0.067f, 1.0f);
    private static final ImInt physicFramerate = new ImInt(60);
    private static final ImInt renderFramerate = new ImInt(60);
    private static VsyncMode vsyncMode = VsyncMode.Enabled;
    private static WindowResizeMode resizeMode = WindowResizeMode.Scale;
    private static boolean projectPreferencesChanged = false;

    static void reloadPreferenceData() {
        projectPreferencesChanged = false;
        ProjectPreference preference = Project.preference();
        gameTitle.set(preference.name());
        gameWindowSize.set(preference.gameWindowWidth(), preference.gameWindowHeight());
        allowResize.set(preference.allowResize());
        maintainAspectRatio.set(preference.maintainAspectRatio());
        textureGlobalScale.set(preference.textureGlobalScale());
        clearColor.set(preference.clearColor().toVector());
        physicFramerate.set(preference.physicFrameRate());
        renderFramerate.set(preference.renderingSetting().targetFrameRate());
        vsyncMode = preference.renderingSetting().vsyncMode();
        resizeMode = preference.resizeMode();
    }

    static void autoSavePreferences() {
        if (!projectPreferencesChanged) return;
        if (gameTitle.isEmpty() || gameWindowSize.x <= 0 || gameWindowSize.y <= 0) return;
        boolean success = Project.updateProjectPreference(gameTitle.get(),
                gameWindowSize.x, gameWindowSize.y,
                allowResize.get(), resizeMode, maintainAspectRatio.get(),
                textureGlobalScale.get(), new ClearColor(clearColor),
                physicFramerate.get(),
                new RenderingSetting(vsyncMode, renderFramerate.get())
        );
        if (success) projectPreferencesChanged = false;
    }

    static void imgui() {
        boolean openDetail = ImGui.collapsingHeader("Detail##Game_Detail_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (openDetail) renderGameDetailHeaderContent();
        ImGui.spacing();
        boolean openWindow = ImGui.collapsingHeader("Window##Game_Window_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (openWindow) renderGameWindowHeaderContent();
        ImGui.spacing();
        boolean openTexture = ImGui.collapsingHeader("Texture##Game_Texture_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (openTexture) renderGameTextureHeaderContent();
        ImGui.spacing();
        boolean openPhysic = ImGui.collapsingHeader("Physic##Game_Physic_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (openPhysic) renderGamePhysicHeaderContent();
        ImGui.spacing();
        boolean openRender = ImGui.collapsingHeader("Rendering##Game_Rendering_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (openRender) renderGameRenderingHeaderContent();
    }

    private static void renderGameDetailHeaderContent() {
        ImGui.separator();
        ImGui.indent();
        ImGui.text("Title:");
        String oldTitle = gameTitle.get();
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (!oldTitle.equals(gameTitle.get())) projectPreferencesChanged = true;
        if (gameTitle.isEmpty()) ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Game title cannot be empty");
        else ImGui.newLine();
        ImGui.unindent();
        ImGui.separator();
    }

    private static void renderGameWindowHeaderContent() {
        ImGui.separator();
        ImGui.indent();
        ImGui.text("Window size:");
        ImGui.beginDisabled();
        ImGui.textWrapped("Determine the default size of game window");
        ImGui.endDisabled();
        ImGui.spacing();
        Vector2i oldSize = new Vector2i(gameWindowSize);
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        if (!oldSize.equals(gameWindowSize)) projectPreferencesChanged = true;
        ImGui.spacing();
        renderPreferenceToggle("Resizable", allowResize, "Allow player to resize the game window");
        ImGui.spacing();
        renderPreferenceToggle("Lock aspect ratio", maintainAspectRatio, "Maintain the game's intended aspect ratio when window is resized");
        ImGui.spacing();
        if (EditorWidget.colorCtrl("Clear color", clearColor, ProjectPreferenceTab.class)) projectPreferencesChanged = true;
        ImGui.spacing();
        WindowResizeMode oldMode = resizeMode;
        if (ImGui.beginCombo("Resize Mode", oldMode != null ? oldMode.name() : "Select a Resize mode...")) {
            for (WindowResizeMode mode : WindowResizeMode.values()) {
                String id = mode.name() + "##EPTD_Window_Resize_Mode_" + mode + "_Selectable";
                if (!ImGui.selectable(id, oldMode == mode)) continue;
                resizeMode = mode;
                projectPreferencesChanged = true;
            }
            ImGui.endCombo();
        }
        ImGui.beginDisabled();
        ImGui.textWrapped("Scale: resize the game view | Expand: resize the visible area");
        ImGui.endDisabled();
        ImGui.unindent();
        ImGui.separator();
    }

    private static void renderGameTextureHeaderContent() {
        ImGui.separator();
        ImGui.indent();
        float textureScale = textureGlobalScale.get();
        textureGlobalScale.set(inputFloat("Global Scaling", textureGlobalScale.get(), 0.01f));
        if (textureScale != textureGlobalScale.get()) projectPreferencesChanged = true;
        ImGui.beginDisabled();
        ImGui.textWrapped("Rendering scale of textures on scene, project-wise.");
        ImGui.endDisabled();
        ImGui.unindent();
        ImGui.separator();
    }

    private static void renderGamePhysicHeaderContent() {
        ImGui.separator();
        ImGui.indent();
        int oldFrameRate = physicFramerate.get();
        physicFramerate.set(inputInt("Physic Framerate", physicFramerate.get(), 5));
        if (oldFrameRate != physicFramerate.get()) projectPreferencesChanged = true;
        ImGui.beginDisabled();
        ImGui.textWrapped("Fixed physic update tick per second (5-120, default 60)");
        ImGui.endDisabled();
        ImGui.spacing();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openLayer = ImGui.collapsingHeader("Physic Layer Name##EPTD_Physic_Layer_Name_Edit_Preference_Header", ImGuiTreeNodeFlags.DefaultOpen);
        ImGui.popStyleColor(1);
        if (openLayer) renderPhysicLayerNames();
        ImGui.unindent();
        ImGui.separator();
    }

    private static void renderPhysicLayerNames() {
        ImGui.indent();
        if (!ImGui.beginTable("##EPTD_Physic_Layer_Name_Edit_Layout_Table", 2, ImGuiTableFlags.SizingFixedFit)) {
            ImGui.unindent();
            return;
        }
        ImGui.tableSetupColumn("##EPTD_Physic_Layer_Label_Column", ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##EPTD_Physic_Layer_Name_Input_Column", ImGuiTableColumnFlags.WidthStretch);
        int maxLayer = Physic2D.MaxLayer;
        for (int i = 0; i < maxLayer; i++) {
            ImGui.tableNextColumn();
            ImGui.text("Layer " + i);
            ImGui.tableNextColumn();
            String oldLayerName = Project.getPhysicLayerName(i);
            ImString newLayerName = new ImString(oldLayerName, 128);
            boolean edited = ImGui.inputTextWithHint("##EPTD_Custom_Layer_Name_" + i, "Enter a custom name for layer " + i + "...", newLayerName);
            if (edited) Project.updatePhysicLayerName(i, newLayerName.get().trim());
        }
        ImGui.endTable();
        ImGui.unindent();
    }

    private static void renderGameRenderingHeaderContent() {
        ImGui.separator();
        ImGui.indent();
        VsyncMode oldMode = vsyncMode;
        if (ImGui.beginCombo("Vsync Mode", oldMode != null ? oldMode.name() : "Select a Vsync mode...")) {
            for (VsyncMode mode : VsyncMode.values()) {
                String id = mode.name() + "####EPTD_Rendering_Setting_VsyncMode_" + mode + "_Selectable";
                if (!ImGui.selectable(id, oldMode == mode)) continue;
                vsyncMode = mode;
                projectPreferencesChanged = true;
            }
            ImGui.endCombo();
        }
        ImGui.beginDisabled();
        ImGui.textWrapped("Disabled: vsync disabled | Adaptive: sync when above refresh rate | Enabled: sync to monitor");
        ImGui.endDisabled();
        ImGui.spacing();
        int oldFrameRate = renderFramerate.get();
        renderFramerate.set(inputInt("Frame Rate Limit##EPTD_FPS_Limit_Input", renderFramerate.get(), RenderingSetting.MinFrameRate));
        if (oldFrameRate != renderFramerate.get()) projectPreferencesChanged = true;
        ImGui.beginDisabled();
        ImGui.textWrapped("Max rendering FPS, default is 60 (0 = unlimited)");
        ImGui.endDisabled();
        ImGui.unindent();
        ImGui.separator();
    }

    private static void renderPreferenceToggle(String label, ImBoolean dest, String extraInfo) {
        if (!ImGui.beginTable("##EPTD_Toggle_Preference_Layout_Table_" + label, 2)) return;
        ImGui.tableSetupColumn("##EPTD_Toggle_Preference_Checkbox_Column_" + label, ImGuiTableColumnFlags.WidthFixed);
        ImGui.tableSetupColumn("##EPTD_Toggle_Preference_Info_Column_" + label, ImGuiTableColumnFlags.WidthStretch);
        ImGui.tableNextColumn();
        if (ImGui.checkbox(label + "##EPTD_" + label, dest)) projectPreferencesChanged = true;
        ImGui.tableNextColumn();
        ImGui.beginDisabled();
        ImGui.textWrapped(extraInfo);
        ImGui.endDisabled();
        ImGui.endTable();
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + EditProjectSettingsDialog.IDPool().newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);
        modified = ImGui.inputInt(label, destination, 1, 8);
        if (modified) target = Math.max(destination.get(), minValue);
        ImGui.popID();
        return target;
    }

    private static float inputFloat(String label, float target, float minValue) {
        String id = label + "_" + EditProjectSettingsDialog.IDPool().newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImFloat destination = new ImFloat(target);
        modified = ImGui.inputFloat(label, destination);
        if (modified) target = Math.max(destination.get(), minValue);
        ImGui.popID();
        return target;
    }
}
