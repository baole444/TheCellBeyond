package scene;

import TheCellBeyond.*;
import TheCellBeyond.internal.DataSnapshot;
import TheCellBeyond.internal.LogicServer;
import TheCellBeyond.internal.RenderUpdateSnapshot;
import components.Component;
import editor.components.EditorObjectIndicator;
import eventviewer.EngineEventCallback;
import eventviewer.event.SceneEvent;
import physic2d.PhysicBody2D;
import physic2d.Physic2D;

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
    private final SceneLoader sceneLoader;
    private final DataSnapshot sceneData;
    private final List<GameObject> addedGameObjects;
    private final List<GameObject> removedGameObjects;
    private final List<Component> removedComponents;
    private final HashMap<GameObject, GameObject> addedGameObjectWithParents;
    private UUID sceneUUID;
    private String name;
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
    public synchronized void destroy() {
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneLeaved, this));
        sceneData.gameObjectByUUIDs().values().forEach(GameObject::destroy);
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
        sceneData.gameObjectByUUIDs().values().forEach(go -> {
            go.start();
            sceneData.physic2D().add(go);
            cacheComponents(go);
        });
        sceneLoader.onSceneStarted(this);
        updateQueues();
    }

    public void editorStart() {
        sceneStarted = true;
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.editorStart();
            cacheComponents(go);
        }
        updateQueues();
    }

    public void editorUpdate(float dt) {
        if (!sceneStarted) return;
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.editorUpdate(dt);
            if (go.isRemoved()) queueObjectForRemoval(go);
        }
        updateQueues();
        sceneData.updated().set(true);
        RenderUpdateSnapshot snapshot = sceneData.extractRenderData();
        if (snapshot != null) {
            EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectUpdated, this, snapshot));
        }
    }

    /**
     * Step the physic logic of scene.
     * Should be call before {@link #update(float)}
     * @param dt variable frame delta time
     */
    public void updatePhysic(float dt) {
        if (!sceneStarted) return;
        sceneData.physic2D().update(dt, (fixedDT) -> {
            for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
                go.physicUpdate(fixedDT);
            }
        });
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            if (go instanceof PhysicBody2D physicBody2D) physicBody2D.syncTransformFromPhysic();
        }
    }

    /**
     * Update this scene by the given delta time.
     * @param dt variable frame delta time
     */
    public void update(float dt) {
        if (!sceneStarted) return;
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.update(dt);
            if (go.isRemoved()) queueObjectForRemoval(go);
        }
        updateQueues();
        sceneData.updated().set(true);
        RenderUpdateSnapshot snapshot = sceneData.extractRenderData();
        if (snapshot != null) EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectUpdated, this, snapshot));
    }

    /**
     * Get the map of all game object currently in the scene.
     * @return a copy of the scene's game object map
     */
    public Map<UUID, GameObject> getGameObjects() {
        return new HashMap<>(sceneData.gameObjectByUUIDs());
    }

    /**
     * Get the list of object that is serialized in scene.
     * @return a new list of {@link GameObject}
     */
    public List<GameObject> getSerializedObject() {
        return sceneData.gameObjectByUUIDs().values().stream().filter(GameObject::isSerialize).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Get the list of object at the scene root level.
     * @return a new list of {@link GameObject}
     */
    public List<GameObject> getRootGameObjects() {
        return new ArrayList<>(sceneData.rootGameObjects());
    }

    /**
     * Get a game object in the scene using their generated uid for the shader.
     * @param id the object id, often come from ObjectSelection shader
     * @return the {@link GameObject} with the given uid, null if there is no match
     * @see #getGameObject(UUID) Get a game object using their UUID
     */
    public GameObject getGameObject(int id) {
        UUID uuid = sceneData.cachedIDs().get(id);
        if (uuid != null) return sceneData.gameObjectByUUIDs().get(uuid);
        return null;
    }

    /**
     * Get a game object in the scene using the given uuid.
     * @param objectUUID uuid of the object to get
     * @return the {@link GameObject} with the given uuid, null if there is no match
     */
    public GameObject getGameObject(UUID objectUUID) {
        return sceneData.gameObjectByUUIDs().get(objectUUID);
    }

    /**
     * Queue the object to add to the scene.
     * @param go the object to add
     */
    public void queueForObjectAddition(GameObject go) {
        queueForObjectAddition(go, null);
    }

    /**
     * Queue the object to add to the scene as child of another object on scene.
     * @param go the object to add
     * @param parent the parent for the object
     */
    public void queueForObjectAddition(GameObject go, GameObject parent) {
        if (go == null) return;
        if (addedGameObjects.contains(go) || sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) return;
        addedGameObjects.add(go);
        if (parent != null) addedGameObjectWithParents.put(go, parent);
    }

    /**
     * Queue the object to be removed from the scene.
     * @param go the object to remove
     */
    public void queueObjectForRemoval(GameObject go) {
        if (go == null) return;
        if (removedGameObjects.contains(go) || sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) return;
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
     * Reparent a child object with its new parent. This follows the hierarchy rule of no circular reference.
     * @param child the object to reparent
     * @param newParent the target parent object to receive the child
     * @return true if reparent successfully
     */
    public boolean reparentObject(GameObject child, GameObject newParent) {
        if (child == null || child == newParent) return false;
        if (newParent != null && newParent.isAncestor(child)) return false;
        if (child.getParent() == null) sceneData.rootGameObjects().remove(child);
        child.setParent(newParent);
        if (newParent == null && !sceneData.rootGameObjects().contains(child)) sceneData.rootGameObjects().add(child);
        return true;
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

    private void cacheComponents(GameObject go) {
        for (Component c : go.getComponents()) {
            sceneData.componentsByUUID().put(c.getUUID(), c);
        }
    }

    private void uncacheComponents(GameObject go) {
        for (Component c : go.getComponents()) {
            sceneData.componentsByUUID().remove(c.getUUID());
        }
    }

    private void addObjectToScene(GameObject go, GameObject parent) {
        if (go == null || sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) return;
        sceneData.cachedIDs().put(go.getUID(), go.getUUID());
        sceneData.gameObjectByUUIDs().put(go.getUUID(), go);
        if (parent != null) {
            parent.addChild(go);
        }
        if (go.getParentUUID() == null && !sceneData.rootGameObjects().contains(go)) sceneData.rootGameObjects().add(go);
        EditorObjectIndicator.add(go);
        if (sceneStarted) {
            if (LogicServer.runtimeMode()) {
                go.start();
                sceneData.physic2D().add(go);
            }
            else go.editorStart();
            cacheComponents(go);
        }
        for (GameObject child : go.getChildren()) {
            if (!sceneData.gameObjectByUUIDs().containsKey(child.getUUID())) queueForObjectAddition(child, go);
        }
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectAdded, this, go));
    }

    private void removeObjectFromScene(GameObject go) {
        if (go == null) return;
        if (go.getParent() != null) {
            go.getParent().removeChild(go);
        } else {
            sceneData.rootGameObjects().remove(go);
        }
        List<GameObject> descendants = go.getAllDescendants();
        for (GameObject descendant : descendants) {
            sceneData.cachedIDs().remove(descendant.getUID());
            sceneData.gameObjectByUUIDs().remove(descendant.getUUID());
            sceneData.physic2D().destroyObject(descendant);
            uncacheComponents(descendant);
        }
        sceneData.cachedIDs().remove(go.getUID());
        sceneData.gameObjectByUUIDs().remove(go.getUUID());
        sceneData.physic2D().destroyObject(go);
        uncacheComponents(go);
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ObjectRemoved, this , go));
    }

    private void removeComponentFromScene(Component component) {
        if (component == null) return;
        sceneData.componentsByUUID().remove(component.getUUID());
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
        for (Component c : componentToRemove) removeComponentFromScene(c);
        for (GameObject go : toRemove) removeObjectFromScene(go);
        for (GameObject go : toAdd) {
            GameObject parent = toAddParent.get(go);
            addObjectToScene(go, parent);
        }
    }

    /**
     * Exclusive method for scene manager to load the deserialized data of this scene.
     * @param file the file data to load from
     */
    void loadDataFromFile(SceneFile file) {
        if (file == null || file.objects().isEmpty()) return;
        sceneUUID = file.uuid();
        name = file.name();
        List<GameObject> objects = file.objects();
        objects.forEach(go -> addObjectToScene(go, null));
        objects.forEach(go -> go.restoreHierarchy(this));
    }
}
