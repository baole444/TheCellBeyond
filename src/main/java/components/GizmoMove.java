package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import editor.Properties;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * Gizmo used for moving an object around in the editor.
 */
public class GizmoMove extends Gizmo {
    public GizmoMove(Sprite arrowSprite, Properties properties) {
        super(arrowSprite, properties);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null && activeGameObj instanceof GameObject2D go2D) {
            Vector2f cursorT = MouseListener.getCursorTraverse();

            if (xActiveDrag && !yActiveDrag) {
                Vector2f current = go2D.getGlobalPosition();
                go2D.setGlobalPosition(new Vector2f(current.x - cursorT.x, current.y));
            } else if (yActiveDrag) {
                Vector2f current = go2D.getGlobalPosition();
                go2D.setGlobalPosition(new Vector2f(current.x, current.y  - cursorT.y));
            }
        }

        super.editorUpdate(dt);
    }
}
