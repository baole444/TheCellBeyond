package editor.components;

import TheCellBeyond.KeyListener;
import TheCellBeyond.MouseListener;
import TheCellBeyond.Viewport;
import components.Component;
import editor.ImGuiLayer;
import imgui.ImGui;
import imgui.flag.ImGuiPopupFlags;
import org.joml.Vector2f;

import static org.lwjgl.glfw.GLFW.*;

/**
 * Handler for controls while editing scene in Editor UI.
 */
public final class EditorSceneCtrl extends Component {
    private boolean isBackTo0 = false;
    private boolean isResetZ = false;
    private float dragInit = 0.032f;
    private static final float pixelDragSensitivity = 24.0f; // in pixel
    private static final float scrollSensitivity = 0.1f; // zoom step
    private final Viewport workingViewport;
    private final Vector2f clickOrigin = new Vector2f();
    private static final float MaxZoom = 10.0f;
    private static final float MinZoom = 0.01f;

    /**
     * Create a new handler with the given viewport.
     * @param targetViewport the scene's global viewport
     */
    public EditorSceneCtrl(Viewport targetViewport) {
        workingViewport = targetViewport;
    }

    @Override
    public void editorUpdate(float dt) {
        if (!ImGuiLayer.editorWantCaptureMouse() || ImGuiLayer.editorWantCaptureKeyboard() || ImGui.isPopupOpen("", ImGuiPopupFlags.AnyPopup)) return;
        if (MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE) && dragInit > 0) {
            clickOrigin.set(MouseListener.getWorldPosition());
            dragInit -= dt;
            return;
        }
        if (MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE)) {
            Vector2f cursorPos = MouseListener.getWorldPosition();
            Vector2f delta = new Vector2f(cursorPos).sub(this.clickOrigin);
            workingViewport.position.sub(delta.mul(dt).mul(pixelDragSensitivity));
            this.clickOrigin.lerp(cursorPos, dt);
        }
        if (dragInit <= 0.0f && !MouseListener.isButtonPressed(GLFW_MOUSE_BUTTON_MIDDLE)) dragInit = 0.032f;
        handleZoom();
        if (KeyListener.isKeyPressed(GLFW_KEY_Z)) isResetZ = true;
        if (isResetZ) {
            workingViewport.setZoom(1.0f);
            isResetZ= false;
        }
        if (KeyListener.isKeyPressed(GLFW_KEY_HOME)) isBackTo0 = true;
        if (isBackTo0) {
            workingViewport.position.set(0.0f);
            workingViewport.setZoom(1.0f);
            isBackTo0 = false;
        }
    }

    private void handleZoom() {
        float scroll = MouseListener.getScrollY();
        if (Math.abs(scroll) == 0.0f) return;
        Vector2f currentZoom = new Vector2f(workingViewport.getZoom());
        float zoomStep = Math.abs(scroll) * scrollSensitivity * -Math.signum(scroll);
        float addValue;
        if (zoomStep < 0.0f) {
            float min = currentZoom.get(currentZoom.minComponent());
            addValue = -Math.abs(Math.min(Math.abs(MinZoom - min), Math.abs(zoomStep)));
        } else {
            float max = currentZoom.get(currentZoom.maxComponent());
            addValue = Math.min(MaxZoom - max, zoomStep);
        }
        workingViewport.addZoom(addValue);
    }
}
