package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.ComponentSerializer;
import components.Component;
import editor.template.EditorTemplate;
import scene.Scene;
import utility.HierarchyPath;
import utility.HierarchyPaths;
import utility.IdPool;
import utility.log.EngineLog;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/**
 * GameObject is the base type of other object.
 * It contains uuid, name, and optional components.
 * It does not exist in the logic spatial world.
 * <p>
 * GameObject participates in the hierarchy of the scene tree,
 * which mean it can have a parent and multiple child objects.
 * Game objects that have no parent are considered root game objects,
 * located at scene's root level on their hierarchy tree.
 * <b>Inherited by:</b> {@link GameObject2D}
 * @see utility.HierarchyPath HierarchyPath system
 */
public class GameObject {
    /**
     * Logger for game objects.
     */
    protected static final EngineLog LOGGER = new EngineLog(GameObject.class);

    /**
     * Unique ID distributor for {@link GameObject}, managed the {@link #cachedID} of each object.
     */
    private static final IdPool idCounter = new IdPool(1, true);

    /**
     * The unique ID number of this game object.
     * Used by shader programs.
     * The number is assigned upon object creation and freed
     * (or released back to ID pool) upon object's destruction.
     */
    private transient int cachedID;

    /**
     * The identifier UUID of this game object.
     * @see UUID
     */
    private UUID uuid;

    /**
     * The name of this game object.
     */
    private String name;

    /**
     * The list of {@link Component} that this game object has.
     */
    private final CopyOnWriteArrayList<Component> components;

    /**
     * Should this game object be serialized or not.
     */
    private boolean isSerialize = true;

    /**
     * Is this game object removed and should be garbage collected.
     */
    private transient boolean isRemoved = false;

    /**
     * Is this game object finish initialization after created.
     */
    private transient boolean isStarted = false;

    /**
     * Is this game object dirty and need to be updated.
     */
    private transient boolean isDirty = false;

    /**
     * The parent object of this game object.
     */
    private transient GameObject parent;

    /**
     * The set of child object this game object has.
     */
    private final transient LinkedHashSet<GameObject> children;

    /**
     * The identifier UUID of this game object's parent object.
     */
    private UUID parentUUID;

    /**
     * The list of UUID string of the child objects this game object has.
     */
    private final List<UUID> childrenUUIDs;

    /**
     * Create a new {@link GameObject}.
     */
    public GameObject() {
        String name = GameObject.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link GameObject} with the given name.
     * @param name the new name for the object
     */
    public GameObject(String name) {
        if (invalidName(name)) name = GameObject.class.getSimpleName();
        this.name = name.trim();
        components = new CopyOnWriteArrayList<>();
        children = new LinkedHashSet<>();
        childrenUUIDs = new ArrayList<>();
        uuid = UUID.randomUUID();
        cachedID = idCounter.newId();
    }

    /**
     * Get the name of this object.
     * @return the name string
     */
    public String name() {
        if (name == null) name = getClass().getSimpleName();
        return name;
    }

    /**
     * Set a custom name for this object.
     * <p>
     * If the new name is null or bank, this object revert back to its class name.
     * @param name the name to update to
     */
    public void name(String name) {
        if (invalidName(name)) name = getClass().getSimpleName();
        this.name = name.trim();
    }

    /**
     * Get the child object of this game object with the given name.
     * @param childName the name of the child object
     * @return the first {@link GameObject} with a matching name or null
     */
    public GameObject getChild(String childName) {
        for (GameObject child : children) {
            if (child.name.equals(childName)) return child;
        }
        return null;
    }

    /**
     * Add an object as a child object to this game object. The child object's parent is set to this.
     * <p>
     * The child object cannot be this object itself or its ancestor.
     * For example, consider this hierarchy tree:
     * {@snippet lang="TEXT":
     *    A
     *    |_B
     *      |_C
     *        |_D
     * }
     * <br>
     * Object {@code D} cannot add object {@code A} as its parent.
     * </p>
     * @param child the child object to add
     */
    public void addChild(GameObject child) {
        if (child == null || child == this) return;
        if (isAncestor(child)) {
            System.err.println("Cannot add parent as child.");
            return;
        }
        if (child.parent != null) child.parent.removeChild(child);
        children.add(child);
        child.parent = this;
        child.parentUUID = this.uuid;
        updateChildrenUUIDs();
    }

