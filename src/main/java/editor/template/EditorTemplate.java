package editor.template;

import TheCellBeyond.GameObject;
import components.Component;
import components.NotSerializeComponent;

/**
 * Editor UI rendering for supported types of Component or GameObject.
 */
public final class EditorTemplate {
    private EditorTemplate() {}

    /**
     * For component and its subclasses.
     * @param component context
     */
    public static void render(Component component) {
        if (component == null || component.isDestroyed() || component instanceof NotSerializeComponent) return;
        ComponentTemplate.render(component);
    }

    /**
     * For game object and its subclasses.
     * @param object context
     */
    public static void render(GameObject object) {
        if (object == null || object.isDestroyed()) return;
        GameObjectTemplate.render(object);
    }
}
