package editor.template;

import components.Component;
import components.RemoteTransform2D;
import components.SpriteRenderer;

/**
 * Editor UI rendering for supported types of Component or GameObject
 */
public class EditorTemplate {
    public static void render(Component component) {
        if (component == null) return;
        switch (component) {
            case RemoteTransform2D remoteTransform2D -> RemoteTransform2DTemplate.render(remoteTransform2D);
            case SpriteRenderer spriteRenderer -> SpriteRendererTemplate.render(spriteRenderer);
            default -> {}
        }
    }
}
