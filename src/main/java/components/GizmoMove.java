package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import editor.Properties;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * Gizmo used for moving an object around in the editor.
 *
 * <p>Extends {@link Gizmo} to provide functionality for translating
 * {@link GameObject2D} objects along the X or Y axis using mouse input.</p>
 *
 * @see Gizmo
 * @see GizmoControl
 */
public class GizmoMove extends Gizmo {
    // Creates a move gizmo with the given arrow sprite.
    public GizmoMove(Sprite arrowSprite) {
        super("translate_", arrowSprite);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null && activeGameObj instanceof GameObject2D go2D) {
            Vector2f cursorT = MouseListener.getCursorTraverse();

            if (xActiveDrag && !yActiveDrag) {
                go2D.translate(new Vector2f(- cursorT.x, 0));
            } else if (yActiveDrag) {
                go2D.translate(new Vector2f(0, - cursorT.y));
            }
        }

        super.editorUpdate(dt);
    }
}
