package components;

import TCB_Field.KeyListener;
import TCB_Field.Window;

import static org.lwjgl.glfw.GLFW.*;

public class GizmoControl extends Component {
    private SpriteSheet gizmo;
    private int isGizUse = 0;

    public GizmoControl(SpriteSheet gizmoSprite) {
        gizmo = gizmoSprite;
    }

    @Override
    public void start() {
        gameObject.addComponent(new GizmoMove(gizmo.spriteIndex(1), Window.loadImGui().loadProperties()));
        gameObject.addComponent(new GizmoScale(gizmo.spriteIndex(2), Window.loadImGui().loadProperties()));

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


        // Make keybind of Shift + S = scale | Shift + T = translate
        if (KeyListener.isKeyPressed(GLFW_KEY_T, GLFW_MOD_SHIFT)) {
            isGizUse = 0;
            //System.out.println("Using move gizmo");
        } else if (KeyListener.isKeyPressed(GLFW_KEY_S, GLFW_MOD_SHIFT)) {
            isGizUse = 1;
            //System.out.println("using scale gizmo");
        }

        //if (KeyListener.isKeyPressed(GLFW_KEY_T)) {
        //    isGizUse = 0;
        //} else if (KeyListener.isKeyPressed(GLFW_KEY_S)) {
        //    isGizUse = 1;
        //}

    }
}
