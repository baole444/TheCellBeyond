package editor.template;

import components.Component;
import editor.EditorWidget;

import java.util.Objects;

/**
 * Template for {@link Component}'s editor UI.
 */
final class ComponentTemplate implements IComponentTemplate<Component> {
    private static final ComponentTemplate instance = new ComponentTemplate();
    private ComponentTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(Component component) {
        String result = EditorWidget.inputText("Name", component.name(), component);
        if (!Objects.equals(component.name(), result)) component.name(result);
        ComponentTemplateHierarchy.render(component);
    }

    /**
     * Render the content of {@link #editorUI(Component)}.
     * @param component the context component
     */
    static void render(Component component) {
        if (component == null) return;
        instance.editorUI(component);
    }
}
