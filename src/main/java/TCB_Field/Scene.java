package TCB_Field;

import render.Renderer;

import java.util.ArrayList;
import java.util.List;

public abstract class Scene {

    protected Renderer renderer = new Renderer();
    protected  Viewport viewport;
    private boolean isOn = false;

    protected List<GameObject> gObjects = new ArrayList<>();

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
}
