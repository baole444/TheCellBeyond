package components;

import TheCellBeyond.GameObject;
import editor.ImEditorGui;
import imgui.ImGui;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import java.util.UUID;

/**
 * Component is an extension for a {@link GameObject} that it is mounted to.
 * Component does not participate in the hierarchy system and is local to its object.
 *
 * <p>Common usages include adding rendering logic, physics behavior, or
 * custom scripts.</p>
 */
public abstract class Component {
    private String uuid;

    public transient GameObject gameObject;

    private String componentName;

    public Component() {
        this.uuid = UUID.randomUUID().toString();
    }

    /**
     * Override this to fully customize the start logic of a component.
     * @see Component#additionalStartLogic() Add additional startup logic
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

    /**
     * Called when a collision starts with another object.
     * Override to handle collision enter.
     * @param targetObj the other object
     * @param contact collision data
     * @param hitNormalization collision normal vector
     */
    public void startCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Called when a collision ends with another object.
     * Override to handle collision exit.
     */
    public void endCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

     /**
     * Called before the physics solver resolves a collision.
     * Override to adjust or cancel collision behavior.
     */
    public void preSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Called after the physics solver resolves a collision.
     * Override to react to collision results.
     */
    public void postSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Called when this component is destroyed.
     * Override to clean up resources.
     */
    public void destroy() {}

    /**
     * Finds a sibling component of the same GameObject.
     * @param componentClass the class of the component
     * @return the first matching component, or null if not found
     */
    public <T extends Component> T getSibling(Class<T> componentClass) {
        if (gameObject == null) return null;
        return gameObject.getFirstComponent(componentClass);
    }

    /**
     * Finds a component from the parent GameObject.
     * @param componentClass the class of the component
     * @return the first matching component, or null if not found or no parent exists
     */
    public <T extends Component> T getFromParent(Class<T> componentClass) {
        if (gameObject == null || gameObject.getParent() == null) return null;
        return gameObject.getParent().getFirstComponent(componentClass);
    }

    /**
     * Finds a component from a named child GameObject.
     * @param childName the name of the child
     * @param componentClass the class of the component
     * @return the first matching component, or null if not found
     */
    public <T extends Component> T getFromChild(String childName, Class<T> componentClass) {
        if (gameObject == null) return null;
        GameObject child = gameObject.getChild(childName);
        if (child == null) return null;
        return child.getFirstComponent(componentClass);
    }

     /**
     * Finds a component by its name in the root GameObject hierarchy.
     * @param name the component name
     * @return the matching component, or null if not found
     */
    public Component findComponentByName(String name) {
        if (gameObject == null) return null;
        GameObject root = gameObject.getRoot();
        return root.findComponentByName(name);
    }

    /**
     * Finds a component by its path.
     * @param path the path string
     * @return the matching component, or null if not found
     */
    public Component getComponentByPath(String path) {
        if (gameObject == null || path == null) return null;
        return gameObject.resolveComponentPath(path);
    }

    /**
     * Override this fully customize the exported field(s) in the properties window.
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
     * Sets the name of this component and notifies the owning GameObject.
     * @param name new component name
     */
    public void setComponentName(String name) {
        componentName = name;
        if (gameObject != null) {
            gameObject.onComponentNameChanged(this, name);
        }
    }
}