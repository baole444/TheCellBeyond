package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Input;
import TheCellBeyond.InputAction;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import physic2d.PhysicBody2D;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Controller2D component allows applying movement velocity to the game object it is mounted to (if supported).
 * <p>
 * The movement is applied base on {@link ControllerDirection}s and their bound {@link InputAction}s.
 * @see ControlMode Controller2D's control modes
 */
public final class Controller2D extends Component {

    /**
     * ControlMode define how the controller apply movement.
     * <p>
     * There are 2 modes:
     * <ul>
     *     <li>{@link ControlMode#SpatialLogic}</li>
     *     <li>{@link ControlMode#PhysicalLogic}</li>
     * </ul>
     */
    public enum ControlMode {

        /**
         * The game object cannot be moved
         */
        Incompatible,

        /**
         * Move the game object using spatial transformation logic.
         * Movement velocity is scaled by delta time.
         * <p>
         * Require the game object of the controller to be of type {@link GameObject2D} or its subclasses.
         */
        SpatialLogic,

        /**
         * Move the game object using physics logic.
         * Movement velocity is added to the physic body without delta time scaling.
         * <p>
         * Require the game object of the controller to be of type {@link PhysicBody2D} or its subclasses.
         */
        PhysicalLogic
    }

    /**
     * Movement speed to apply to the direction vector, in world unit.
     */
    public float movementSpeed = 0.0f;

    /**
     * Is the controller only accept the first active action in a frame.
     */
    public boolean oneActionPerFrame = true;

    /**
     * Is the diagonal movement speed applied by this controller normalized.
     */
    public boolean normalizeDiagonalSpeed = true;

    private final ConcurrentHashMap<String, ControllerBinding> controllerBindings = new ConcurrentHashMap<>();

    /**
     * The control mode of this controller.
     */
    private ControlMode controlMode = ControlMode.PhysicalLogic;

    private transient GameObject2D gameObject2D = null;
    private transient PhysicBody2D physicBody2D = null;
    private transient boolean isGameObjectSpatialCompatible = false;
    private transient boolean isGameObjectPhysicCompatible = false;


    @Override
    protected void onStart() {
        if (gameObject == null) return;
        if (gameObject instanceof GameObject2D go2D) {
            gameObject2D = go2D;
            isGameObjectSpatialCompatible = true;
        }
        if (gameObject instanceof PhysicBody2D body2D) {
            physicBody2D = body2D;
            isGameObjectPhysicCompatible = true;
        }

        if (!isGameObjectSpatialCompatible && !isGameObjectPhysicCompatible) {
            controlMode = ControlMode.Incompatible;
            LOGGER.warning(String.format("Mounting %s to unsupported '%s' object", this.getClass().getSimpleName(), gameObject.name()));
            return;
        }

        if (!isGameObjectPhysicCompatible) controlMode = ControlMode.SpatialLogic;
    }

    @Override
    protected void onEditorStart() {
        onStart();
    }

    @Override
    protected void onDestroy() {
        gameObject2D = null;
        physicBody2D = null;
    }

    @Override
    public void update(float dt) {
        if (controlMode == ControlMode.Incompatible) return;

        Vector2f finalDirection = getCombinedDirection(dt);
        if (finalDirection.lengthSquared() == 0.0f) return;
        if (normalizeDiagonalSpeed) finalDirection.normalize();
        finalDirection.mul(movementSpeed);

        if (controlMode == ControlMode.PhysicalLogic) {
            applyPhysicalMovement(finalDirection);
            return;
        }

        if (controlMode == ControlMode.SpatialLogic) applySpatialMovement(finalDirection, dt);
    }

    public ControlMode controlMode() {
        return controlMode;
    }

    public void controlMode(ControlMode mode) {
        if (mode == null || mode == ControlMode.Incompatible) return;
        if (!isGameObjectPhysicCompatible && !isGameObjectSpatialCompatible) {
            controlMode = ControlMode.Incompatible;
            return;
        }

        if (!isGameObjectSpatialCompatible) return;

        if (!isGameObjectPhysicCompatible) {
            controlMode = ControlMode.SpatialLogic;
            return;
        }

        controlMode = mode;
    }

    public void newBinding() {
        String newName = "binding";

        if (controllerBindings.isEmpty()) {
            controllerBindings.put(newName, new ControllerBinding());
            return;
        }

        String uniqueName = newName;
        int i = controllerBindings.size();
        if (controllerBindings.containsKey(uniqueName)) {
            uniqueName = newName + "_" + i;
            i++;
        }

        controllerBindings.put(uniqueName, new ControllerBinding());
    }

    public boolean renameBinding(String oldName, String newName) {
        if (controllerBindings.isEmpty()) return false;
        if (oldName == null || newName == null || oldName.isBlank() || newName.isBlank()) return false;
        newName = newName.trim();
        if (!controllerBindings.containsKey(oldName) || controllerBindings.containsKey(newName)) return false;

        ControllerBinding binding = controllerBindings.remove(oldName);
        if (binding == null) return false;

        controllerBindings.put(newName, binding);
        return true;
    }

    public void duplicateBinding(String name) {
        if (name == null || name.isBlank()) return;
        if (controllerBindings.isEmpty() || !controllerBindings.containsKey(name)) return;

        ControllerBinding binding = controllerBindings.get(name);
        if (binding == null) return;

        String newName = name + "_copy";
        int i = 1;
        while (controllerBindings.containsKey(newName)) {
            newName = name + "_copy" + i;
            i++;
        }

        controllerBindings.put(newName, new ControllerBinding(binding));
    }

