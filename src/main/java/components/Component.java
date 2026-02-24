package components;

import TheCellBeyond.GameObject;
import editor.EditorWidget;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import utility.HierarchyPath;
import utility.HierarchyPaths;
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
 * <b>Inherited by:</b> {@link Component2D}, {@link TheCellBeyond.Transform2D}, {@link Controller2D}, {@link StateEngine}
 * <p>
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
 *         super.onStart();
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
    private String componentName;

    /**
     * Create a new {@link Component} instance.
     */
    public Component() {
        String name = Component.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link Component} instance using the given name.
     * @param name the new name for the component
     */
    public Component(String name) {
        if (invalidName(name)) name = this.getClass().getSimpleName();
        componentName = name.trim();
        uuid = UUID.randomUUID();
    }

    /**
     * Initialize the component's state when its {@link GameObject} started.
     * If a component is added to an already running game object, it will be automatically started.
     * <p>
     * Mainly called by the owning game object {@link GameObject#start()} logic.
     * </p>
     * Unless there is a very specific use case, it is suggested to override {@link #onStart()} instead of this.
     * @see Component#onStart() Add additional component startup logic
     */
    public void start() {
        onStart();
    }

    /**
     * Optional hook for additional component's start logic. Suggested override flow:
     * {@snippet lang = java:
     * public class CustomComponent extends Component {
     *     @Override
     *     protected void onStart() {
     *         customSubclassLogic();
     *         super.onStart();
     *     }
     *
     *     private void customSubclassLogic() {
     *         // do custom component's logic
     *     }
     * }
     *}
     * @apiNote
     * Call to {@code super.onStart} can be omitted if the class that implement this method
     * is direct subclass of {@link Component}, or the super class does not have override for this method.
     * <p>
     * The position for when to call {@code super.onStartLogic()} may depend on if the implemented logic need to go first,
     * or after the super's implementation.
     */
    protected void onStart() {}

    /**
     * Initialize the component's state when its {@link GameObject} started in editor.
     * If a component is added to an already running game object, it will be automatically started.
     * <p>
     * Mainly called by the owning game object {@link GameObject#editorStart()} logic.
     * </p>
     * Unless there is a very specific use case, it is suggested to override {@link #onEditorStart()} instead of this.
     * @see Component#onEditorStart()  Add additional component startup logic
     * @apiNote Do not use this method, unless there are specific initialization for the component
     * that need to be reflected in editor mode.
     */
    public void editorStart() {
        onEditorStart();
    }

    /**
     * Optional hook for additional component's start logic while in editor mode.
     * <p>
     * Unless there is specific changes that need to reflect in editor, do not use this method.
     */
    protected void onEditorStart() {}

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

    /**
     * Step the physic logic of this component by the delta time of the physic system.
     * <p>
     * Called once per iteration of the game's loop by the owning game object {@link GameObject#physicUpdate(float)}.
     * </p>
     * @param dt the fixed delta time of physic tick
     * @see #onPhysicUpdate(float) Add optional physic update hook
     * @apiNote
     * Unless for the purpose of implement custom game physic logic, <b><u>do not</u></b> call this manually,
     * as it can cause unwanted physic step.
     * <p>
     * If this must be called, ensure that, for an instance of {@link Component}, this is only called once.
     */
    public void physicUpdate(float dt) {
        onPhysicUpdate(dt);
    }

    /**
     * Optional hook for additional component's physic logic.
     * @param dt the fixed delta time of physic tick
     */
    protected void onPhysicUpdate(float dt) {}

    /**
     * Callback at the start of physic collision.
     * @param targetObj the contacted object
     * @param contact the physic contact
     * @param hitNormalization normalization vector
     */
    public void startCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Callback at the end of physic collision.
     * @param targetObj the contacted object
     * @param contact the physic contact
     * @param hitNormalization normalization vector
     */
    public void endCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Callback before resolving physic collision.
     * @param targetObj the contacted object
     * @param contact the physic contact
     * @param hitNormalization normalization vector
     */
    public void preSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    /**
     * Callback after physic collision is resolved.
     * @param targetObj the contacted object
     * @param contact the physic contact
     * @param hitNormalization normalization vector
     */
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

    /**
     * Export this component's properties for editing in the Editor UI.
     */
    public void imgui() {
        String name = EditorWidget.inputText("Name", componentName, this);
        if (!name.equals(componentName)) name(name);

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
     * @return the name string
     */
    public String name() {
        if (componentName == null) componentName = this.getClass().getSimpleName();
        return componentName;
    }

    /**
     * Set a custom name for this component.
     * <p>
     * If the name is {@code null} or blank, this component revert back to its class name.
     * @param name the name to update with
     */
    public void name(String name) {
        if (invalidName(name)) name = this.getClass().getSimpleName();
        componentName = name.trim();
    }

    /**
     * Get the hierarchy path that leads to this component.
     * @return the {@link HierarchyPath} of this component
     */
    public HierarchyPath asPath() {
        return HierarchyPaths.of(this);
    }

    /**
     * Get a component at the given hierarchy path.
     * @param absolutePath the absolute hierarchy path that leads to the component
     * @return a {@link Component} or null there is none
     */
    public static Component getComponent(HierarchyPath absolutePath) {
        return HierarchyPaths.toComponent(absolutePath);
    }

    /**
     * Get a component at the given hierarchy path.
     * @param path the relative hierarchy path that leads to the component
     * @param context the context object to resolve the relative path from
     * @return a {@link Component} or null there is none
     */
    public static Component getComponent(HierarchyPath path, GameObject context) {
        return HierarchyPaths.toComponent(path, context);
    }

    /**
     * Get a component at the given hierarchy path.
     * @param path the relative hierarchy path string that leads to the component
     * @param context the context object to resolve the relative path from
     * @return a {@link Component} or null there is none
     */
    public static Component getComponent(String path, GameObject context) {
        return HierarchyPaths.toComponent(path, context);
    }

    /**
     * Get a component at the given hierarchy path.
     * @param absolutePath the absolute hierarchy path string that leads to the component
     * @return a {@link Component} or null there is none
     */
    public static Component getComponent(String absolutePath) {
        return HierarchyPaths.toComponent(absolutePath);
    }

    /**
     * Check if the name given to a component is invalid.
     * <p>
     * A name is considered invalid if it is null, blank or contain the component delimit symbol from {@link HierarchyPath#ComponentDelimiter}.
     * @param name the component name to check for
     * @return true if the name is invalid
     */
    protected static boolean invalidName(String name) {
        return name == null || name.isBlank() || name.contains(HierarchyPath.ComponentDelimiter);
    }
}