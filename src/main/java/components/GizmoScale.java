package components;

import TheCellBeyond.MouseListener;
import editor.Properties;
import render.texture.Sprite;

/**
 * Gizmo used for scaling an object in the editor
 */
public class GizmoScale extends Gizmo{
    public GizmoScale(Sprite boxSprite, Properties properties) {
        super(boxSprite, properties);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null) {
            if (xActiveDrag && !yActiveDrag) {
                activeGameObj.transform.scale.x -= MouseListener.getWorldX();
            } else if (yActiveDrag) {
                activeGameObj.transform.scale.y -= MouseListener.getWorldY();
            }
        }

        super.editorUpdate(dt);
    }
}
