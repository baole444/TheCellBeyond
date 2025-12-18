package components;

import TheCellBeyond.GameObject;
import editor.ImEditorGui;
import imgui.ImGui;
import org.jbox2d.dynamics.contacts.Contact;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector2f;
import utility.log.EngineLog;

import java.util.UUID;

/**
 * Component is an extension for {@link GameObject}.
 * <p>
 * It is the leaf of the scene hierarchy, only affected by the immediate object it belongs to.
 * </p>
 * Component can reference each other by name or by hierarchy path.
 * <p>
 * If a component A contains nested component member B and C.
 * Exposing B and C to A's owning {@link GameObject} is not always desired.
 * In this situation, A need to chain B and C's logic into its own life cycle.
 * </p>
 * Example for nested component scenario:
 * {@snippet lang = java:
 * public class A extends Component {
 *     public static class B extends Component {
 *         // B component's logic
 *     }
 *
 *     public static class C extends Component {
 *         // C component's logic
 *     }
 *
 *     private C nestedC = new C();
 *     private B nestedB = new B();
 *
 *     @Override
 *     protected void onStartLogic() {
 *         nestedB.gameObject = this.gameObject;
 *         nestedC.gameObject = this.gameObject;
 *         nestedB.start();
 *         nestedC.start();
 *         super.onStarting();
 *     }
 *
 *     @Override
 *     protected void onUpdate(float dt) {
 *         nestedB.update(dt);
 *         nestedC.update(dt);
 *         super.onUpdate();
 *     }
 *
 *     @Override
 *     protected void onDestroy() {
 *         nestedB.destroy();
 *         nestedC.destroy();
 *         super.onDestroy();
 *     }
 * }
 *}
 */
public abstract class Component {
    /**
     * Logger for components.
     */
    protected static final EngineLog LOGGER = new EngineLog(Component.class);

    /**
     * Get the UUID uses to identify this component.
     */
    private UUID uuid;

    /**
     * The owning {@link GameObject} of this component.
     */
    public transient GameObject gameObject;

    /**
     * Custom name of this component.
     * Named component is cached in scene's data for faster access.
     */
    private String componentName = null;

    public Component() {
        uuid = UUID.randomUUID();
    }

    /**
     * Initialize the component's state when its {@link GameObject} started.
     * If a component is added to an already running game object, it will be automatically started.
     * <p>
     * Mainly called by the owning game object {@link GameObject#start()} logic.
     * </p>
     * Unless there is a very specific use case, it is suggested to override {@link #onStarting()} instead of this.
     * @see Component#onStarting() Add additional component startup logic
     */
    public void start() {
        onStarting();
    }

    /**
     * Optional hook for additional component's start logic. Suggested override flow:
     * {@snippet lang = java:
     * public class CustomComponent extends Component {
     *     @Override
     *     protected void onStartLogic() {
     *         customSubclassLogic();
     *         super.onStarting();
     *     }
     *
     *     private void customSubclassLogic() {
     *         // do custom component's logic
     *     }
     * }
     *}
     */
    protected void onStarting() {}

    /**
     * Step the editor logic of this component by the given delta time.
     * <p>
     * Called once per iteration of the game's loop by the owning game object {@link GameObject#editorUpdate(float)}.
     * This method is used instead of {@link #update(float)} when in editor mode
     * (not play testing the game or scene).
     * @param dt delta time
     * @see #onEditorUpdate(float) Add optional editor update hook
     * @apiNote
     * Incorrect use of this method might cause damage to the scene data.
     * In most cases, a component does not require execution of its logic when the scene is being edited.<br>
     * All logic executed <b><u>should not</u></b> affect serialized data in an unrecoverable way,
     * especially related to removal logic.
     * <p>
     * Unless for the purpose of implement custom game loop logic, do not call this manually,
     * as it can cause unwanted editor logic step.
     * </p>
     * If this must be called, ensure that, for an instance of {@link Component}, this is only called once.
     */
    public void editorUpdate(float dt) {
        onEditorUpdate(dt);
    }

    /**
     * Optional hook for additional component's editor logic.
     * @param dt delta time
     */
    protected void onEditorUpdate(float dt) {}

    /**
     * Step the logic of this component by the given delta time.
     * <p>
     * Called once per iteration of the game's loop by the owning game object {@link GameObject#update(float)}.
     * </p>
     * @param dt delta time
     * @see #onUpdate(float) Add optional update hook
     * @apiNote
     * Unless for the purpose of implement custom game loop logic, <b><u>do not</u></b> call this manually,
     * as it can cause unwanted logic step.
     * <p>
     * If this must be called, ensure that, for an instance of {@link Component}, this is only called once.
     */
    public void update(float dt) {
        onUpdate(dt);
    }

    /**
     * Optional hook for additional component's logic.
     * @param dt delta time
     */
    protected void onUpdate(float dt) {}

    public void startCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void endCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void preSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void postSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Upon calling destroy, the component will discard its uuid, game object reference, and its name.
     */
    public final void destroy() {
        onDestroy();
        uuid = null;
        gameObject = null;
        componentName = null;
    }

    /**
     * Optional hook for additional destroy (clean up) logic of a component.
     * <p>
     * This is useful for when there are external states that need to be aware of this component's destruction.
     */
    protected void onDestroy() {}

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
     * Additional component's properties export.
     */
    protected void additionalImGuiLogic() {}

    /**
     * Get the UUID uses for identify this component.
     * @return the UUID
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Set the identifier UUID of this component.
     * @param uuid the UUID to set
     * @apiNote
     * <p>
     * API intended for internal usage only.
     * </p>
     * Do not use this method to refresh, or change component UUID.
     * Doing so will cause inconsistency with the scene data structure and potential data corruption.
     */
    public void setUUID(UUID uuid) {
        this.uuid = uuid;
    }

    /**
     * Get the custom name of this component.
     * @return the name string, or null if it doesn't have one
     */
    @Nullable
    public String getComponentName() {
        return componentName;
    }

    /**
     * Set a custom name for this component.
     * <p>
     * If the name is {@code null} or blank, this remove the custom name of the component instead.
     * @param name the name to update with, nullable
     */
    public void setComponentName(@Nullable String name) {
        if (name != null && name.isBlank()) name = null;
        componentName = name;

        if (gameObject != null) gameObject.onComponentNameChanged(this, name);

    }
}