    /**
     * Remove an object as a child object from this game object. The child object's parent is set to {@code null}.
     * @param child the child object to be removed
     */
    public void removeChild(GameObject child) {
        if (child == null) return;
        children.remove(child);
        if (child.parent == this) {
            child.parent = null;
            child.parentUUID = null;
        }
        updateChildrenUUIDs();
    }

    /**
     * Set the parent object for this game object using the given parent.
     * The new parent object adds this as its child.
     * <p>
     * If the new parent is {@code null} instead, the current parent object (if exist),
     * remove this object as its child.
     * </p>
     * @param newParent the new parent object
     */
    public void setParent(GameObject newParent) {
        if (newParent != null) {
            newParent.addChild(this);
            return;
        }
        if (parent != null) parent.removeChild(this);
        parent = null;
        parentUUID = null;
    }

    /**
     * Check if a game object is this game object's ancestor or not.
     * @param ancestorAble the potential ancestor object
     * @return true if this object belong to the ancestor's hierarchy tree
     */
    public boolean isAncestor(GameObject ancestorAble) {
        GameObject current = this.parent;
        while (current != null) {
            if (current == ancestorAble) {
                return true;
            }

            current = current.parent;
        }

        return false;
    }

    /**
     * Check if this game object is at the scene's root level (It has no parent).
     * @return true if there is no parent object
     */
    public boolean isRoot() {
        return parent == null;
    }

    /**
     * Get the highest ancestor object (at scene's root level) of this game object.
     * @return the root ancestor {@link GameObject}
     */
    public GameObject getRoot() {
        GameObject current = this;
        while (current.parent != null) {
            current = current.parent;
        }

        return current;
    }

    /**
     * Get all the descendant objects of this game object.
     * Starting from this game object's child objects, to the end of those children's hierarchy branches.
     * @return a list of descendant {@link GameObject}
     */
    public List<GameObject> getAllDescendants() {
        List<GameObject> descendants = new ArrayList<>();
        addDescendantsRecursive(descendants);

        return descendants;
    }

    /**
     * Recursively add all children of a game object into a list.
     * @param descendants the list for the descendant objects
     */
    private void addDescendantsRecursive(List<GameObject> descendants) {
        for (GameObject child : children) {
            descendants.add(child);
            child.addDescendantsRecursive(descendants);
        }
    }

    /**
     * Update the list of child objects' UUID that this game object has.
     */
    private void updateChildrenUUIDs() {
        childrenUUIDs.clear();
        for (GameObject child : children) {
            childrenUUIDs.add(child.uuid);
        }
    }

    /**
     * Get the {@link Component} with the given name.
     * @param componentName the name of the component to find
     * @return the first {@link Component} with matching name, or null if there is none
     */
    public Component findComponentByName(String componentName) {
        for (Component c : components) {
            if (c != null && c.name().equals(componentName)) return c;
        }

        return null;
    }

