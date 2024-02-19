package components;

import TCB_Field.KeyListener;
import TCB_Field.MouseListener;
import TCB_Field.Viewport;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

public class WorkViewport extends Component {
    private boolean isBackTo0 = false;
    private boolean isResetZ = false;
    private float lerpT = 0.0f;
    private float dragInit = 0.032f;
    private float dragSensitivity = 24.0f;
    private float scrollSensitivity = 0.1f;
    private Viewport workViewport;
    private Vector2f clickOrigin;
    private float MAX_ZOOM = 4.0f;
    private float MIN_ZOOM = 0.5f;
    public WorkViewport(Viewport workViewport) {
        this.workViewport = workViewport;
        this.clickOrigin = new Vector2f();
    }

    @Override
    public void updateEditor(float dt) {
        if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE) && dragInit > 0) {
            this.clickOrigin = MouseListener.getWorld();
            dragInit -= dt;
            return;

        } else if (MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE)) {
            Vector2f cursorPos = MouseListener.getWorld();
            Vector2f delta = new Vector2f(cursorPos).sub(this.clickOrigin);
            workViewport.position.sub(delta.mul(dt).mul(dragSensitivity));
            this.clickOrigin.lerp(cursorPos, dt);
        }

        if (dragInit <= 0.0f && !MouseListener.mouseButtonDown(GLFW_MOUSE_BUTTON_MIDDLE)) {
            dragInit = 0.032f;
        }

        if (MouseListener.getScrollY() != 0.0f) {
            float addVal = (float)Math.pow(Math.abs(MouseListener.getScrollY()) * scrollSensitivity,
                    1 / workViewport.loadZoom()
            );
            addVal *= -Math.signum(MouseListener.getScrollY());
            if (workViewport.loadZoom() + addVal <= MAX_ZOOM && workViewport.loadZoom() + addVal >= MIN_ZOOM) {
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
            workViewport.setZoom(this.workViewport.loadZoom() + (1.0f - workViewport.loadZoom()) * lerpT);

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
