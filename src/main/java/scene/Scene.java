package scene;

import TheCellBeyond.*;
import TheCellBeyond.internal.DataSnapshot;
import TheCellBeyond.internal.LogicServer;
import TheCellBeyond.internal.RenderingSnapshot;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.ComponentSerializer;
import components.Component;
import components.IsNotSelectable;
import editor.components.EditorObjectIndicator;
import editor.dialog.SaveSceneAsDialog;
import eventviewer.EngineEventCallback;
import eventviewer.event.SceneEvent;
import project.Project;
import imgui.type.ImBoolean;
import physic2d.Physic2D;
import render.Renderer;
import utility.PathResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Scene {
    private final SceneLoader sceneLoader;
    private transient final ImBoolean isSceneOn;
    private final DataSnapshot sceneData;

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
        isSceneOn = new ImBoolean(false);
    }

    public void init() {
        sceneLoader.loadResource(this);
        sceneLoader.onSceneEntered(this);
    }

    public void start() {
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        updateQueues();

        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.start();
            sceneData.physic2D().add(go);
            cacheComponents(go);
        }

        isSceneOn.set(true);
    }

    public void editorStart() {
        EngineEventCallback.emit(new SceneEvent(SceneEvent.Type.SceneEntered, this));
        updateQueues();

        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.editorStart();
            cacheComponents(go);
        }

        isSceneOn.set(true);
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
        if (go == null) return;

        if (sceneData.gameObjectByUUIDs().containsKey(go.getUUID())) return;

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
            EditorObjectIndicator editorObjectIndicator = new EditorObjectIndicator();
            go.addComponent(editorObjectIndicator);
        }

        if (isSceneOn.get()) {
            go.start();
            sceneData.physic2D().add(go);
            cacheComponents(go);
        }

        for (GameObject child : go.getChildren()) {
            if (!sceneData.gameObjectByUUIDs().containsKey(child.getUUID())) queueForObjectAddition(child, go);
        }
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
        sceneData.markComponentForRemove(component);
        component.destroy();
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
            sceneData.markObjectForRemove(descendant);
            sceneData.physic2D().destroyObject(descendant);
            uncacheComponents(descendant);
        }

        sceneData.cachedIDs().remove(go.getUID());
        sceneData.gameObjectByUUIDs().remove(go.getUUID());
        sceneData.markObjectForRemove(go);
        sceneData.physic2D().destroyObject(go);
        uncacheComponents(go);
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
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();

        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.editorUpdate(dt);

            if (go.isRemoved()) queueObjectForRemoval(go);
        }

        updateQueues();
        sceneData.updated().set(true);

        RenderingSnapshot snapshot = sceneData.extractRenderData();
        if (snapshot != null) Renderer.get().queueSnapshot(snapshot);
    }

    public void update(float dt) {
        sceneData.updated().set(false);
        sceneData.viewport().adjustProjection();
        sceneData.physic2D().update(dt);

        for (GameObject go : sceneData.gameObjectByUUIDs().values()) {
            go.update(dt);

            if (go.isRemoved()) queueObjectForRemoval(go);
        }

        updateQueues();
        sceneData.updated().set(true);

        RenderingSnapshot snapshot = sceneData.extractRenderData();
        if (snapshot != null) Renderer.get().queueSnapshot(snapshot);
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

    public void saveLevel() {
        String currentSceneName = LogicServer.currentSceneName();
        if (currentSceneName == null) {
            SaveSceneAsDialog.show(this::saveLevel);
            return;
        }

        String resolvedPath = PathResolver.resolveToAbsolute(Project.projectRoot(), Project.currentProject().scenes().get(currentSceneName).path());

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        try {
            FileWriter writer = new FileWriter(resolvedPath);
            List<GameObject> serializeList = new ArrayList<>();
            for (GameObject obj : sceneData.gameObjectByUUIDs().values()) {
                if (obj.isSerialize() && Project.currentProject() != null && Project.projectRoot() != null) {
                    obj.prepareForSerialization();
                    serializeList.add(obj);
                }
            }
            writer.write(gson.toJson(serializeList));
            writer.close();
        } catch (IOException e) {
            System.err.println("Failed to save level!");
            System.err.println("_______________________________________________________________________\n");
            System.err.println(e.getMessage());
            System.err.println("\n_______________________________________________________________________\n");
        }
    }

    public void loadLevel() {
        String currentSceneName = LogicServer.currentSceneName();

        if (currentSceneName == null) return;

        String resolvedPath = PathResolver.resolveToAbsolute(Project.projectRoot(), Project.currentProject().scenes().get(currentSceneName).path());

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String loadFile;
        try {
            loadFile = new String(Files.readAllBytes(Paths.get(resolvedPath)));
        } catch (IOException e) {
            System.out.println("No level file found, generating new file...");
            saveLevel();
            System.out.println("File created.");
            return;
        }

        if (!loadFile.isEmpty()) {
            GameObject[] objects = gson.fromJson(loadFile, GameObject[].class);
            for (GameObject go : objects) {
                addObjToScene(go, null);
            }

            for (GameObject go : objects) {
                go.restoreHierarchy(this);
            }
        }
    }
}
