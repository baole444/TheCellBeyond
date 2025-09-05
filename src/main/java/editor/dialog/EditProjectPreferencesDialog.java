package editor.dialog;

import editor.project.Project;
import editor.project.ProjectPreference;
import imgui.ImGui;
import imgui.ImVec2;
import imgui.flag.ImGuiCond;
import imgui.flag.ImGuiWindowFlags;
import imgui.type.ImBoolean;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import utility.IdPool;

public class EditProjectPreferencesDialog {
    private static final IdPool ID_POOL = new IdPool(0, false);
    private static final String POPUP_ID = "Project Preferences";
    private static final String PREFERENCE_ID = "Preference_Editor";
    private static final ImVec2 DIALOG_SIZE = new ImVec2(720.0f, 400.0f);
    private static boolean showDialog = false;
    private static final boolean enableBorder = true;

    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static final ImBoolean allowResize = new ImBoolean(false);
    private static final ImBoolean maintainAspectRatio = new ImBoolean(true);

    private static final float metaYPercentage = 0.85f;

    public static void show() {
        showDialog = true;
        loadFromPreference();
    }

    public static void imgui() {
        if (!showDialog) return;

        ImGui.openPopup(POPUP_ID);

        ImVec2 centre = ImGui.getMainViewport().getCenter();
        float pivotXY = 0.5f;

        ImGui.setNextWindowPos(centre.x, centre.y, ImGuiCond.Appearing, pivotXY, pivotXY);
        ImGui.setNextWindowSize(DIALOG_SIZE, ImGuiCond.FirstUseEver);

        if (ImGui.beginPopupModal(POPUP_ID, ImGuiWindowFlags.NoResize | ImGuiWindowFlags.NoScrollbar)) {
            ImGui.text("Adjust project's references");
            renderPreferenceEditor();

            float buttonWidth = 120;
            float buttonHeight = 30;
            float buttonReserverY = ImGui.getFrameHeightWithSpacing();
            ImGui.setCursorPosY(ImGui.getWindowHeight() - buttonReserverY - (buttonHeight / 2) - ImGui.getStyle().getWindowPaddingY());
            float buttonPivotX = buttonWidth * 0.5f;
            float availX = ImGui.getContentRegionAvailX();
            float applyX = (availX * 0.15f) - buttonPivotX;
            float saveX = (availX * 0.5f) - buttonPivotX;
            float cancelX = (availX * 0.85f) - buttonPivotX;
            boolean canModify = gameTitle.isNotEmpty() && gameWindowSize.x > 0 && gameWindowSize.y > 0;
            ImGui.setCursorPosX(applyX);
            if (canModify) {
                if (ImGui.button("Apply", buttonWidth, buttonHeight)) savePreference();
            } else {
                ImGui.beginDisabled();
                ImGui.button("Apply", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(saveX);
            if (canModify) {
                if (ImGui.button("Save", buttonWidth, buttonHeight)) {
                    savePreference();
                    showDialog = false;
                    ImGui.closeCurrentPopup();
                }
            } else {
                ImGui.beginDisabled();
                ImGui.button("Save", buttonWidth, buttonHeight);
                ImGui.endDisabled();
            }

            ImGui.sameLine();
            ImGui.setCursorPosX(cancelX);
            if (ImGui.button("Cancel", buttonWidth, buttonHeight)) {
                showDialog = false;
                ImGui.closeCurrentPopup();
            }

            ImGui.endPopup();
            ID_POOL.reset();
        }

        if (!ImGui.isPopupOpen(POPUP_ID)) showDialog = false;
    }

    private static void renderPreferenceEditor() {
        int sectionY = (int) (ImGui.getContentRegionAvailY() * metaYPercentage);
        ImGui.beginChild(PREFERENCE_ID, 0, sectionY, enableBorder);
        ImGui.text("Title:");
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (gameTitle.isEmpty()) {
            ImGui.textColored(ImGui.colorConvertFloat4ToU32(1.0f, 0.2f, 0.2f, 1.0f), "Game title cannot be empty");
        } else {
            ImGui.newLine();
        }

        ImGui.spacing();
        ImGui.text("Window size:");
        ImGui.beginDisabled();
        ImGui.textWrapped("Determine the default size of game window");
        ImGui.endDisabled();
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        ImGui.spacing();

        ImGui.checkbox("Resizable", allowResize);
        ImGui.beginDisabled();
        ImGui.textWrapped("(allow player to resize the game window)");
        ImGui.endDisabled();
        ImGui.spacing();

        ImGui.checkbox("Lock aspect ratio", maintainAspectRatio);
        ImGui.beginDisabled();
        ImGui.textWrapped("(maintain the game's intended aspect ratio when window is resized)");
        ImGui.endDisabled();

        ImGui.endChild();
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + ID_POOL.newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }

    private static void loadFromPreference() {
        ProjectPreference preference = Project.preference();
        gameTitle.set(preference.name());
        gameWindowSize.set(preference.gameWindowWidth(), preference.gameWindowHeight());
        allowResize.set(preference.allowResize());
        maintainAspectRatio.set(preference.maintainAspectRatio());
    }

    private static void savePreference() {
        if (gameTitle.isEmpty()) gameTitle.set("Untitled");
        if (gameWindowSize.x < 1) gameWindowSize.set(1, gameWindowSize.y);
        if (gameWindowSize.y < 1) gameWindowSize.set(gameWindowSize.x, 1);

        Project.updateProjectPreference(gameTitle.get(),
                gameWindowSize.x, gameWindowSize.y,
                allowResize.get(), maintainAspectRatio.get()
        );
    }
}
