package editor.template;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;

/**
 * Template for {@link GameObject2D}'s editor UI.
 */
class GameObject2DTemplate implements ObjectTemplate<GameObject2D> {
    private static final GameObject2DTemplate instance = new GameObject2DTemplate();
    private static final Transform2D editing = new Transform2D("Template cache");
    private GameObject2DTemplate() {}

    /**
     * Execute the rendering code for the Editor UI, related to this object.
     * This method is passive, and mst be call to render the UI.
     *
     * @param object the context object
     */
    @Override
    public void editorUI(GameObject2D object) {
        ImGui.spacing();
        boolean openTransform = ImGui.collapsingHeader("GameObject2D##Transform_GameObject2D_Properties_" + object.getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openTransform) return;
        Transform2D.copy(object.localTransform(), editing);
        ImGui.indent();
        editing.imgui();
        ImGui.unindent();
        object.localTransform(editing);
    }

    /**
     * Render the content of {@link #editorUI(GameObject2D)}
     * @param gameObject2D the context object
     */
    static void render(GameObject2D gameObject2D) {
        if (gameObject2D == null) return;
        instance.editorUI(gameObject2D);
    }
}
