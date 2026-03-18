package scene;

import TheCellBeyond.*;
import TheCellBeyond.internal.DataSnapshot;
import TheCellBeyond.internal.LogicServer;
import components.Component;
import editor.components.EditorObjectIndicator;
import eventviewer.EngineEventCallback;
import eventviewer.event.SceneEvent;
import physic2d.PhysicBody2D;
import physic2d.Physic2D;
import utility.log.EngineLog;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Scene hold the collection of {@link GameObject} that form the scene tree hierarchy.
 * Each scene have access to their own physic world and viewport.
 * <p>
 * Scene manage its objects life cycle, consist of physic process and idle (logic) process update.
 * When an object is added or remove from scene, it is first queued and then process at the end of logic process update.
 */
public class Scene {
    private static final EngineLog Logger = new EngineLog(Scene.class);
    private final SceneLoader sceneLoader;
    private final DataSnapshot sceneData;
    private final List<GameObject> addedGameObjects;
    private final List<GameObject> removedGameObjects;
    private final List<Component> removedComponents;
    private final HashMap<GameObject, GameObject> addedGameObjectWithParents;
    private UUID sceneUUID;
    private String name;
    private GameObject root;
    private transient boolean sceneStarted = false;

    /**
     * Create a new {@link Scene} using the given loader.
     * @param sceneLoader the loader for this scene
     */
    public Scene(SceneLoader sceneLoader) {
        this.sceneLoader = sceneLoader;
        sceneData = new DataSnapshot();
        addedGameObjects  = new ArrayList<>();
        removedGameObjects = new ArrayList<>();
        removedComponents = new ArrayList<>();
        addedGameObjectWithParents = new HashMap<>();
    }

    /**
     * Initialize and load the scene resource.
     */
    public void init() {
        sceneLoader.loadResource(this);
    }

