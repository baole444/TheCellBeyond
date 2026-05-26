package editor.components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.MouseListener;
import org.joml.Vector2f;
import render.texture.Sprite;

/**
 * EditorGizmo used for moving an object around in the editor.
 */
public class EditorGizmoMove extends EditorGizmo {
    /**
     * Create a new {@link EditorGizmoMove} component with the given arrow sprite.
     * @param arrowSprite the arrow sprite for the new component
     */
    public EditorGizmoMove(Sprite arrowSprite) {
        super("translate_", arrowSprite);
    }

    @Override
    protected void internalEditorUpdate(float dt) {
        if (!(activeGameObj instanceof GameObject2D go2D)) {
            super.internalEditorUpdate(dt);
            return;
        }
        Vector2f cursorT = MouseListener.getCursorWorldTraverse();
        if (xActiveDrag && !yActiveDrag) go2D.translate(new Vector2f(- cursorT.x, 0));
        else if (yActiveDrag) go2D.translate(new Vector2f(0, - cursorT.y));
        super.internalEditorUpdate(dt);
    }
}
