package editor;

import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Viewport;
import components.Component;
import org.joml.Math;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class EditorSceneCtrl extends Component {
    private boolean isBackTo0 = false;
    private boolean isResetZ = false;
    private float lerpT = 0.0f;
    private float dragInit = 0.032f;
    private final float dragSensitivity = 24.0f; // in pixel
    private final float scrollSensitivity = 0.1f; // zoom step
    private final Viewport workViewport;
    private Vector2f clickOrigin;
    private final float MAX_ZOOM = 10.0f;
    private final float MIN_ZOOM = 0.01f;

    public EditorSceneCtrl(Viewport workViewport) {
        this.workViewport = workViewport;
        this.clickOrigin = new Vector2f();
    }

    @Override
    public void editorUpdate(float dt) {
        if (MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE) && dragInit > 0) {
            this.clickOrigin = MouseListener.getWorldPosition();
            dragInit -= dt;
            return;

        } else if (MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE)) {
            Vector2f cursorPos = MouseListener.getWorldPosition();
            Vector2f delta = new Vector2f(cursorPos).sub(this.clickOrigin);
            workViewport.position.sub(delta.mul(dt).mul(dragSensitivity));
            this.clickOrigin.lerp(cursorPos, dt);

        }

        if (dragInit <= 0.0f && !MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE)) {
            dragInit = 0.032f;
        }

        if (MouseListener.getScrollY() != 0.0f) {
            float addVal = (float) java.lang.Math.pow(Math.abs(MouseListener.getScrollY()) * scrollSensitivity,
                    1 / workViewport.getZoom()
            );
            addVal *= -Math.signum(MouseListener.getScrollY());
            if (workViewport.getZoom() + addVal <= MAX_ZOOM && workViewport.getZoom() + addVal >= MIN_ZOOM) {
                workViewport.addZoom(addVal);
            }
        }

        if (KeyListener.isKeyPressed(GLFW_KEY_Z)) {
            isResetZ = true;
        }

        if (isResetZ) {
                this.workViewport.setZoom(1.0f);
                isResetZ= false;
        }

        if (KeyListener.isKeyPressed(GLFW_KEY_HOME)) {
            isBackTo0 = true;
        }

        if (isBackTo0) {
            workViewport.position.lerp(new Vector2f(0, 0), lerpT);

            // Lerp function for the zoom
            workViewport.setZoom(this.workViewport.getZoom() + (1.0f - workViewport.getZoom()) * lerpT);

            // Unity fix on lerp to origin
            this.lerpT += 0.1f + dt;

            if (Math.abs(workViewport.position.x) <= 5.0f &&
                    Math.abs(workViewport.position.y) <= 5.0f
            ) {
                this.lerpT = 0.0f;
                workViewport.position.set(0f, 0f);
                this.workViewport.setZoom(1.0f);
                isBackTo0 = false;
            }
        }
    }

}
