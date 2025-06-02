package scene;

import TheCellBeyond.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.CompDeSerializer;
import components.Component;
import components.SpriteRender;
import org.joml.Vector2f;
import physic2d.Physic2D;
import render.Renderer;
import utility.PathResolver;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static editor.project.Project.CurrentProject;
import static editor.project.Project.ProjectRoot;

public class Scene {
    private Renderer renderer;
    private Viewport viewport;
    private boolean isSceneOn;
    private List<GameObject> gameObjects;
    private Physic2D physic2D;
    private boolean isFileLoaded = false;

    private SceneInit sceneInit;

    public Scene(SceneInit sceneInit) {
        this.sceneInit = sceneInit;
        this.physic2D = new Physic2D();
        this.renderer = new Renderer();
        this.gameObjects = new ArrayList<>();
        this.isSceneOn = false;
    }

    public void init() {
        // View point position
        this.viewport = new Viewport(new Vector2f(0, 0));

        // Load resource, maintain init method
        this.sceneInit.loadResource(this);
        this.sceneInit.init(this);
    }

    public void start() {
        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.start();
            this.renderer.queueObjectForAddition(go);
            this.physic2D.add(go);
        }
        isSceneOn = true;
    }

    public void addObjToScene(GameObject go) {
        if (!isSceneOn) {
            gameObjects.add(go);
        } else {
            gameObjects.add(go);
            go.start();
            this.renderer.queueObjectForAddition(go);
            this.physic2D.add(go);
        }
    }

    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
        }
    }

    public List<GameObject> getGameObjects() {
        return this.gameObjects;
    }

    public GameObject getGameObject(int gObjectID) {
        Optional<GameObject> result = this.gameObjects.stream().
                filter(gameObject -> gameObject.getUID() == gObjectID).
                findFirst();

        return result.orElse(null);
    }

    public void editorUpdate(float dt) {
        this.viewport.adjustProjection();

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.editorUpdate(dt);

            if (go.isRemoved()) {

                gameObjects.remove(i);
                this.renderer.queueObjectForRemoval(go);
                this.physic2D.destroyObject(go);

                i --; // Step back if remove
            }
        }
    }

    public void update(float dt) {
        this.viewport.adjustProjection();
        this.physic2D.update(dt);

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.update(dt);

            if (go.isRemoved()) {
                //System.out.println("A request to end an object's rendering is called at position: " + i + " This one is from update");
                gameObjects.remove(i);
                this.renderer.queueObjectForRemoval(go);
                this.physic2D.destroyObject(go);

                i --; // Step back if remove
            }
        }
    }
    public void render() {
        this.renderer.render();
    }

    public Viewport viewport() {
        return this.viewport;
    }

    public void imgui() {
        this.sceneInit.imgui();
    }

    public GameObject generateObject(String name) {
        GameObject obj = new GameObject(name);
        obj.addComponent(new Transform());
        obj.transform = obj.getComponent(Transform.class);

        return obj;
    }

    public Physic2D getFlatPhysic() {
        return this.physic2D;
    }

    public Renderer getRenderer() {
        return renderer;
    }

    public void logGameObjects() {
        System.out.println("Logging all game objects");
        List<GameObject> allObj = getGameObjects();
        Collections.sort(allObj, Comparator.comparingInt(GameObject::getUID));

        allObj.forEach(go -> System.out.println(go));
        System.out.println("\n");
    }

    // TODO: Remove project root from saveLevel function
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
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .enableComplexMapKeySerialization()
                .create();

        try {
            FileWriter writer = new FileWriter(resolvedPath);
            List<GameObject> serializeList = new ArrayList<>();
            for (GameObject obj : this.gameObjects) {
                if (obj.isSerialize()) {
                    if (CurrentProject != null && ProjectRoot != null && currentSceneName != null) {
                        if (obj.getComponent(SpriteRender.class) != null) {
                            String texturePath = obj.getComponent(SpriteRender.class).getTexture().getFilePath();
                            String relativePath = PathResolver.resolveToRelative(ProjectRoot, texturePath);
                            obj.getComponent(SpriteRender.class).getTexture().setFilePath(relativePath);
                        }
                    }

                    serializeList.add(obj);
                }
            }
            writer.write(gson.toJson(serializeList));
            writer.close();
        } catch (IOException e) {
            System.out.println("Failed to save level, please check following stack trace for more info.");
            System.out.println("_______________________________________________________________________\n");
            e.printStackTrace();
            System.out.println("\n_______________________________________________________________________\n");
        }
    }

    // TODO: attach project root when loading level
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
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String loadFile = "";
        try {
            loadFile = new String(Files.readAllBytes(Paths.get(resolvedPath)));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("No level file found, generating new file...");
            saveLevel();
            System.out.println("File created.");
            return;
        }

        if (!loadFile.isEmpty()) {
            int maxObjID = -1;
            int maxCompID = -1;

            GameObject[] objs = gson.fromJson(loadFile, GameObject[].class);
            for (int i = 0; i < objs.length; i++) {
                // adjust the object path here, probably

                if (CurrentProject != null && ProjectRoot != null && currentSceneName != null) {
                    String texturePath = objs[i].getComponent(SpriteRender.class).getTexture().getFilePath();
                    String absPath = PathResolver.resolveToAbsolute(ProjectRoot, texturePath);
                    objs[i].getComponent(SpriteRender.class).getTexture().setFilePath(absPath);
                }

                addObjToScene(objs[i]);

                for (Component c : objs[i].getComponents()) {
                    if (c.getUID() > maxCompID) {
                        maxCompID = c.getUID();
                    }
                }

                if (objs[i].getUID() > maxObjID) {
                    maxObjID = objs[i].getUID();
                }
            }

            maxObjID++;
            maxCompID++;
            GameObject.init(maxObjID);
            Component.init(maxCompID);

            this.isFileLoaded = true;
        }
    }
}
