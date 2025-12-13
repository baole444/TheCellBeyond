package components;

import TheCellBeyond.GameObject;
import editor.ImEditorGui;
import imgui.ImGui;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import java.util.UUID;

/**
 * Component is an extension for {@link GameObject}.
 * <p>
 * It is the leaf of the scene hierarchy, only affected by the immediate object it belongs to.
 * </p>
 * Component can reference each other by name or by hierarchy path.
 */
public abstract class Component {
    private String uuid;

    /**
     * The owning {@link GameObject} of this component.
     */
    public transient GameObject gameObject;

    private String componentName;

    public Component() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Initialize the component's state when its {@link GameObject} started.
     * <p>
     * Called by the owning game object {@link GameObject#start()} logic.
     * Override this to fully customize the start logic of this component.
     * @see Component#additionalStartLogic() Add additional component startup logic
     */
    public void start() {
        additionalStartLogic();
    }

    /**
     * Override this to add additional logic to the startup logic of a component.
     */
    protected void additionalStartLogic() {}

    /**
     * Override this to fully customize the update logic of a component while in editor mode.
     * @param dt delta time.
     * @see Component#additionalUpdateLogic(float) Add additional update logic
     */
    public void editorUpdate(float dt) {
        additionalUpdateLogic(dt);
    }

    /**
     * Override this to fully customize the update logic of a component while in runtime(product) mode.
     * @param dt delta time.
     * @see Component#additionalUpdateLogic(float) Add additional update logic
     */
    public void update(float dt) {
        additionalUpdateLogic(dt);
    }

    /**
     * Override this to add additional logic to the update logic of a component for both editor and runtime mode.
     * @param dt delta time.
     */
    protected void additionalUpdateLogic(float dt) {}

    public void startCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void endCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void preSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void postSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Upon calling destroy, the component will discard its uuid, game object reference, and its name.
     */
    public final void destroy() {
        additionalDestroyLogic();
        uuid = null;
        gameObject = null;
        componentName = null;
    }

    /**
     * Optional hook for additional destroy (clean up) logic of a component.
     * <p>
     * This is useful for when there are external states that need to be aware of this component's destruction.
     */
    protected void additionalDestroyLogic() {}

    public <T extends Component> T getSibling(Class<T> componentClass) {
        if (gameObject == null) return null;
        return gameObject.getFirstComponent(componentClass);
    }

    public <T extends Component> T getFromParent(Class<T> componentClass) {
        if (gameObject == null || gameObject.getParent() == null) return null;
        return gameObject.getParent().getFirstComponent(componentClass);
    }

    public <T extends Component> T getFromChild(String childName, Class<T> componentClass) {
        if (gameObject == null) return null;
        GameObject child = gameObject.getChild(childName);
        if (child == null) return null;
        return child.getFirstComponent(componentClass);
    }

    public Component findComponentByName(String name) {
        if (gameObject == null) return null;
        GameObject root = gameObject.getRoot();
        return root.findComponentByName(name);
    }

    public Component getComponentByPath(String path) {
        if (gameObject == null || path == null) return null;
        return gameObject.resolveHierarchyPathAsComponent(path);
    }

    /**
     * Override this to fully customize the exported field(s) in the properties window.
     * @see Component#additionalImGuiLogic() Add additional field export logic
     */
    public void imgui() {
        if (componentName == null) {
            if (ImGui.button("Set component's name")) componentName = this.getClass().getSimpleName();
        } else {
            String name = ImEditorGui.inputText("Name", componentName, this);
            if (!name.equals(componentName)) setComponentName(name);
        }

        additionalImGuiLogic();
    }

    /**
     * Override this to add additional field(s) exported to the properties window.
     */
    protected void additionalImGuiLogic() {}

    public String getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public String getComponentName() {
        return componentName;
    }

    /**
     * Set a custom name for this component.<br>
     * If the name is {@code null} or blank, this remove the custom name of the component instead.
     * @param name the name to update with, nullable
     */
    public void setComponentName(String name) {
        if (name != null && name.isBlank()) name = null;
        componentName = name;

        if (gameObject != null) gameObject.onComponentNameChanged(this, name);

    }
}