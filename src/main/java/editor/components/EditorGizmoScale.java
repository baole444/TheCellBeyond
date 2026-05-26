package editor.components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * EditorGizmo used for scaling an object in the editor.
 */
public class EditorGizmoScale extends EditorGizmo {
    /**
     * Create a new {@link EditorGizmoScale} component with the given scale sprite.
     * @param boxSprite the scale sprite for the new component
     */
    public EditorGizmoScale(Sprite boxSprite) {
        super("scale_", boxSprite);
    }

    @Override
    protected void internalEditorUpdate(float dt) {
        if (!(activeGameObj instanceof GameObject2D go2D)) {
            super.internalEditorUpdate(dt);
            return;
        }
        Vector2f cursorT = MouseListener.getCursorWorldTraverse();
        Vector2f currentScale = go2D.scale();
        float scaleStep = 0.1f;
        if (xActiveDrag && !yActiveDrag) {
            float scaleX = currentScale.x - (cursorT.x * scaleStep);
            scaleX = Math.max(scaleX, 0.01f);
            go2D.scale(new Vector2f(scaleX, currentScale.y));
        } else if (yActiveDrag) {
            float scaleY = currentScale.y - (cursorT.y * scaleStep);
            go2D.scale(new Vector2f(currentScale.x, scaleY));
        }
        super.internalEditorUpdate(dt);
    }
}
