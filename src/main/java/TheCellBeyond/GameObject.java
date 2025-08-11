package TheCellBeyond;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.ComponentSerializer;
import components.Component;
import components.IsNotSerialized;
import components.SpriteRenderer;
import imgui.ImGui;
import render.Texture;
import scene.Scene;
import utility.IdPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class GameObject {
    private static final IdPool idCounter = new IdPool(1, true);

    // This is auto managed for shader compatibility, not serialized
    private transient int cachedID = -1;

    private String uuid;
    public String name;

    // During production, this might cause performance impact
    // when user add component to an object.
    // During runtime, most of the time will be spent reading this component list,
    // Thia tradeoff is acceptable.
    private final CopyOnWriteArrayList<Component> components;

    // Cached component that might be used for referencing
    private final transient Map<String, Component> namedComponents = new ConcurrentHashMap<>();

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
        this.components = new CopyOnWriteArrayList<>();
        this.children = new LinkedHashSet<>();
        this.uuid = UUID.randomUUID().toString();
        this.cachedID = idCounter.newId();
    }

    public GameObject getChild(String childName) {
        for (GameObject child : children) {
            if (child.name.equals(childName)) {
                return child;
            }
        }

        return null;
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

    public Component findComponentByName(String componentName) {
        Component c = namedComponents.get(componentName);
        if (c != null) return c;

        for (GameObject child : children) {
            c = child.findComponentByName(componentName);
            if (c != null) return c;
        }

        return null;
    }

    public Component resolveComponentPath(String path) {
        if (path == null || path.isEmpty()) return null;

        // From root
        if (path.startsWith("/")) {
            GameObject root = getRoot();
            return root.resolveComponentPath(path.substring(1));
        }

        // Relative to parent
        if (path.startsWith("../")) {
            if (parent == null) return null;
            return parent.resolveComponentPath(path.substring(3));
        }

        // Check if there are still more Objects to traverse
        int slashIndex = path.indexOf("/");
        if (slashIndex > 0) {
            String childName = path.substring(0, slashIndex);
            String remains = path.substring(slashIndex + 1);

            GameObject child = getChild(childName);
            if (child == null) return null;
            return child.resolveComponentPath(remains);
        }

        // We are now at the final GameObject of this path
        Component component = namedComponents.get(path);
        if (component != null) return component;

        // No component of given name, assumed it is class name
        for (Component c : components) {
            if (c.getClass().getSimpleName().equals(path)) {
                return c;
            }
        }

        return null;
    }

    public <Obj extends Component> Obj getFirstComponent(Class<Obj> componentClass) {
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

    /**
     * Get components of a given type from this Object.
     * @param componentClass the class of the components.
     * @return list of the components.
     * @param <T> Type of the components.
     */
    public <T extends Component> List<T> getComponents(Class<T> componentClass) {
        return components.stream()
                .filter(c -> componentClass.isAssignableFrom(c.getClass()))
                .map(componentClass::cast)
                .collect(Collectors.toList());
    }

    /**
     * Remove components of a given type from this Object.
     * @param componentClass the class of the components.
     * @param <T> Type of the component.
     */
    public <T extends Component> void removeComponents(Class<T> componentClass) {
        components.removeIf(c -> componentClass.isAssignableFrom(c.getClass()));
    }

    /**
     * Remove a component from this Object.
     * @param component the component to be removed.
     * @return true if removed.
     * @param <T> Type of the component.
     */
    public <T extends Component> boolean removeComponent(T component) {
        return components.remove(component);
    }

    public void addComponent(Component c) {
        if (c == null) return;

        if (c.getUUID() == null) {
            c.setUUID(UUID.randomUUID().toString());
        }

        this.components.add(c);
        c.gameObject = this;

        if (c.getComponentName() != null && !c.getComponentName().isEmpty()) {
            namedComponents.put(c.getComponentName(), c);
        }
    }

    public void onComponentNameChanged(Component component, String name) {
        namedComponents.values().removeIf(c -> c == component);

        if (name != null && !name.isEmpty()) {
            namedComponents.put(name, component);
        }
    }

    public void editorUpdate(float dt) {
        for (Component component : components) {
            component.editorUpdate(dt);
        }
    }

    public void update(float dt) {
        for (Component component : components) {
            component.update(dt);
        }
    }

    public void start() {
        for (Component component : components) {
            component.start();
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
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String oJson = gson.toJson(this);
        GameObject obj = gson.fromJson(oJson, GameObject.class);

        obj.uuid = UUID.randomUUID().toString();
        obj.cachedID = idCounter.newId();

        for (Component c : obj.getComponents()) {
            c.setUUID(UUID.randomUUID().toString());

            if (c.getComponentName() != null && !c.getComponentName().isEmpty()) {
                obj.namedComponents.put(c.getComponentName(), c);
            }
        }

        SpriteRenderer sprite = obj.getFirstComponent(SpriteRenderer.class);

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

    // TODO: Investigate the effectiveness of regenerate uid
    public void regenerateUID() {
        idCounter.releaseId(cachedID);
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