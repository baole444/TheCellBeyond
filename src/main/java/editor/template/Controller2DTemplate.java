package editor.template;

import components.Controller2D;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;

import java.util.UUID;

/**
 * Template for {@link Controller2D}'s editor UI.
 */
final class Controller2DTemplate implements IComponentTemplate<Controller2D> {
    private static final Controller2DTemplate instance = new Controller2DTemplate();
    private Controller2DTemplate() {}
    /**
     * Execute the rendering code for the Editor UI, related to this component.
     * This method is passive, and must be call to render the UI.
     *
     * @param component the context component
     */
    @Override
    public void editorUI(Controller2D component) {
        ImGui.spacing();
        UUID uuid = component.getUUID();
        boolean openController = ImGui.collapsingHeader("Controller2D##Controler2D_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openController) return;
        Controller2D.ControlMode current = component.controlMode();
        ImGui.text("Control Mode:");
        if (ImGui.beginCombo("##Select_Controller_Control_Mode_Combo_" + uuid, current.name())) {
            if (!component.isGameObjectSpatialCompatible() && !component.isGameObjectPhysicCompatible()) {
                String name = component.gameObject == null ? "Unknown object" : component.gameObject.name();
                ImGui.textWrapped(name  + " is not compatible with this controller");
            } else {
                for (Controller2D.ControlMode mode : Controller2D.ControlMode.values()) {
                    if (mode == Controller2D.ControlMode.Incompatible) continue;
                    String label = mode.name() + "##Select_" + mode.name() + "_ControlMode_Selectable_" + uuid;
                    if (ImGui.selectable(label, mode == component.controlMode())) component.controlMode(mode);
                }
            }
            ImGui.endCombo();
        }
        ImGui.spacing();
        float speed = EditorWidget.dragFloatCtrl("Movement Speed", component.movementSpeed, 0.0f, 0.1f, component);
        if (Float.compare(speed, component.movementSpeed) != 0) component.movementSpeed = speed;
        ImBoolean oneAction = new ImBoolean(component.oneActionPerFrame);
        ImBoolean normalizeDiagonal = new ImBoolean(component.normalizeDiagonalSpeed);
        if (ImGui.checkbox("Single Action##AllowOneActionPerFrame_" + uuid, oneAction)) component.oneActionPerFrame = oneAction.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("If enabled, one 1 action/binding will be process each frame.");
            ImGui.textWrapped("For example, binding \"Move Up\" and \"Move Down\" are both press in the same frame." +
                    " If only 1 action is allowed, the controller will process whatever binding is active first.");
            ImGui.endTooltip();
        }
        if (ImGui.checkbox("Normalize Diagonal Movement##NormalizeDiagonalMovement_" + uuid, normalizeDiagonal)) component.normalizeDiagonalSpeed = normalizeDiagonal.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("If enabled, diagonal movement will be normalized.");
            ImGui.textWrapped("This prevent faster movement when moving diagonally due to vector combination.");
            ImGui.endTooltip();
        }
    }

    /**
     * Render the content of {@link #editorUI(Controller2D)}.
     * @param controller2D the context component
     */
    static void render(Controller2D controller2D) {
        if (controller2D == null) return;
        instance.editorUI(controller2D);
    }
}
