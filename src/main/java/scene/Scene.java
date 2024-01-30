package scene;

import TCB_Field.GameObjDeSerializer;
import TCB_Field.GameObject;
import TCB_Field.Transform;
import TCB_Field.Viewport;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.CompDeSerializer;
import components.Component;
import render.Renderer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public abstract class Scene {
    protected Renderer renderer = new Renderer();
    protected Viewport viewport;
    private boolean isOn = false;
    protected List<GameObject> gObjects = new ArrayList<>();

    protected boolean isLoaded = false;

    public Scene() {}

    public void init() {}

    public void start() {
        for (GameObject go: gObjects) {
            go.start();
            this.renderer.add(go);
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
        }
    }

    public GameObject loadGameObj(int gObjectID) {
        Optional<GameObject> result = this.gObjects.stream().
                filter(gameObject -> gameObject.loadUid() == gObjectID).
                findFirst();

        return result.orElse(null);
    }

    public abstract void update(float dt);
    public abstract void render();

    public Viewport viewport() {
        return this.viewport;
    }

    public void imgui() {}

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

            this.isLoaded = true;
        }
    }
}
