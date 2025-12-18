package editor.components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * EditorGizmo used for scaling an object in the editor.
 */
public class EditorGizmoScale extends EditorGizmo {
    public EditorGizmoScale(Sprite boxSprite) {
        super("scale_", boxSprite);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null && activeGameObj instanceof GameObject2D go2D) {
            Vector2f cursorT = MouseListener.getCursorWorldTraverse();
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
