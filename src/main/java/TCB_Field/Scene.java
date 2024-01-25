package TCB_Field;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import imgui.ImGui;
import render.Renderer;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public abstract class Scene {
    protected Renderer renderer = new Renderer();
    protected  Viewport viewport;
    private boolean isOn = false;

    protected List<GameObject> gObjects = new ArrayList<>();

    protected GameObject activeGameObject = null;

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

    public abstract void update(float dt);

    public Viewport viewport() {
        return this.viewport;
    }

    public void sceneImGui() {
        if (activeGameObject != null) {
            ImGui.begin("Loader");
            activeGameObject.imgui();
            ImGui.end();
        }
        imgui();
    }

    public void imgui() {}

    public void saveLevel() {
        Gson gson = new GsonBuilder()
                .setPrettyPrinting()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .create();

        try {
            FileWriter writer = new FileWriter("level.tcb");
            writer.write(gson.toJson(this.gObjects));
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
            GameObject[] objs = gson.fromJson(loadFile, GameObject[].class);
            for (int i = 0; i < objs.length; i++) {
                addObjToScene(objs[i]);
            }
            this.isLoaded = true;
        }
    }
}