    /**
     * Destroy/Leave and end the scene.
     */
    public void destroy() {
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneLeaved, this));
        sceneData.gameObjects().forEach(GameObject::destroy);
        sceneData.clear();
        sceneLoader.onSceneEnd();
    }

    /**
     * Start the scene in runtime mode.
     * This will start all the game objects within it and initialize the scene's physic world.
     * <p>
     * This will emit the {@link SceneEvent.Type#SceneEntered} event before starting the game objects.
     * <p>
     * At the end of the start sequence, scene will process its queues to catch all changes before update started.
     */
    public void start() {
        sceneStarted = true;
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        sceneData.gameObjects().forEach(go -> {
            go.start();
            sceneData.physic2D().add(go);
        });
        sceneLoader.onSceneStarted(this);
        updateQueues();
    }

    /**
     * Start the scene in editor mode.
     */
    public void editorStart() {
        sceneStarted = true;
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        sceneData.gameObjects().forEach(GameObject::editorStart);
        sceneLoader.onSceneStarted(this);
        updateQueues();
    }

    public void editorUpdate(float dt) {
        if (!sceneStarted) return;
        if (rootIsFreed()) return;
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();
        sceneData.gameObjects().forEach(go -> {
            go.editorUpdate(dt);
            if (go.isRemoved()) queueObjectForRemoval(go);
        });
        updateQueues();
        sceneData.updated().set(true);
    }

    /**
     * Step the physic logic of scene.
     * Should be call before {@link #update(float)}
     * @param dt variable frame delta time
     */
    public void updatePhysic(float dt) {
        if (!sceneStarted) return;
        if (rootIsFreed()) return;
        sceneData.physic2D().update(dt, (fixedDT) -> {
            sceneData.gameObjects().forEach(go -> go.physicUpdate(fixedDT));
        });
        for (GameObject go : sceneData.gameObjects()) {
            if (go instanceof PhysicBody2D physicBody2D) physicBody2D.syncTransformFromPhysic();
        }
    }

    private boolean rootIsFreed() {
        if (root == null || root.isRemoved()) {
            Logger.info(String.format("Root object had been freed, exiting scene '%s'", name));
            destroy();
            return true;
        }
        return false;
    }

    /**
     * Update this scene by the given delta time.
     * @param dt variable frame delta time
     */
    public void update(float dt) {
        if (!sceneStarted) return;
        if (root.isRemoved()) {
            destroy();
            return;
        }
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();
        sceneData.gameObjects().forEach(go -> {
            go.update(dt);
            if (go.isRemoved()) queueObjectForRemoval(go);
        });
        updateQueues();
        sceneData.updated().set(true);
    }

    /**
     * Get the map of all game object currently in the scene.
     * @return a copy of the scene's game object map
     */
    public List<GameObject> getGameObjects() {
        return new ArrayList<>(sceneData.gameObjects());
    }

    /**
     * Get the list of object that is serialized in scene.
     * @return a new list of {@link GameObject}
     */
    public List<GameObject> getSerializedObjects() {
        return sceneData.gameObjects().stream()
                .filter(GameObject::isSerialize)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get the list of object that is serialized in scene and is orphan (has no parent). This does not include root object.
     * @return a new list of {@link GameObject}
     */
    public List<GameObject> getOrphanObjects() {
        return sceneData.gameObjects().stream()
                .filter(GameObject::isSerialize)
                .filter(go -> go != root)
                .filter(GameObject::isRoot)
                .collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get the root object of the scene.
     * @return scene root object or null if not exist
     */
    public GameObject root() {
        return root;
    }

    /**
     * Get a game object in the scene using their generated uid for the shader.
     * @param id the object id, often come from ObjectSelection shader
     * @return the {@link GameObject} with the given uid, null if there is no match
     * @see #getGameObject(UUID) Get a game object using their UUID
     */
    public GameObject getGameObject(int id) {
        UUID uuid = sceneData.cachedIDs().get(id);
        if (uuid != null) return sceneData.cachedObjectsByUUID().get(uuid);
        return null;
    }

    /**
     * Get a game object in the scene using the given uuid.
     * @param objectUUID uuid of the object to get
     * @return the {@link GameObject} with the given uuid, null if there is no match
     */
    public GameObject getGameObject(UUID objectUUID) {
        return sceneData.cachedObjectsByUUID().get(objectUUID);
    }

    /**
     * Queue the object to add to the scene as child of root object.
     * @param go the object to add
     */
    public void queueForObjectAddition(GameObject go) {
        queueForObjectAddition(go, root);
    }

    /**
     * Queue the object to add to the scene as child of another object on scene.
     * @param go the object to add
     * @param parent the parent for the object
     */
    public void queueForObjectAddition(GameObject go, GameObject parent) {
        if (go == null) return;
        if (addedGameObjects.contains(go) || sceneData.cachedObjectsByUUID().containsKey(go.getUUID())) return;
        addedGameObjects.add(go);
        if (parent != null) addedGameObjectWithParents.put(go, parent);
    }

    /**
     * Queue the object to be removed from the scene.
     * @param go the object to remove
     */
    public void queueObjectForRemoval(GameObject go) {
        if (go == null) return;
        if (go == root) {
            Logger.warning("Remove root object from scene is not allowed");
            return;
        }
        if (removedGameObjects.contains(go) || !sceneData.cachedObjectsByUUID().containsKey(go.getUUID())) return;
        removedGameObjects.add(go);
        addedGameObjects.remove(go);
        addedGameObjectWithParents.remove(go);
    }

    /**
     * Queue the component to be removed from the scene.
     * @param component the component to remove
     */
    public void queueForComponentRemoval(Component component) {
        if (component == null) return;
        if (!removedComponents.contains(component)) removedComponents.add(component);
    }

    /**
     * Reorder a game object in the scene's hierarchy. If the reordering object and the context sibling
     * does not share the same parent, it will be reparented to the sibling's parent.
     * <p>
     * This operation conformed to the hierarchy rules,
     * and will trigger object order update in scene if the reordering is valid.
     * @param reordering the object that need to be reordered
     * @param contextSibling the context object to provide position to reorder around
     * @param insertBeforeSibling should the reordering object be inserted before or after the context sibling
     * @return true if the operation was valid and reordered
     */
    public boolean reorderObject(GameObject reordering, GameObject contextSibling, boolean insertBeforeSibling) {
        if (reordering == null || contextSibling == null || reordering == contextSibling) return false;
        if (reordering == root) return false;
        if (reordering.isDescendantOf(contextSibling)) return false;
        GameObject targetParent = contextSibling.getParent();
        if (reordering.getParent() != targetParent) {
            if (invalidReparent(reordering, targetParent)) return false;
            reordering.setParent(targetParent);
        }
        if (targetParent != null) targetParent.reorderChild(reordering, contextSibling, insertBeforeSibling);
        reorderGameObjects();
        return true;
    }

    /**
     * Reparent a child object with its new parent. This follows the hierarchy rule of no circular reference.
     * @param child the object to reparent
     * @param newParent the target parent object to receive the child
     * @return true if reparent successfully
     */
    public boolean reparentObject(GameObject child, GameObject newParent) {
        if (invalidReparent(child, newParent)) return false;
        child.setParent(newParent);
        reorderGameObjects();
        return true;
    }

    /**
     * Check if reparenting the given child to a new parent object is an invalid hierarchy restructure or not.
     * <p>
     * A reparent operation is considered invalid if the child is null, or is root object of scene,
     * the new parent is the child itself, or the child is the ancestor of the new parent.
     * @param child the child object to reparent
     * @param newParent the parent object to for the child
     * @return true if the operation is invalid
     */
    public boolean invalidReparent(GameObject child, GameObject newParent) {
        if (child == null || child == newParent) return true;
        if (child == root) {
            Logger.warning("Cannot reparent scene root object");
            return true;
        }
        return newParent != null && newParent.isDescendantOf(child);
    }

    /**
     * Get the viewport of this scene.
     * @return the viewport instance
     */
    public Viewport viewport() {
        return sceneData.viewport();
    }

    public void imgui() {
        sceneLoader.imgui();
    }

    /**
     * Get the physic world of this scene
     * @return the physic 2D instance
     */
    public Physic2D getPhysic2D() {
        return sceneData.physic2D();
    }

    /**
     * Get the uuid of this scene
     * @return the scene uuid
     */
    public UUID sceneUUID() {
        return sceneUUID;
    }

    /**
     * Get the name of the scene.
     * @return the name string
     */
    public String name() {
        return name;
    }

    void name(String name) {
        this.name = name;
    }

    private void addObjectToScene(GameObject go, GameObject parent) {
        if (go == null || sceneData.cachedObjectsByUUID().containsKey(go.getUUID())) return;
        sceneData.cachedIDs().put(go.getUID(), go.getUUID());
        sceneData.cachedObjectsByUUID().put(go.getUUID(), go);
        if (go == root) sceneData.gameObjects().addFirst(go);
        else sceneData.gameObjects().add(go);
        if (parent != null) parent.addChild(go);
        EditorObjectIndicator.add(go);
        if (sceneStarted) {
            if (LogicServer.runtimeMode()) {
                go.start();
                sceneData.physic2D().add(go);
            }
            else go.editorStart();
        }
        go.getChildren().stream()
                .filter(child -> !sceneData.cachedObjectsByUUID().containsKey(child.getUUID()))
                .forEach(child -> queueForObjectAddition(child, go));
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectAdded, this, go));
    }

    private void removeObjectFromScene(GameObject go) {
        if (go == null) return;
        if (go == root) {
            Logger.warning("Cannot remove scene root object");
            return;
        }
        if (go.getParent() != null) go.getParent().removeChild(go);
        go.getAllDescendants().forEach(this::removeFromData);
        removeFromData(go);
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectRemoved, this , go));
    }

    private void removeFromData(GameObject go) {
        sceneData.removeObject(go);
    }

    private void removeComponentFromScene(Component component) {
        if (component == null) return;
        component.destroy();
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ComponentRemoved, this, component));
    }

    private void updateQueues() {
        List<GameObject> toAdd = new ArrayList<>(addedGameObjects);
        HashMap<GameObject, GameObject> toAddParent = new HashMap<>(addedGameObjectWithParents);
        List<GameObject> toRemove = new ArrayList<>(removedGameObjects);
        List<Component> componentToRemove = new ArrayList<>(removedComponents);
        addedGameObjects.clear();
        addedGameObjectWithParents.clear();
        removedGameObjects.clear();
        removedComponents.clear();
        componentToRemove.forEach(this::removeComponentFromScene);
        toRemove.forEach(this::removeObjectFromScene);
        toAdd.forEach(go -> {
            GameObject parent = toAddParent.get(go);
            addObjectToScene(go, parent);
        });
        if (toAdd.isEmpty()) return;
        reorderGameObjects();
    }

    /**
     * Reorder the array of objects in scene so that they conform to the hierarchy order when flattened.
     */
    private void reorderGameObjects() {
        List<GameObject> ordered = new ArrayList<>();
        collectHierarchy(root, ordered);
        sceneData.gameObjects().stream()
                .filter(go -> go != root && go.isRoot())
                .forEach(ordered::add);
        sceneData.gameObjects().clear();
        sceneData.gameObjects().addAll(ordered);
    }

    /**
     * Collect the hierarchy of an object into a flat array list.
     * This method collect in depth first order.
     * @param context the object to start searching
     * @param collector the array that will receive the list
     */
    private void collectHierarchy(GameObject context, List<GameObject> collector) {
        if (context == null || !sceneData.cachedObjectsByUUID().containsKey(context.getUUID())) return;
        collector.add(context);
        context.getChildren().forEach(child -> collectHierarchy(child, collector));

    }

    /**
     * Exclusive method for scene manager to load the deserialized data of this scene.
     * @param file the file data to load from
     */
    void loadDataFromFile(SceneFile file) {
        if (file == null) return;
        sceneUUID = file.uuid();
        name = file.name();
        root = file.root();
        List<GameObject> objects = new ArrayList<>(file.objects());
        UUID rootUUID = root.getUUID();
        if (rootUUID != null) objects.removeIf(go -> rootUUID.equals(go.getUUID()));
        objects.addFirst(root);
        objects.forEach(go -> addObjectToScene(go, null));
        objects.forEach(go -> go.restoreHierarchy(this));
        reorderGameObjects();
    }
}
