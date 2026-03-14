package editor.template;

import TheCellBeyond.Camera2D;
import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import components.Component;
import components.RemoteTransform2D;
import components.SpriteRenderer;

/**
 * Editor UI rendering for supported types of Component or GameObject.
 */
public final class EditorTemplate {
    private EditorTemplate() {}

    /**
     * For component and its subclasses.
     * @param component context
     * @apiNote To maintainer:<br>
     * The cases are inheritance sensitive. Therefore, classes that is higher up in the inheritance,
     * should be placed after its subclasses' cases.
     */
    public static void render(Component component) {
        if (component == null) return;
        switch (component) {
            case RemoteTransform2D remoteTransform2D -> RemoteTransform2DTemplate.render(remoteTransform2D);
            case SpriteRenderer spriteRenderer -> SpriteRendererTemplate.render(spriteRenderer);
            case Transform2D transform2D -> Transform2DTemplate.render(transform2D);
            default -> {}
        }
    }

    /**
     * For game object and its subclasses.
     * @param object context
     * @apiNote To maintainer:<br>
     * The cases are inheritance sensitive. Therefore, classes that is higher up in the inheritance,
     * should be placed after its subclasses' cases.
     */
    public static void render(GameObject object) {
        if (object == null) return;
        switch (object) {
            case Camera2D camera2D -> Camera2DTemplate.render(camera2D);
            case GameObject2D go2D -> GameObject2DTemplate.render(go2D);
            case GameObject go -> GameObjectTemplate.render(go);
        }
    }
}
