package components;

import TheCellBeyond.Window;

/**
 * A class dedicated to handling Editor's gizmo system.
 * Handle gizmo's type and keybindings.
 */
public class GizmoControl extends Component {
    private final SpriteSheet gizmo;
    private static int isGizUse = 0;

    public GizmoControl(SpriteSheet gizmoSprite) {
        gizmo = gizmoSprite;
    }

    @Override
    public void start() {
        gameObject.addComponent(new GizmoMove(gizmo.spriteIndex(1), Window.getImGuiLayer().loadProperties()));
        gameObject.addComponent(new GizmoScale(gizmo.spriteIndex(2), Window.getImGuiLayer().loadProperties()));
    }

    public static int getIsGizUse() {
        return isGizUse;
    }

    public static void setIsGizUse(int val) {
        isGizUse = val;
    }

    @Override
    public void editorUpdate(float dt) {
        if (isGizUse == 0) {
            gameObject.getComponent(GizmoMove.class).setUse();

            gameObject.getComponent(GizmoScale.class).setUnUse();

        } else if (isGizUse == 1) {
            gameObject.getComponent(GizmoMove.class).setUnUse();

            gameObject.getComponent(GizmoScale.class).setUse();
        }
    }
}
