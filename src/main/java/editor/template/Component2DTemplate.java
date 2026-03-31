package editor.template;

import TheCellBeyond.Transform2D;
import components.Component2D;
import imgui.ImGui;

/**
 * Template for {@link Component2D}'s editor UI.
 */
final class Component2DTemplate implements IComponentTemplate<Component2D> {
    private static final Component2DTemplate instance = new Component2DTemplate();
    private static final Transform2D editing = new Transform2D("Component2D Template Cache");
    private Component2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(Component2D component) {
        ImGui.spacing();
        boolean openTransform = ImGui.collapsingHeader("Component2D##Transform_Component2D_Properties_" + component.getUUID());
        if (!openTransform) return;
        Transform2D.copy(component.localTransform(), editing);
        ImGui.indent();
        Transform2DTemplate.render(editing);
        ImGui.unindent();
        component.localTransform(editing);
    }

    /**
     * Render the content of {@link #editorUI(Component2D)}.
     * @param component2D the context component
     */
    static void render(Component2D component2D) {
        if (component2D == null) return;
        instance.editorUI(component2D);
    }
}