    /**
     * Get the first {@link Component} of a given type from this game object.
     * <p>
     * The return of this method follow the same order of which the components were added to this game object.
     * The result also includes subclasses of type {@code T}.
     * For example, 2 components were added to a game object as follows:
     * {@snippet lang = "java":
     * import components.AnimatedSpriteRenderer;
     * import components.SpriteRenderer;
     * public class Game() {
     *     public static void run() {
     *         GameObject object = newGameObject();
     *         SpriteRenderer spriteRenderer = object.getFirstComponent(SpriteRenderer.class);
     *         if (spriteRenderer instanceof AnimatedSpriteRenderer) {
     *              System.out.println("Get first SpriteRenderer return firstly added AnimatedSpriteRenderer component");
     *         }
     *     }
     *
     *     public static GameObject newGameObject() {
     *         GameObject newObject = new GameObject();
     *
     *         AnimatedSpriteRenderer animatedSpriteRenderer = new AnimatedSpriteRenderer();
     *         SpriteRenderer spriteRenderer = new SpriteRenderer();
     *         newObject.addComponents(animatedSpriteRenderer, spriteRenderer);
     *
     *         return newObject;
     *     }
     * }
     * }
     * Because {@link components.AnimatedSpriteRenderer} is subclass of {@link components.SpriteRenderer},
     * The returned component is the same as the {@code animatedSpriteRenderer} created and added in {@code newGameObject} method.
     * </p>
     * @param componentClass the class of the component
     * @return the first {@link Component} of type {@code T} or its subclasses
     * @param <T> the component's type, must be subclass of {@link Component}
     * @see #getComponents(Class) Get all components of a type from this game object
     */
    public <T extends Component> T getFirstComponent(Class<T> componentClass) {
        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                try {
                    return componentClass.cast(c);
                } catch (ClassCastException e) {
                    assert false : "FATAL: Casting component failed.";
                }
            }
        }
        return null;
    }

    /**
     * Get all {@link Component} of a given type from this game object.
     * <p>
     * The return of this method includes subclasses of type {@code T}.
     * For example, 3 components were added to a game object as follows:
     * {@snippet lang="java":
     * import components.AnimatedSpriteRenderer;
     * import components.SpatialComponent;
     * import components.SpriteRenderer;
     * import components.TextRenderer;
     * public class Game() {
     *     public static void run() {
     *         GameObject object = newGameObject();
     *         List<SpatialComponent> spatialComponents = getSpatialComponents(object); // size = 3
     *     }
     *
     *     public static GameObject newGameObject() {
     *         GameObject newObject = new GameObject();
     *         SpriteRenderer spriteRenderer = new SpriteRenderer();
     *         AnimatedSpriteRenderer animatedSpriteRenderer = new AnimatedSpriteRenderer();
     *         TextRenderer textRenderer = new TextRenderer();
     *         newObject.addComponents(animatedSpriteRenderer, spriteRenderer, textRenderer);
     *
     *         return newObject;
     *     }
     *
     *     public static SpatialComponent getSpatialComponents(GameObject gameObject) {
     *         if (gameObject == null) return List.of();
     *         return gameObject.getComponents(SpatialComponent.class);
     *     }
     * }
     * }
     * <br>
     * The content returns from {@code getSpatialComponents} when passing the game object created from {@code newGameObject}
     * will contain all 3 components {@code spriteRenderer},
     * {@code animatedSpriteRenderer} and {@code textRenderer} as they are subclasses of {@link components.SpatialComponent}.
     * @param componentClass the class of the component
     * @return the list of {@link Component} with type {@code T} and its subclasses
     * @param <T> the component's type, must be subclass of {@link Component}
     * @see #getFirstComponent(Class) Get the first component of a type from this game object
     */
    public <T extends Component> List<T> getComponents(Class<T> componentClass) {
        return components.stream()
                .filter(c -> c != null && componentClass.isAssignableFrom(c.getClass()))
                .map(componentClass::cast)
                .collect(Collectors.toList());
    }

    /**
     * Remove all {@link Component} of a given type from this game object.
     * <p>
     * This method removal also includes all subclasses of type {@code T}.
     * For example, 5 components were added to the game object as follows:
     * {@snippet lang = "java":
     * import components.AnimatedSpriteRenderer;
     * import components.SpriteRenderer;
     * import physic2d.collider.BoxCollider2D;
     * import physic2d.collider.CapsuleCollider2D;
     * import physic2d.collider.CircleCollider2D;
     * import physic2d.collider.CollisionShape2D;
     * public class Game() {
     *     public static void run(String[] args) {
     *         GameObject object = newGameObject(); // 5 components
     *         object.removeComponents(CollisionShape2D.class); // 3 components will be removed
     *         List<Component> components = object.getComponents(); // size = 2
     *     }
     *
     *     public static GameObject newGameObject() {
     *         GameObject newObject = new GameObject();
     *         newObject.addComponents(new AnimatedSpriteRenderer(), new SpriteRenderer(),
     *                new CircleCollider2D(), new CapsuleCollider2D(), new BoxCollider2D()
     *         );
     *
     *         return newObject;
     *     }
     * }
     * }
     * <br>
     * Since {@link physic2d.collider.BoxCollider2D}, {@link physic2d.collider.CircleCollider2D}
     * and {@link physic2d.collider.CapsuleCollider2D} are all subclass of {@link physic2d.collider.CollisionShape2D},
     * all 3 of them are removed from the game object.
     * The content of the component list after removing components of type {@link physic2d.collider.CollisionShape2D} will be
     * the added {@link components.AnimatedSpriteRenderer} and {@link components.SpriteRenderer}.
     * </p>
     * @param componentClass the class of the component
     * @param <T> the component's type, must be subclass of {@link Component}
     * @see #removeComponent(Component) Remove a component from this game object
     */
    public <T extends Component> void removeComponents(Class<T> componentClass) {
        Scene scene = LogicServer.currentScene();
        for (Component component : components) {
            if (component == null || !componentClass.isAssignableFrom(component.getClass())) continue;
            if (scene != null) scene.queueForComponentRemoval(component);
        }
        components.removeIf(c -> componentClass.isAssignableFrom(c.getClass()));
    }

    /**
     * Remove the given {@link Component} from this game object.
     * @param component the component to be removed
     * @return true if removed successfully
     */
    public boolean removeComponent(Component component) {
        boolean removed = components.remove(component);
        if (!removed) return false;
        Scene scene = LogicServer.currentScene();
        if (scene != null) scene.queueForComponentRemoval(component);

        setDirty(true);
        return true;
    }

    /**
     * Add a {@link Component} to this game object.
     * It is not allowed to add a component that is belonged to another game object.
     * @param component the component to add
     * @see #addComponents(Component...) Add multiple components to this game object
     */
    public void addComponent(Component component) {
        if (component == null) return;

        GameObject otherOwner = component.gameObject;
        if (otherOwner != null && otherOwner != this) return;
        if (component.getUUID() == null) component.setUUID(UUID.randomUUID());

        components.add(component);
        component.gameObject = this;
        if (isStarted) {
            if (LogicServer.runtimeMode()) component.start();
            else component.editorStart();
        }
        setDirty(true);
    }

    /**
     * Add one or more {@link Component} to this game object.
     * @param components the component(s) to add
     * @see #addComponent(Component) Add a component to this game object
     */
    public void addComponents(Component... components) {
        for (Component component : components) {
            if (component == null) continue;
            addComponent(component);
        }
    }

    /**
     * Step the editor logic of this game object by the given delta time.
     * <p>
     * Called once per iteration of the game's loop by the Engine.
     * This method is used instead of {@link #update(float)} when in editor mode
     * (not play testing the game or scene).
     * @param dt delta time
     * @apiNote
     * Incorrect use of this method might cause damage to the scene data.
     * In most cases, a game object does not require execution of its logic when the scene is being edited.<br>
     * All logic executed <b><u>should not</u></b> affect serialized data in an unrecoverable way,
     * especially related to removal logic.
     * <p>
     * Unless for the purpose of implement custom game loop logic, do not call this manually,
     * as it can cause unwanted editor logic step.
     * </p>
     * If this must be called, ensure that, for an instance of {@link GameObject}, this is only called once.
     */
    public void editorUpdate(float dt) {
        onUpdate(dt);

        for (Component component : components) {
            component.editorUpdate(dt);
        }
    }

    /**
     * Step the logic of this game object by the given delta time.
     * <p>
     * Called once per iteration of the game's loop by the Engine.
     * This first executes the logic in {@link #onUpdate(float)},
     * then iterates the component list and executes {@link Component#update(float)}
     * </p>
     * Unless there is a very specific use case, it is suggested to override {@link #onUpdate(float)} instead of this.
     * @param dt delta time
     * @apiNote
     * Unless for the purpose of implement custom game loop logic, <b><u>do not</u></b> call this manually,
     * as it can cause unwanted logic step.
     * <p>
     * If this must be called, ensure that, for an instance of {@link GameObject}, this is only called once.
     */
    public void update(float dt) {
        onUpdate(dt);
        for (Component component : components) component.update(dt);
    }

    /**
     * Optional hook for additional game object's logic before updating its components.
     * @param dt delta time
     */
    protected void onUpdate(float dt) {}

    /**
     * Step the physic logic of this game object by the delta time of the physic system.
     * <p>
     * Called once per iteration of the game's loop by the Engine.
     * This first executes the logic in {@link #onPhysicUpdate(float)},
     * then iterates the component list and executes {@link Component#physicUpdate(float)}
     * </p>
     *
     * @param dt
     */
    public void physicUpdate(float dt) {
        onPhysicUpdate(dt);

        for (Component component : components) {
            component.physicUpdate(dt);
        }
    }

    protected void onPhysicUpdate(float dt) {}

    /**
     * Initialize this game object's state when it is first added to the {@link Scene}.
     * <p>
     * This is called by the {@link Scene} on its starting logic or when this game object is added to a running scene.
     */
    public final void start() {
        onStart();
        isStarted = true;

        for (Component component : components) component.start();

        isDirty = true;
    }

    /**
     * Optional hook for additional game object's start logic before starting its components.
     */
    protected void onStart() {}

    /**
     * Initialize this game object's editor state when it is first added to the {@link Scene}.
     * <p>
     * This is called by the {@link Scene} on its editor starting logic or when this game object is added to a running scene.
     * @apiNote Do not use this method, unless there are specific initialization for the game object
     * that need to be reflected in editor mode.
     */
    public final void editorStart() {
        onEditorStart();
        isStarted = true;

        for (Component component : components) component.editorStart();

        isDirty = true;
    }

    /**
     * * Optional hook for additional game object's editor start logic before starting its components.
     */
    protected void onEditorStart() {}

    /**
     * Check if this object is initialized or not.
     * @return true if {@link #start()} or {@link #editorStart()} was called
     */
    public boolean started() {
        return isStarted;
    }

    /**
     * Export this game object's properties for editing in the Editor UI.
     */
    public final void imgui() {
        EditorTemplate.render(this);
    }

    /**
     * Additional game object's properties export before exporting its component's properties.
     */
    public void additionalImGuiLogic() {}

    /**
     * Destroy this game object and all its descendant objects.
     * This will also destroy all the components owned by these objects.
     * <p>
     * The destroyed objects are removed from the scene and cannot be added back.
     */
    public void destroy() {
        onDestroy();
        isRemoved = true;
        if (parent != null) parent.removeChild(this);
        List<GameObject> childrenCopy = new ArrayList<>(children);
        for (GameObject child : childrenCopy) {
            if (child == null) continue;
            child.destroy();
        }
        for (Component component : components) {
            if (component == null) continue;
            component.destroy();
        }
        if (cachedID > 0) {
            idCounter.releaseId(cachedID);
            cachedID = -1;
        }
    }

    /**
     * Optional hook for additional game object's logic before it is destroyed.
     */
    protected void onDestroy() {}

    /**
     * Create a new {@link GameObject} from this game object and its components without the hierarchy.
     * <p>
     * The copy process use serialization, ensure all subclasses of {@link GameObject} are supported.
     * </p>
     * The newly created object is a root object.
     * @return a new {@link GameObject}
     */
    public GameObject copy () {
        return copy(false);
    }

    /**
     * Create a new {@link GameObject} from this game object,
     * its components and optionally copy all the descendant objects.
     * <p>
     * The copy process use serialization, ensure all subclasses of {@link GameObject} are supported.
     * </p>
     * The newly created object is a root object.
     * @param copyHierarchy option to copy the hierarchy branch from this object
     * @return a new {@link GameObject}
     */
    public GameObject copy(boolean copyHierarchy) {
        GameObject copy = copySingleObject();

        boolean isChildrenEmpty = children.isEmpty() && (childrenUUIDs == null || childrenUUIDs.isEmpty());
        if (copyHierarchy && !isChildrenEmpty) copyDescendants(this, copy);

        return copy;
    }

    /**
     * Check if this game object is removed (destroyed)
     * @return true of destroyed
     * @see #destroy() Destroy this game object
     */
    public boolean isRemoved() {
        return isRemoved;
    }

    /**
     * Get the shader program's unique ID of this game object.
     * <p>
     * This is not the UUID used to identify this game object.
     * @return the ID value used by shader program
     * @see #getUUID() Get this game object's UUID
     */
    public int getUID() {
        return cachedID;
    }

    /**
     * Get the UUID uses to identify this game object.
     * @return the UUID
     */
    public UUID getUUID() {
        return uuid;
    }

    /**
     * Get all the components of this game object.
     * @return a copy of this game object's {@link Component} list
     */
    public List<Component> getComponents() {
        return new ArrayList<>(components);
    }

    /**
     * Disable the serialization flag for this game object. It can't no longer be saved to scene or copied from.
     */
    public void setNotSerialize() {
        isSerialize = false;
    }

    /**
     * Set the serialization flag for this game object.
     * @param isSerialized the serialization flag, true to enable serialization
     */
    public void setSerialize(boolean isSerialized) {
        isSerialize = isSerialized;
    }

    /**
     * Check if serialization is enabled for this game object.
     * @return true if enabled
     */
    public boolean isSerialize() {
        return isSerialize;
    }

    /**
     * Get the parent object of this game object.
     * @return the parent {@link GameObject}
     */
    public GameObject getParent() {
        return parent;
    }

    /**
     * Get the parent object's UUID of this game object.
     * @return the UUID of the parent object
     */
    public UUID getParentUUID() {
        return parentUUID;
    }

    /**
     * Get the set of child objects from this game object.
     * @return a copy of this game object's children set
     */
    public Set<GameObject> getChildren() {
        return new LinkedHashSet<>(children);
    }

    /**
     * Get the UUID of this game object's child objects.
     * @return a copy of this game object's children's UUID
     */
    public List<UUID> getChildrenUUIDs() {
        return new ArrayList<>(childrenUUIDs);
    }

    /**
     * Prepare this game object for serialization. This will sync the parent object's UUID and the children's UUIDs.
     */
    public void prepareForSerialization() {
        if (parent != null) {
            parentUUID = parent.uuid;
        }

        updateChildrenUUIDs();
    }

    /**
     * Restore the hierarchy structure of this game object after it is loaded from scene's structure.
     * @param scene the scene this game object is from
     */
    public void restoreHierarchy(Scene scene) {
        if (scene == null) return;

        if (parentUUID != null && isParentNotValid()) {
            GameObject parentGO = scene.getGameObject(parentUUID);
            if (parentGO != null) {
                parent = parentGO;
                parentGO.children.add(this);
            }
        }

        if (childrenUUIDs == null) return;
        children.clear();
        for (UUID childUUID : childrenUUIDs) {
            GameObject childGO = scene.getGameObject(childUUID);
            if (childGO == null) continue;
            children.add(childGO);
            if (childGO.isParentNotValid()) childGO.parent = this;
        }
    }

    /**
     * Check if the parent object's UUID is out of sync with the actual parent {@link GameObject}.
     * @return true if it is out of sync
     */
    private boolean isParentNotValid() {
        if (parent == null) return true;

        return !parentUUID.equals(parent.uuid);
    }

    /**
     * Copy this game object using serialization.
     * @return a new {@link GameObject}
     */
    protected GameObject copySingleObject() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String oJson = gson.toJson(this);
        GameObject obj = gson.fromJson(oJson, GameObject.class);

        obj.uuid = UUID.randomUUID();
        obj.cachedID = idCounter.newId();

        obj.parent = null;
        obj.parentUUID = null;
        obj.children.clear();
        obj.childrenUUIDs.clear();

        for (Component c : obj.getComponents()) c.setUUID(UUID.randomUUID());
        return obj;
    }

    /**
     * Copy the descendants objects from a game object to another.
     * @param source the game object where the descendant objects are from
     * @param target the game object to add the descendant to
     */
    protected void copyDescendants(GameObject source, GameObject target) {
        for (GameObject sourceChild : source.children) {
            GameObject copyChild = sourceChild.copySingleObject();

            target.addChild(copyChild);

            if (!sourceChild.children.isEmpty()) copyDescendants(sourceChild, copyChild);
        }
    }

    public HierarchyPath asPath() {
        return HierarchyPaths.of(this);
    }

    public static GameObject getObject(HierarchyPath absolutePath) {
        return HierarchyPaths.toGameObject(absolutePath);
    }

    public static GameObject getObject(HierarchyPath path, GameObject context) {
        return HierarchyPaths.toGameObject(path, context);
    }

    public static GameObject getObject(String absolutePath) {
        return HierarchyPaths.toGameObject(absolutePath);
    }

    public static GameObject getObject(String path, GameObject context) {
        return HierarchyPaths.toGameObject(path, context);
    }

    @Override
    public String toString() {
        return "Name: " + this.name +
                "\n  UUID: " + this.uuid +
                "\n  isSerialize: " + this.isSerialize +
                "\n  isGone: " + this.isRemoved;
    }

    /**
     * Check if this game object need to be updated.
     * @return the dirty flag status
     */
    public boolean isDirty() {
        return isDirty;
    }

    /**
     * Set the dirty flag for this game object.
     * @param dirty the dirty flag's value, true to indicate the object need to be updated.
     */
    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    protected static boolean invalidName(String name) {
        return name == null || name.isBlank() || name.trim().equals(HierarchyPath.Root);
    }
}