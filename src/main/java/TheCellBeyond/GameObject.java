package TheCellBeyond;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.CompDeSerializer;
import components.Component;
import components.IsNotSerialized;
import components.SpriteRenderer;
import imgui.ImGui;
import render.Texture;
import scene.Scene;
import utility.IdPool;

import java.util.*;

public class GameObject {
    private static final IdPool idCounter = new IdPool(1, true);

    // This is auto managed for shader compatibility, not serialized
    private transient int cachedID = -1;

    private String uuid;
    public String name;
    private final List<Component> components;

    @Deprecated(since = "0.1", forRemoval = true)
    public transient Transform transform;

    private boolean isSerialize = true;
    private boolean isRemoved = false;

    private transient GameObject parent;
    private final transient LinkedHashSet<GameObject> children;

    private String parentUUID;
    private List<String> childrenUUIDs;

    public GameObject(String name) {
        this.name = name;
        this.components = new ArrayList<>();
        this.children = new LinkedHashSet<>();
        this.uuid = UUID.randomUUID().toString();
        this.cachedID = idCounter.newId();
    }

    public void addChild(GameObject child) {
        if (child == null || child == this) return;

        // Circular references check.
        if (isAncestor(child)) {
            System.err.println("Cannot add parent as child.");
            return;
        }

        // Remove from previous parent.
        if (child.parent != null) {
            child.parent.removeChild(child);
        }

        children.add(child);

        child.parent = this;
        child.parentUUID = this.uuid;

        // Update serialization data
        updateChildrenUUIDs();
    }

    public void removeChild(GameObject child) {
        if (child == null) return;

        children.remove(child);
        if (child.parent == this) {
            child.parent = null;
            child.parentUUID = null;
        }

        updateChildrenUUIDs();
    }

    public void setParent(GameObject newParent) {
        if (newParent != null) {
            newParent.addChild(this);
        } else {
            if (parent != null) {
                parent.removeChild(this);
            }

            this.parent = null;
            this.parentUUID = null;
        }
    }

    public boolean isAncestor(GameObject ancestorAble) {
        GameObject current = this.parent;
        while (current != null) {
            if (current == ancestorAble) {
                return true;
            }

            current = current.parent;
        }

        return false;
    }

    public boolean isRoot() {
        return parent == null;
    }

    public GameObject getRoot() {
        GameObject current = this;
        while (current.parent != null) {
            current = current.parent;
        }

        return current;
    }

    // Recursive list that gets all children and their children and so on.
    public List<GameObject> getAllDescendants() {
        List<GameObject> descendants = new ArrayList<>();
        addDescendantsRecursive(descendants);

        return descendants;
    }

    private void addDescendantsRecursive(List<GameObject> descendants) {
        for (GameObject child : children) {
            descendants.add(child);
            child.addDescendantsRecursive(descendants);
        }
    }

    private void updateChildrenUUIDs() {
        childrenUUIDs = new ArrayList<>();
        for (GameObject child : children) {
            childrenUUIDs.add(child.uuid);
        }
    }

    // TODO: support multiple component by return a list
    public <Obj extends Component> Obj getComponent(Class<Obj> componentClass) {
        for (Component c : components) {
            if (componentClass.isAssignableFrom(c.getClass())) {
                try {
                    return componentClass.cast(c);
                } catch (ClassCastException e) {
                    assert false : "FATAL: Casting component failed.";
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
        if (c == null) return;
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
            if (c instanceof IsNotSerialized) continue;

            if (ImGui.collapsingHeader(c.getClass().getSimpleName()))
                c.imgui();
        }
    }

    public void destroy() {
        this.isRemoved = true;

        if (parent != null) {
            parent.removeChild(this);
        }

        List<GameObject> childrenCopy = new ArrayList<>(children);

        for (GameObject child : childrenCopy) {
            child.destroy();
        }

        for (Component component : components) {
            if (component == null) continue;
            component.destroy();
        }

        if (cachedID > 0) {
            idCounter.releaseId(cachedID);
            cachedID = -1;
        }
    }

    public GameObject copy() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new CompDeSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String oJson = gson.toJson(this);
        GameObject obj = gson.fromJson(oJson, GameObject.class);

        obj.uuid = UUID.randomUUID().toString();
        obj.cachedID = idCounter.newId();

        for (Component c : obj.getComponents()) {
            c.createUID();
        }

        SpriteRenderer sprite = obj.getComponent(SpriteRenderer.class);

        if (sprite != null && sprite.getTexture() != null) {
            Texture ogTexture = sprite.getTexture();
            Texture copy = ogTexture.copy();
            sprite.setTexture(copy);
        }

        return obj;
    }

    public boolean isRemoved() {
        return this.isRemoved;
    }

    public int getUID() {
        return cachedID;
    }

    protected void regenerateUID() {
        cachedID = idCounter.newId();
    }

    public String getUUID() {
        return this.uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public List<Component> getComponents() {
        return this.components;
    }

    public void setNotSerialize() {
        this.isSerialize = false;
    }

    public void setSerialize(boolean isSerialized) {
        this.isSerialize = isSerialized;
    }

    public boolean isSerialize() {
        return this.isSerialize;
    }

    public GameObject getParent() {
        return this.parent;
    }

    public String getParentUUID() {
        return this.parentUUID;
    }

    public void setParentUUID(String uuid) {
        this.parentUUID = uuid;
    }

    public Set<GameObject> getChildren() {
        return this.children;
    }

    public List<String> getChildrenUUIDs() {
        return this.childrenUUIDs;
    }

    public void setChildrenUUIDs(List<String> childrenUUIDs) {
        this.childrenUUIDs = childrenUUIDs;
    }


    public void prepareForSerialization() {
        if (parent != null) {
            parentUUID = parent.uuid;
        }

        updateChildrenUUIDs();
    }

    public void restoreHierarchy(Scene scene) {
        // Parent relationship
        if (parentUUID != null && isParentNotValid()) {
            GameObject parentGO = scene.getGameObject(parentUUID);
            if (parentGO != null) {
                this.parent = parentGO;
                parentGO.children.add(this);
            }
        }

        // Children relationships
        if (childrenUUIDs != null) {
            children.clear();
            for (String childUUID : childrenUUIDs) {
                GameObject childGO = scene.getGameObject(childUUID);
                if (childGO != null) {
                    children.add(childGO);
                    if (childGO.isParentNotValid()) childGO.parent = this;
                }
            }
        }
    }

    private boolean isParentNotValid() {
        // Parent object not yet assigned
        if (parent == null) return true;

        // Parent uuid still match with the object reference
        return !this.parentUUID.equals(parent.uuid);
    }

    @Override
    public String toString() {
        return "Name: " + this.name +
                "\n  UUID: " + this.uuid +
                "\n  isSerialize: " + this.isSerialize +
                "\n  isGone: " + this.isRemoved;
    }
}
