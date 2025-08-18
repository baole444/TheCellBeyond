package scene;

import TheCellBeyond.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.ComponentSerializer;
import components.Component;
import components.IsNotSelectable;
import components.SpriteRenderer;
import editor.Indicator;
import org.joml.Vector2f;
import physic2d.Physic2D;
import render.Renderer;
import utility.PathResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class Scene {
    private final Renderer renderer;
    private Viewport viewport;
    private boolean isSceneOn;
    private final Map<Integer, String> cachedIDs;
    private final Map<String, GameObject> gameObjectByUUIDs;
    private final List<GameObject> rootGameObjects;
    private final List<GameObject> addedGameObjects;
    private final List<GameObject> removedGameObjects;
    private final List<Component> removedComponents;
    private final HashMap<GameObject, GameObject> addedGameObjectWithParents;
    private final Map<String, Component> componentsByUUID;

    private final Physic2D physic2D;

    private final SceneInit sceneInit;

    public Scene(SceneInit sceneInit) {
        this.sceneInit = sceneInit;

        physic2D = new Physic2D();
        renderer = new Renderer();

        cachedIDs = new HashMap<>();
        gameObjectByUUIDs = new HashMap<>();
        rootGameObjects = new ArrayList<>();

        addedGameObjects  = new ArrayList<>();
        removedGameObjects = new ArrayList<>();
        removedComponents = new ArrayList<>();
        addedGameObjectWithParents = new HashMap<>();

        componentsByUUID = new HashMap<>();

        isSceneOn = false;
    }

    public void init() {
        // View point position
        viewport = new Viewport(new Vector2f(0, 0));

        // Load resource, maintain init method
        sceneInit.loadResource(this);
        sceneInit.init(this);
    }

    public void start() {
        updateQueues();

        for (GameObject go : gameObjectByUUIDs.values()) {
            go.start();
            this.renderer.queueObjectForAddition(go);
            this.physic2D.add(go);
            cacheComponents(go);
        }

        isSceneOn = true;
    }

    private void cacheComponents(GameObject go) {
        for (Component c : go.getComponents()) {
            componentsByUUID.put(c.getUUID(), c);
        }
    }

    private void uncacheComponents(GameObject go) {
        for (Component c : go.getComponents()) {
            componentsByUUID.remove(c.getUUID());
        }
    }

    public Component getComponentByUUID(String uuid) {
        return componentsByUUID.get(uuid);
    }

    public void queueForObjectAddition(GameObject go, GameObject parent) {
        if (go == null) return;

        if (!addedGameObjects.contains(go) && !gameObjectByUUIDs.containsKey(go.getUUID())) {
            addedGameObjects.add(go);
            if (parent != null) {
                addedGameObjectWithParents.put(go, parent);
            }
        }
    }

    private void addObjToScene(GameObject go, GameObject parent) {
        if (go == null) return;

        if (gameObjectByUUIDs.containsKey(go.getUUID())) return;

        cachedIDs.put(go.getUID(), go.getUUID());
        gameObjectByUUIDs.put(go.getUUID(), go);

        if (parent != null) {
            parent.addChild(go);
        }

        if (go.getParentUUID() == null && !rootGameObjects.contains(go)) rootGameObjects.add(go);

        if (go.isSerialize() &&
                go.getFirstComponent(IsNotSelectable.class) == null &&
                go.getFirstComponent(Indicator.class) == null &&
                go instanceof GameObject2D
        ) {
            Indicator indicator = new Indicator();
            go.addComponent(indicator);
        }

        if (isSceneOn) {
            go.start();
            this.renderer.queueObjectForAddition(go);
            this.physic2D.add(go);
            cacheComponents(go);
        }

        for (GameObject child : go.getChildren()) {
            if (!gameObjectByUUIDs.containsKey(child.getUUID())) queueForObjectAddition(child, go);
        }
    }

    public void queueForObjectAddition(GameObject go) {
        queueForObjectAddition(go, null);
    }

    public void queueObjectForRemoval(GameObject go) {
        if (go == null) return;

        if (!removedGameObjects.contains(go) && gameObjectByUUIDs.containsKey(go.getUUID())) {
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

        componentsByUUID.remove(component.getUUID());

        renderer.queueComponentForRemoval(component);
    }

    private void removeObjFromScene(GameObject go) {
        if (go == null) return;

        if (go.getParent() != null) {
            go.getParent().removeChild(go);
        } else {
            rootGameObjects.remove(go);
        }

        List<GameObject> descendants = go.getAllDescendants();
        for (GameObject descendant : descendants) {
            cachedIDs.remove(descendant.getUID());
            gameObjectByUUIDs.remove(descendant.getUUID());
            renderer.queueObjectForRemoval(descendant);
            physic2D.destroyObject(descendant);
            uncacheComponents(descendant);
        }

        cachedIDs.remove(go.getUID());
        gameObjectByUUIDs.remove(go.getUUID());
        renderer.queueObjectForRemoval(go);
        physic2D.destroyObject(go);
        uncacheComponents(go);
    }

    public boolean reparentObject(GameObject child, GameObject newParent) {
        if (child == null) return false;

        // cannot reparent oneself to oneself
        if (newParent != null && (child == newParent || child.isAncestor(newParent))) {
            return false;
        }

        if (child.getParent() == null) rootGameObjects.remove(child);

        child.setParent(newParent);

        if (newParent == null && !rootGameObjects.contains(child)) {
            rootGameObjects.add(child);
        }

        return true;
    }

    public void destroy() {
        for (GameObject go : gameObjectByUUIDs.values()) {
            go.destroy();
        }

        gameObjectByUUIDs.clear();
        rootGameObjects.clear();
        removedGameObjects.clear();
        removedComponents.clear();
        addedGameObjects.clear();
        addedGameObjectWithParents.clear();
        componentsByUUID.clear();
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

    public Map<String, GameObject> getGameObjects() {
        return this.gameObjectByUUIDs;
    }

    public List<GameObject> getSerializedObject() {
        return gameObjectByUUIDs.values().stream().filter(GameObject::isSerialize).collect(Collectors.toCollection(ArrayList::new));
    }

    public List<GameObject> getRootGameObjects() {
        return new ArrayList<>(rootGameObjects);
    }

    public GameObject getGameObject(int id) {
        String uuid = cachedIDs.get(id);

        if (uuid != null) return gameObjectByUUIDs.get(uuid);

        return null;
    }

    public GameObject getGameObject(String objectUUID) {
        return gameObjectByUUIDs.get(objectUUID);
    }

    public void editorUpdate(float dt) {
        viewport.adjustProjection();

        for (GameObject go : gameObjectByUUIDs.values()) {
            go.editorUpdate(dt);

            if (go.isRemoved()) queueObjectForRemoval(go);
            else if (go.isDirty()) {
                renderer.queueObjectForUpdate(go);
                go.setDirty(false);
            }
        }

        updateQueues();
    }

    public void update(float dt) {
        viewport.adjustProjection();
        physic2D.update(dt);

        for (GameObject go : gameObjectByUUIDs.values()) {
            go.update(dt);

            if (go.isRemoved()) queueObjectForRemoval(go);
            else if (go.isDirty()) {
                renderer.queueObjectForUpdate(go);
                go.setDirty(false);
            }
        }

        updateQueues();
    }

    public void render() {
        renderer.setMatrices(viewport.getProjectionMatrix(), viewport.getViewMatrix());
        renderer.render();
    }

    public Viewport viewport() {
        return viewport;
    }

    public void imgui() {
        sceneInit.imgui();
    }

    public Physic2D getPhysic2D() {
        return physic2D;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void saveLevel() {
        String currentSceneName = Window.getCurrentSceneName();
        String resolvedPath;
        if (currentSceneName != null) {
            resolvedPath = PathResolver.resolveToAbsolute(ProjectRoot, CurrentProject.getScenes().get(currentSceneName).getPath());
        } else {
            resolvedPath = "untitled.cell";
        }

        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        try {
            FileWriter writer = new FileWriter(resolvedPath);
            List<GameObject> serializeList = new ArrayList<>();
            for (GameObject obj : this.gameObjectByUUIDs.values()) {
                if (obj.isSerialize() && CurrentProject != null && ProjectRoot != null && currentSceneName != null) {
                    obj.prepareForSerialization();

                    if (obj.getFirstComponent(SpriteRenderer.class) != null) {
                        PathResolver resolver = PathResolver.get();

                        List<SpriteRenderer> sps = obj.getComponents(SpriteRenderer.class);

                        for (SpriteRenderer sprite : sps) {
                            String texturePath = sprite.getTexture().getFilePath();
                            String canonicalPath = resolver.toCanonicalPath(texturePath);

                            sprite.getTexture().setFilePath(canonicalPath);
                        }
                    }

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
        String currentSceneName = Window.getCurrentSceneName();
        String resolvedPath;
        if (currentSceneName != null) {
            resolvedPath = PathResolver.resolveToAbsolute(ProjectRoot, CurrentProject.getScenes().get(currentSceneName).getPath());
        } else {
            resolvedPath = "untitled.cell";
        }

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
                if (CurrentProject != null && ProjectRoot != null && currentSceneName != null) {
                    if (go.getFirstComponent(SpriteRenderer.class) != null) {
                        String canonicalPath = go.getFirstComponent(SpriteRenderer.class).getTexture().getFilePath();

                        go.getFirstComponent(SpriteRenderer.class).getTexture().setFilePath(canonicalPath);
                    }
                }

                addObjToScene(go, null);
            }

            for (GameObject go : objects) {
                go.restoreHierarchy(this);
            }
        }
    }
}
