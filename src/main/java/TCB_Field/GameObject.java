package TCB_Field;

import components.Component;
import imgui.ImGui;

import java.util.ArrayList;
import java.util.List;

public class GameObject {
    private static  int ID_COUNTER = 0;
    private int uID = -1;
    private String name;
    private List<Component> components;
    public transient Transform transform;
    private boolean isSerialize = true;

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();

        // TODO: May cause problem when deserializing.
        this.uID = ID_COUNTER++;
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
        c.genId();
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

    public void imgui() {
        for (Component c: components) {
            if (ImGui.collapsingHeader(c.getClass().getSimpleName()))
                c.imgui();
        }
    }

    public static void init(int maxID) {
        ID_COUNTER = maxID;
    }

    public int loadUid() {
        return this.uID;
    }

    public List<Component> loadAllComp() {
        return this.components;
    }

    public void isNotSerialize() {
        this.isSerialize = false;
    }

    public boolean isSerialize() {
        return this.isSerialize;
    }
}
