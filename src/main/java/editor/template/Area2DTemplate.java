package editor.template;

import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import physic2d.Area2D;
import physic2d.CollisionObject2D;

import java.util.UUID;

/**
 * Template for {@link Area2D}'s editor UI.
 */
final class Area2DTemplate implements IObjectTemplate<Area2D> {
    private static final Area2DTemplate instance = new Area2DTemplate();
    private Area2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(Area2D object) {
        ImGui.spacing();
        UUID uuid = object.getUUID();
        boolean openArea = ImGui.collapsingHeader("Area2D##Area2D_Properties_Header_" + uuid, ImGuiTreeNodeFlags.DefaultOpen);
        if (!openArea) {
            return;
        }
        ImBoolean tmp = new ImBoolean(object.monitoring);
        if (ImGui.checkbox("Monitoring##Area2D_Monitoring_Checkbox_" + uuid, tmp)) object.monitoring = tmp.get();
        tmp.set(object.monitorable);
        if (ImGui.checkbox("Monitorable##Area2D_Monitorable_Checkbox_" + uuid, tmp)) object.monitorable = tmp.get();
    }

    /**
     * Render the content of {@link #editorUI(Area2D)} and call {@link CollisionObject2DTemplate#render(CollisionObject2D)}.
     * @param area2D the context object
     */
    static void render(Area2D area2D) {
        if (area2D == null) return;
        instance.editorUI(area2D);
        CollisionObject2DTemplate.render(area2D);
    }
}
