package editor.dialog;

import imgui.ImGui;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import org.joml.Vector2i;
import project.Project;
import project.ProjectPreference;

class ProjectPreferenceTab {
    private static final ImString gameTitle = new ImString(128);
    private static final Vector2i gameWindowSize = new Vector2i(640, 480);
    private static final ImBoolean allowResize = new ImBoolean(false);
    private static final ImBoolean maintainAspectRatio = new ImBoolean(true);
    private static final ImFloat textureGlobalScale = new ImFloat(1.0f);
    private static boolean projectPreferencesChanged = false;

    static void reloadPreferenceData() {
        projectPreferencesChanged = false;
        ProjectPreference preference = Project.preference();
        gameTitle.set(preference.name());
        gameWindowSize.set(preference.gameWindowWidth(), preference.gameWindowHeight());
        allowResize.set(preference.allowResize());
        maintainAspectRatio.set(preference.maintainAspectRatio());
        textureGlobalScale.set(preference.textureGlobalScale());
    }

    static void autoSavePreferences() {
        if (!projectPreferencesChanged) return;
        if (gameTitle.isEmpty() || gameWindowSize.x <= 0 || gameWindowSize.y <= 0) return;

        int width = gameWindowSize.x;
        int height = gameWindowSize.y;
        float scale = Math.max(0.01f, textureGlobalScale.get());

        boolean success =Project.updateProjectPreference(gameTitle.get(),
                width, height,
                allowResize.get(), maintainAspectRatio.get(),
                scale
        );

        if (success) projectPreferencesChanged = false;
    }

    static void imgui() {
        ImGui.text("Title:");
        boolean updated = false;
        String oldTitle = gameTitle.get();
        ImGui.inputTextWithHint("##Game title", "Enter a name for the project...", gameTitle);
        if (!oldTitle.equals(gameTitle.get())) updated = true;
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
        Vector2i oldSize = new Vector2i(gameWindowSize);
        gameWindowSize.x = inputInt("Width", gameWindowSize.x, 1);
        gameWindowSize.y = inputInt("Height", gameWindowSize.y, 1);
        if (!oldSize.equals(gameWindowSize)) updated = true;
        ImGui.spacing();

        boolean resizable = allowResize.get();
        ImGui.checkbox("Resizable", allowResize);
        if (resizable != allowResize.get()) updated = true;
        ImGui.beginDisabled();
        ImGui.textWrapped("(allow player to resize the game window)");
        ImGui.endDisabled();
        ImGui.spacing();

        boolean lockAspectRatio = maintainAspectRatio.get();
        ImGui.checkbox("Lock aspect ratio", maintainAspectRatio);
        if (lockAspectRatio != maintainAspectRatio.get()) updated = true;
        ImGui.beginDisabled();
        ImGui.textWrapped("(maintain the game's intended aspect ratio when window is resized)");
        ImGui.endDisabled();
        ImGui.spacing();

        ImGui.text("Texture :");
        ImGui.beginDisabled();
        ImGui.textWrapped("Settings that affect the appearance of texture, project-wise.");
        ImGui.endDisabled();
        float textureScale = textureGlobalScale.get();
        textureGlobalScale.set(inputFloat("Global Scaling", textureGlobalScale.get(), 0.01f));
        if (textureScale != textureGlobalScale.get()) updated = true;

        if (updated) projectPreferencesChanged = true;
    }

    private static int inputInt(String label, int target, int minValue) {
        String id = label + "_" + EditProjectSettingsDialog.ID_POOL().newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImInt destination = new ImInt(target);

        modified = ImGui.inputInt(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }

    private static float inputFloat(String label, float target, float minValue) {
        String id = label + "_" + EditProjectSettingsDialog.ID_POOL().newId();
        ImGui.pushID(id);
        final boolean modified;
        final ImFloat destination = new ImFloat(target);

        modified = ImGui.inputFloat(label, destination);

        if (modified) target = Math.max(destination.get(), minValue);

        ImGui.popID();
        return target;
    }
}
