package components;

import TCB_Field.MouseListener;
import editor.Properties;

public class GizmoMove extends Gizmo {
    public GizmoMove(Sprite arrowSprite, Properties properties) {
        super(arrowSprite, properties);
    }

    @Override
    public void updateEditor(float dt) {
        if (activeGameObj != null) {
            if (xActiveDrag && !yActiveDrag) {
                activeGameObj.transform.position.x -= MouseListener.getWorldDX();
            } else if (yActiveDrag) {
                activeGameObj.transform.position.y -= MouseListener.getWorldDY();
            }
        }

        super.updateEditor(dt);
    }
}