    public void bindAction(String bindingName, String actionName) {
        if (bindingName == null || actionName == null) return;

        ControllerBinding binding = controllerBindings.get(bindingName);
        if (binding == null) return;

        if (Input.getInputAction(actionName) == null) {
            LOGGER.warning(String.format("Input action '%s' does not exist", actionName));
            return;
        }

        binding.boundActionNames.add(actionName);
    }

    public ControllerBinding getBinding(String bindingName) {
        if (bindingName == null || bindingName.isBlank()) return null;
        return controllerBindings.get(bindingName);
    }

    public boolean removeBinding(String bindingName) {
        if (bindingName == null) return false;
        return controllerBindings.remove(bindingName) != null;
    }

    public void unbindAction(String bindingName, String actionName) {
        if (bindingName == null || bindingName.isBlank() || actionName == null) return;

        ControllerBinding binding = controllerBindings.get(bindingName);
        if (binding == null) return;

        binding.boundActionNames.remove(actionName);
    }

    public void clearBindingAction(String bindingName) {
        if (bindingName == null || bindingName.isBlank()) return;

        ControllerBinding binding = controllerBindings.get(bindingName);
        if (binding == null) return;

        binding.boundActionNames.clear();
    }

    public HashMap<String, ControllerBinding> getBindings() {
        return new HashMap<>(controllerBindings);
    }

    private Vector2f getCombinedDirection(float dt) {
        Vector2f combinedDirection = new Vector2f();
        if (controllerBindings.isEmpty()) return combinedDirection;

        for (Map.Entry<String, ControllerBinding> entry : controllerBindings.entrySet()) {
            ControllerBinding binding = entry.getValue();

            boolean isPressed = false;
            for (String name : binding.boundActionNames) {
                if (!Input.isActionPresses(name)) continue;
                isPressed = true;
                break;
            }

            if (!binding.isActive(isPressed, dt)) continue;
            combinedDirection.add(binding.directionVector());

            if (oneActionPerFrame) break;
        }
        return combinedDirection;
    }

    /**
     * Apply movement by update this component's {@link GameObject2D}'s position.
     * The movement is scaled by delta time when moving this way.
     * @param movementVelocity the movement velocity of movement direction and speed
     * @param dt the delta time of this frame
     */
    private void applySpatialMovement(Vector2f movementVelocity, float dt) {
        if (!isGameObjectSpatialCompatible || gameObject2D == null) return;

        Vector2f displacement = new Vector2f(movementVelocity).mul(dt);
        Vector2f currentPos = gameObject2D.globalPosition();
        gameObject2D.position(currentPos.x + displacement.x, currentPos.y + displacement.y);
    }

    /**
     * Apply movement by adding velocity to this component's {@link PhysicBody2D}.
     * The movement is not scaled by delta time when moving this way.
     * @param movementVelocity the movement velocity of movement direction and speed
     */
    private void applyPhysicalMovement(Vector2f movementVelocity) {
        if (!isGameObjectPhysicCompatible || physicBody2D == null) return;
        physicBody2D.addMovement(movementVelocity);
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openController = ImGui.collapsingHeader("Controller2D##Controler2D_Properties_Header", ImGuiTreeNodeFlags.DefaultOpen);
        if (!openController) return;

        ImGui.indent();
        ControlMode current = controlMode;
        ImGui.text("Control Mode:");
        if (ImGui.beginCombo("##Select_Controller_Control_Mode_Combo_" + getUUID(), current.name())) {
            if (!isGameObjectSpatialCompatible && !isGameObjectPhysicCompatible) {
                ImGui.textWrapped(gameObject.name() + " is not compatible with this controller");
            } else {
                for (ControlMode mode : ControlMode.values()) {
                    if (mode == ControlMode.Incompatible) continue;
                    String label = mode.name() + "##Select_" + mode.name() + "_ControlMode_Selectable_" + getUUID();
                    if (ImGui.selectable(label, mode == controlMode)) controlMode(mode);
                }
            }
            ImGui.endCombo();
        }

        ImGui.spacing();
        float speed = ImEditorGui.dragFloatCtrl("Movement Speed", movementSpeed, 0.0f, 0.1f, this);
        if (Float.compare(speed, movementSpeed) != 0) movementSpeed = speed;

        ImBoolean oneAction = new ImBoolean(oneActionPerFrame);
        ImBoolean normalizeDiagonal = new ImBoolean(normalizeDiagonalSpeed);
        if (ImGui.checkbox("Single Action##AllowOneActionPerFrame_" + getUUID(), oneAction)) oneActionPerFrame = oneAction.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("If enabled, one 1 action/binding will be process each frame.");
            ImGui.textWrapped("For example, binding \"Move Up\" and \"Move Down\" are both press in the same frame." +
                    " If only 1 action is allowed, the controller will process whatever binding is active first.");
            ImGui.endTooltip();
        }
        if (ImGui.checkbox("Normalize Diagonal Movement##NormalizeDiagonalMovement_" + getUUID(), normalizeDiagonal)) normalizeDiagonalSpeed = normalizeDiagonal.get();
        if (ImGui.isItemHovered()) {
            ImGui.beginTooltip();
            ImGui.text("If enabled, diagonal movement will be normalized.");
            ImGui.textWrapped("This prevent faster movement when moving diagonally due to vector combination.");
            ImGui.endTooltip();
        }
        ImGui.unindent();
    }
}