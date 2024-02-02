package scene;

import TCB_Field.GameObjDeSerializer;
import TCB_Field.GameObject;
import TCB_Field.Transform;
import TCB_Field.Viewport;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class Scene {
    private Renderer renderer;
    private Viewport viewport;
    private boolean isOn;
    private List<GameObject> gObjects;
    private SceneInit sceneInit;
    private FlatPhysic flatPhysic;

    public Scene(SceneInit sceneInit) {
        this.sceneInit = sceneInit;
        this.flatPhysic = new FlatPhysic();
        this.renderer = new Renderer();
        this.gObjects = new ArrayList<>();
        this.isOn = false;
    }

    public void init() {
        this.viewport = new Viewport(new Vector2f(0, 0)); //View point position
        this.sceneInit.loadRes(this);
        this.sceneInit.init(this);
    }

    public void start() {
        for (int i = 0; i < gObjects.size(); i++) {
            GameObject go = gObjects.get(i);

            go.start();
            this.renderer.add(go);
            this.flatPhysic.add(go);
        }
        isOn = true;
    }

    public void addObjToScene(GameObject go) {
        if (!isOn) {
            gObjects.add(go);
        } else {
            gObjects.add(go);
            go.start();
            this.renderer.add(go);
            this.flatPhysic.add(go);
        }
    }

    public void end() {
        for (GameObject go: gObjects) {
            go.destroy();
        }
    }

    public List<GameObject> loadGameObjects() {
        return this.gObjects;
    }

    public GameObject loadGameObj(int gObjectID) {
        Optional<GameObject> result = this.gObjects.stream().
                filter(gameObject -> gameObject.loadUid() == gObjectID).
                findFirst();

        return result.orElse(null);
    }

    public void updateEditor(float dt) {
        this.viewport.adjustProjection();

        for (int i = 0; i < gObjects.size(); i++) {
            GameObject go = gObjects.get(i);
            go.updateEditor(dt);

            if (go.isGone()) {
                gObjects.remove(i);
                this.renderer.destroyGameObj(go);
                this.flatPhysic.destroyGameObj(go);

                i--; //step back if destroyed something
            }
        }
    }

    public void update(float dt) {
        this.viewport.adjustProjection();
        this.flatPhysic.update(dt);

        for (int i = 0; i < gObjects.size(); i++) {
            GameObject go = gObjects.get(i);
            go.update(dt);

            if (go.isGone()) {
                gObjects.remove(i);
                this.renderer.destroyGameObj(go);
                this.flatPhysic.destroyGameObj(go);

                i--; //step back if destroyed something
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

    public void saveLevel() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .create();

        try {
            FileWriter writer = new FileWriter("level.tcb");
            List<GameObject> serializeList = new ArrayList<>();
            for (GameObject obj : this.gObjects) {
                if (obj.isSerialize()) {
                    serializeList.add(obj);
                }
            }
            writer.write(gson.toJson(serializeList));
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
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
            e.printStackTrace();
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
        }
    }
}
