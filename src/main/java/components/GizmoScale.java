package components;

import TCB_Field.MouseListener;
import editor.Properties;

public class GizmoScale extends Gizmo{
    public GizmoScale(Sprite boxSprite, Properties properties) {
        super(boxSprite, properties);
    }

    @Override
    public void updateEditor(float dt) {
        if (activeGameObj != null) {
            if (xActiveDrag && !yActiveDrag) {
                activeGameObj.transform.scale.x -= MouseListener.getWorldDX();
            } else if (yActiveDrag) {
                activeGameObj.transform.scale.y -= MouseListener.getWorldDY();
            }
        }

        super.updateEditor(dt);
    }
}
