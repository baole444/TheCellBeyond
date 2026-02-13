package scene;

import TheCellBeyond.*;
import TheCellBeyond.internal.DataSnapshot;
import TheCellBeyond.internal.LogicServer;
import TheCellBeyond.internal.RenderUpdateSnapshot;
import components.Component;
import components.IsNotSelectable;
import editor.components.EditorObjectIndicator;
import eventviewer.EngineEventCallback;
import eventviewer.event.SceneEvent;
import physic2d.PhysicBody2D;
import physic2d.Physic2D;

import java.util.*;
import java.util.stream.Collectors;

public class Scene {
    private final SceneLoader sceneLoader;
    private transient boolean sceneStarted = false;
    private final DataSnapshot sceneData;
    private UUID sceneUUID;

    private final List<GameObject> addedGameObjects;
    private final List<GameObject> removedGameObjects;
    private final List<Component> removedComponents;
    private final HashMap<GameObject, GameObject> addedGameObjectWithParents;

    public Scene(SceneLoader sceneLoader) {
        this.sceneLoader = sceneLoader;
        sceneData = new DataSnapshot();
        addedGameObjects  = new ArrayList<>();
        removedGameObjects = new ArrayList<>();
        removedComponents = new ArrayList<>();
        addedGameObjectWithParents = new HashMap<>();
    }

    public void init() {
        sceneLoader.loadResource(this);
        sceneLoader.onSceneEntered(this);
    }

    public void start() {
        sceneStarted = true;
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.start();
            sceneData.physic2D().add(go);
            cacheComponents(go);
        }

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

    public Component getComponentByUUID(UUID uuid) {
        return sceneData.componentsByUUID().get(uuid);
    }

    public void queueForObjectAddition(GameObject go, GameObject parent) {
        if (go == null) return;

        if (!addedGameObjects.contains(go) && !sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) {
            addedGameObjects.add(go);
            if (parent != null) {
                addedGameObjectWithParents.put(go, parent);
            }
        }
    }

    private void addObjToScene(GameObject go, GameObject parent) {
        if (go == null || sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) return;
        sceneData.cachedIDs().put(go.getUID(), go.getUUID());
        sceneData.gameObjectByUUIDs().put(go.getUUID(), go);
        if (parent != null) {
            parent.addChild(go);
        }

        if (go.getParentUUID() == null && !sceneData.rootGameObjects().contains(go)) sceneData.rootGameObjects().add(go);
        if (go.isSerialize() &&
                go.getFirstComponent(IsNotSelectable.class) == null &&
                go.getFirstComponent(EditorObjectIndicator.class) == null &&
                go instanceof GameObject2D
        ) {
            go.addComponent(new EditorObjectIndicator());
        }

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

    public void queueForObjectAddition(GameObject go) {
        queueForObjectAddition(go, null);
    }

    public void queueObjectForRemoval(GameObject go) {
        if (go == null) return;

        if (!removedGameObjects.contains(go) && sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) {
            removedGameObjects.add(go);
            addedGameObjects.remove(go);
            addedGameObjectWithParents.remove(go);
        }
    }

    public void queueForComponentRemoval(Component component) {
        if (component == null) return;

        if (!removedComponents.contains(component)) removedComponents.add(component);
    }

    private void removeComponentFromScene(Component component) {
        if (component == null) return;

        sceneData.componentsByUUID().remove(component.getUUID());
        component.destroy();

        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.ComponentRemoved, this, component));
    }

    private void removeObjFromScene(GameObject go) {
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

    public boolean reparentObject(GameObject child, GameObject newParent) {
        if (child == null) return false;
        if (newParent != null && (child == newParent || newParent.isAncestor(child))) {
            return false;
        }

        if (child.getParent() == null) sceneData.rootGameObjects().remove(child);

        child.setParent(newParent);

        if (newParent == null && !sceneData.rootGameObjects().contains(child)) {
            sceneData.rootGameObjects().add(child);
        }

        return true;
    }

    public synchronized void destroy() {
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneLeaved, this));
        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.destroy();
        }

        sceneLoader.onSceneEnd();
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

        for (Component c : componentToRemove) {
            removeComponentFromScene(c);
        }

        for (GameObject go : toRemove) {
            removeObjFromScene(go);
        }

        for (GameObject go : toAdd) {
            GameObject parent = toAddParent.get(go);
            addObjToScene(go, parent);
        }
    }

    public Map<UUID, GameObject> getGameObjects() {
        return sceneData.gameObjectByUUIDs();
    }

    /**
     * Get the list of object that is serialized from scene.
     * @return a new list of {@link GameObject}
     */
    public List<GameObject> getSerializedObject() {
        return sceneData.gameObjectByUUIDs().values().stream().filter(GameObject::isSerialize).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<GameObject> getRootGameObjects() {
        return new ArrayList<>(sceneData.rootGameObjects());
    }

    public GameObject getGameObject(int id) {
        UUID uuid = sceneData.cachedIDs().get(id);
        if (uuid != null) return sceneData.gameObjectByUUIDs().get(uuid);

        return null;
    }

    public GameObject getGameObject(UUID objectUUID) {
        return sceneData.gameObjectByUUIDs().get(objectUUID);
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

    public Viewport viewport() {
        return sceneData.viewport();
    }

    public void imgui() {
        sceneLoader.imgui();
    }

    public Physic2D getPhysic2D() {
        return sceneData.physic2D();
    }

    public UUID sceneUUID() {
        return sceneUUID;
    }

    public void saveLevel() {
        SceneManager.saveCurrentScene();
    }

    public void loadLevel() {
        SceneManager.loadScene(this);
    }

    /**
     * Exclusive method for scene manager to load the deserialized data of this scene.
     * @param file the file data to load from
     */
    void loadDataFromFile(SceneFile file) {
        if (file == null || file.objects().isEmpty()) return;
        sceneUUID = file.uuid();
        List<GameObject> objects = file.objects();
        objects.forEach(go -> addObjToScene(go, null));
        objects.forEach(go -> go.restoreHierarchy(this));
    }
}
