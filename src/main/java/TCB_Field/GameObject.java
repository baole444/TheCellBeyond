package TCB_Field;

import java.util.ArrayList;
import java.util.List;

public class GameObject {

    private String name;
    private List<Component> components;
    public Transform transform;
    private int zIndex;

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();
        this.transform = new Transform();
        this.zIndex = 0;

    }

    public GameObject(String name, Transform transform, int zIndex) {
        this.name = name;
        this.components = new ArrayList<>();
        this.transform = transform;

    }

    public <Obj extends Component> Obj getComponent(Class<Obj> componentClass) {
        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                try {
                    return componentClass.cast(c);
                } catch (ClassCastException e) {
                    assert false : "FATAL: Casting component.";
                }
            }
        }
        return null;
    }

    public <Obj extends Component> void removeComponent(Class<Obj> componentClass) {
        for (int i = 0; i < components.size(); i++) {
            Component c = components.get(i);
            if (componentClass.isAssignableFrom(c.getClass())) {
                components.remove(i);
                return;
            }
        }
    }

    public void addComponent(Component c) {
        this.components.add(c);
        c.gameObject = this;
    }

    public void update(float dt) {
        for (int i = 0; i < components.size(); i++) {
            components.get(i).update(dt);
        }
    }

    public void start() {
        for (int i = 0; i < components.size(); i++) {
            components.get(i).start();
        }
    }

    public int zIndex() {
        return this.zIndex;
    }
}
