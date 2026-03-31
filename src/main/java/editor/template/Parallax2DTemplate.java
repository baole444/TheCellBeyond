package editor.template;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Parallax2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;

import java.util.UUID;

/**
 * Template for {@link Parallax2D}'s editor UI.
 */
final class Parallax2DTemplate implements IObjectTemplate<Parallax2D> {
    private static final Parallax2DTemplate instance = new Parallax2DTemplate();
    private Parallax2DTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(Parallax2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openParallax = ImGui.collapsingHeader("Parallax2D##Parallax2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openParallax) return;
        EditorWidget.dragVec2Ctrl("Scroll Scale", object.scrollScale, 1.0f, 0.1f, object);
        EditorWidget.dragVec2Ctrl("Scroll Offset", object.scrollOffset, 0.0f, 0.1f, object);
        ImGui.spacing();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openRepeat = ImGui.collapsingHeader("Repeat##Parallax2D_Repeat_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        ImGui.popStyleColor(1);
        if (openRepeat) renderRepeatProperties(object);
        ImGui.spacing();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openLimit = ImGui.collapsingHeader("Limits##Parallax2D_Limit_Properties_Header_" + uuid);
        ImGui.popStyleColor(1);
        if (openLimit) renderLimitProperties(object);
        ImGui.spacing();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean openOverride = ImGui.collapsingHeader("Overrides##Parallax2D_Overrides_Properties_Header_" + uuid);
        ImGui.popStyleColor(1);
        if (openOverride) renderOverrideProperties(object, uuid);
        ImGui.spacing();
    }

    private static void renderRepeatProperties(Parallax2D object) {
        ImGui.indent();
        object.repeatTime = EditorWidget.dragIntCtrl("Repeat Time", object.repeatTime, 1, object, 1);
        EditorWidget.dragVec2Ctrl("Repeat Size", object.repeatSize, 0.0f, 0.1f, object);
        EditorWidget.dragVec2Ctrl("Autoscroll Velocity", object.autoScrollVelocity, 0.0f, 0.1f, object);
        ImGui.unindent();
    }

    private static void renderLimitProperties(Parallax2D object) {
        ImGui.indent();
        object.bottomLeftLimit.x = EditorWidget.dragFloatCtrl("Left Limit", object.bottomLeftLimit.x, -1024.0f, 1.0f, object);
        object.topRightLimit.x = EditorWidget.dragFloatCtrl("Right Limit", object.topRightLimit.x, 1024.0f, 1.0f, object);
        object.topRightLimit.y = EditorWidget.dragFloatCtrl("Top Limit", object.topRightLimit.y, 1024.0f, 1.0f, object);
        object.bottomLeftLimit.y = EditorWidget.dragFloatCtrl("Bottom Limit", object.bottomLeftLimit.y, -1024.0f, 1.0f, object);
        ImGui.unindent();
    }

    private static void renderOverrideProperties(Parallax2D object, UUID uuid) {
        ImGui.indent();
        ImBoolean tmp = new ImBoolean(object.followViewport);
        if (ImGui.checkbox("Follow Viewport##Parallax2D_FollowViewport_Checkbox_" + uuid, tmp)) object.followViewport = tmp.get();
        tmp.set(object.ignoreViewportScroll);
        if (ImGui.checkbox("Ignore Viewport Scroll##Parallax2D_IgnoreViewportScroll_Checkbox_" + uuid, tmp)) object.ignoreViewportScroll = tmp.get();
        boolean ignoreVPScroll = object.ignoreViewportScroll;
        if (!ignoreVPScroll) ImGui.beginDisabled();
        EditorWidget.dragVec2Ctrl("Screen Offset", object.screenOffset, 0.0f, 0.1f, object);
        if (!ignoreVPScroll) ImGui.endDisabled();
        ImGui.unindent();
    }

    /**
     * Render the content of {@link #editorUI(Parallax2D)} and call {@link GameObject2DTemplate#render(GameObject2D)}.
     * @param parallax2D the context object
     */
    static void render(Parallax2D parallax2D) {
        if (parallax2D == null) return;
        instance.editorUI(parallax2D);
        GameObject2DTemplate.render(parallax2D);
    }
}
