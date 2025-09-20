package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import editor.Properties;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * Gizmo used for scaling an object in the editor
 *
 * <p>Extends {@link Gizmo} to provide functionality for scaling
 * {@link GameObject2D} objects along the X or Y axis using mouse input.</p>
 *
 * @see Gizmo
 * @see GizmoControl
 */
public class GizmoScale extends Gizmo{
     /**
     * Creates a scale gizmo with the given box sprite.
     *
     * @param boxSprite sprite used for rendering scale handles
     */
    public GizmoScale(Sprite boxSprite) {
        super("scale_", boxSprite);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null && activeGameObj instanceof GameObject2D go2D) {
            Vector2f cursorT = MouseListener.getCursorTraverse();
            Vector2f currentScale = go2D.getOffsetScale();

            float scaleStep = 0.1f;
            if (xActiveDrag && !yActiveDrag) {
                float scaleX = currentScale.x - (cursorT.x * scaleStep);
                scaleX = Math.max(scaleX, 0.01f);

                go2D.setScale(new Vector2f(scaleX, currentScale.y));
            } else if (yActiveDrag) {
                float scaleY = currentScale.y - (cursorT.y * scaleStep);
                go2D.setScale(new Vector2f(currentScale.x, scaleY));
            }
        }

        super.editorUpdate(dt);
    }
}
