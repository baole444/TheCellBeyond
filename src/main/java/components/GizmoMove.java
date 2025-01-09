package components;

import TCB_Field.MouseListener;
import editor.Properties;

/**
 * Gizmo used for moving an object around in the editor.
 */
public class GizmoMove extends Gizmo {
    public GizmoMove(Sprite arrowSprite, Properties properties) {
        super(arrowSprite, properties);
    }

    @Override
    public void editorUpdate(float dt) {
        if (activeGameObj != null) {
            if (xActiveDrag && !yActiveDrag) {
                activeGameObj.transform.position.x -= MouseListener.getCursorTraverse().x;
                //System.out.println("Requested move on X axis");
            } else if (yActiveDrag) {
                activeGameObj.transform.position.y -= MouseListener.getCursorTraverse().y;
                //System.out.println("Requested move on Y axis");
            }
        }

        super.editorUpdate(dt);
    }
}
