package TCB_Field;

import java.util.ArrayList;
import java.util.List;

public abstract class Scene {

    protected  Viewport viewport;
    private boolean isOn = false;

    protected List<GameObject> gObjects = new ArrayList<>();

    public Scene() {}

    public void init() {}

    public void start() {
        for (GameObject go: gObjects) {
            go.start();
        }
        isOn = true;
    }

    public void addObjToScene(GameObject go) {
        if (!isOn) {
            gObjects.add(go);
        } else {
            gObjects.add(go);
            go.start();
        }
    }

    public abstract void update(float dt);
}
