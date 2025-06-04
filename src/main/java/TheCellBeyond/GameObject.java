package TheCellBeyond;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.CompDeSerializer;
import components.Component;
import components.SpriteRender;
import imgui.ImGui;
import utility.AssetsPool;
import utility.PathResolver;

import java.util.ArrayList;
import java.util.List;

import static editor.project.Project.ProjectRoot;

public class GameObject {
    private static  int ID_COUNTER = 0;
    private int uID = -1;
    public String name;
    private List<Component> components;
    public transient Transform transform;
    private boolean isSerialize = true;
    private boolean isRemoved = false;


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
        c.createUID();
        this.components.add(c);
        c.gameObject = this;
    }

    public void editorUpdate(float dt) {
        for (int i = 0; i < components.size(); i++) {
            components.get(i).editorUpdate(dt);
        }
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

    public void destroy() {
        this.isRemoved = true;
        for (int i = 0; i < components.size(); i++) {
            components.get(i).destroy();
        }
    }

    public GameObject copy() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjDeSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String oJson = gson.toJson(this);
        GameObject obj = gson.fromJson(oJson, GameObject.class);

        obj.createUID();

        for (Component c : obj.getComponents()) {
            c.createUID();
        }

        SpriteRender sprite = obj.getComponent(SpriteRender.class);

        if (sprite != null && sprite.getTexture() != null) {
            String texturePath = sprite.getTexture().getFilePath();
            String absPath = PathResolver.resolveToAbsolute(ProjectRoot, texturePath);
            sprite.setTexture(AssetsPool.loadTexture(absPath));
        }

        return obj;
    }

    public static void init(int maxID) {
        ID_COUNTER = maxID;
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public int getUID() {
        return this.uID;
    }

    public void setUID(int uid) {
        this.uID = uid;
    }

    public void createUID() {
        this.uID = ID_COUNTER++;
    }

    public List<Component> getComponents() {
        return this.components;
    }

    public void setNotSerialize() {
        this.isSerialize = false;
    }

    public boolean isSerialize() {
        return this.isSerialize;
    }

    @Override
    public String toString() {
        return "Name: " + this.name +
                "\n  uID: " + this.uID +
                "\n  isSerialize: " + this.isSerialize +
                "\n  isGone: " + this.isRemoved;
    }
}
