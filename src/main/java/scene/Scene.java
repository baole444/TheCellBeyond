package scene;

import TCB_Field.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.CompDeSerializer;
import components.Component;
import org.joml.Vector2f;
import physic_2d.FlatPhysic;
import render.Renderer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_P;
import static org.lwjgl.glfw.GLFW.GLFW_MOD_CONTROL;

public class Scene {
    private Renderer renderer;
    private Viewport viewport;
    private boolean isSceneOn;
    private List<GameObject> gameObjects;
    private FlatPhysic flatPhysic;
    private boolean isFileLoaded = false;

    private SceneInit sceneInit;

    public Scene(SceneInit sceneInit) {
        this.sceneInit = sceneInit;
        this.flatPhysic = new FlatPhysic();
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
            this.renderer.add(go);
            this.flatPhysic.add(go);
        }
        isSceneOn = true;
    }

    public void addObjToScene(GameObject go) {
        if (!isSceneOn) {
            gameObjects.add(go);
        } else {
            gameObjects.add(go);
            go.start();
            this.renderer.add(go);
            this.flatPhysic.add(go);
        }
    }

    public void destroy() {
        for (GameObject go : gameObjects) {
            go.destroy();
        }
    }

    public List<GameObject> getGameObject() {
        return this.gameObjects;
    }

    public GameObject loadGameObj(int gObjectID) {
        Optional<GameObject> result = this.gameObjects.stream().
                filter(gameObject -> gameObject.loadUid() == gObjectID).
                findFirst();

        return result.orElse(null);
    }

    public void editorUpdate(float dt) {
        this.viewport.adjustProjection();

        // log the list of game object
        //if (KeyListener.isKeyPressed(GLFW_KEY_P, GLFW_MOD_CONTROL)) {
        //    logGameObjects();
        //}

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.editorUpdate(dt);

            if (go.isGone()) {
                // Debug output
                //System.out.println("Current size of Object list is " + gameObjects.size());
                //System.out.println("A request to end an object's rendering is called at position: " + i + " This one is from updateEditor.\n > Object uid is " + go.loadUid());

                gameObjects.remove(i);
                this.renderer.destroyObject(go);
                this.flatPhysic.destroyObject(go);

                i --; // Step back if remove
            }
        }
    }

    public void update(float dt) {
        this.viewport.adjustProjection();
        this.flatPhysic.update(dt);

        for (int i = 0; i < gameObjects.size(); i++) {
            GameObject go = gameObjects.get(i);
            go.update(dt);

            if (go.isGone()) {
                System.out.println("A request to end an object's rendering is called at position: " + i + " This one is from update");
                gameObjects.remove(i);
                this.renderer.destroyObject(go);
                this.flatPhysic.destroyObject(go);

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

    public void logGameObjects() {
        System.out.println("Logging all game objects");
        List<GameObject> allObj = getGameObject();
        Collections.sort(allObj, Comparator.comparingInt(GameObject::loadUid));

        allObj.forEach(go -> System.out.println(go));
        System.out.println("\n");
    }

    public void saveLevel() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .create();

        try {
            FileWriter writer = new FileWriter("level.tcb");
            List<GameObject> serializeList = new ArrayList<>();
            for (GameObject obj : this.gameObjects) {
                if (obj.isSerialize()) {
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

    public void loadLevel() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .create();

        String loadFile = "";
        try {
            loadFile = new String(Files.readAllBytes(Paths.get("level.tcb")));
        } catch (IOException e) {
            //e.printStackTrace();
            System.out.println("No level file found, generating new file...");
            saveLevel();
            System.out.println("File created.");
            return;
        }

        if (!loadFile.equals("")) {
            int maxObjID = -1;
            int maxCompID = -1;

            GameObject[] objs = gson.fromJson(loadFile, GameObject[].class);
            for (int i = 0; i < objs.length; i++) {
                addObjToScene(objs[i]);

                for (Component c : objs[i].loadAllComp()) {
                    if (c.loadUID() > maxCompID) {
                        maxCompID = c.loadUID();
                    }
                }

                if (objs[i].loadUid() > maxObjID) {
                    maxObjID = objs[i].loadUid();
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
