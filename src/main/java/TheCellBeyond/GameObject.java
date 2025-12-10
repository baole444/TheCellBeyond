package TheCellBeyond;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import components.ComponentSerializer;
import components.Component;
import components.NotSerializeComponent;
import editor.BottomPanel;
import editor.EditorIcons;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTableColumnFlags;
import imgui.flag.ImGuiTableFlags;
import imgui.flag.ImGuiTreeNodeFlags;
import scene.Scene;
import utility.IdPool;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

import static org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_1;

public class GameObject {
    private static final IdPool idCounter = new IdPool(1, true);
    private transient int cachedID;

    // TODO: refactor this to use UUID class
    private String uuid;
    public String name;

    private final CopyOnWriteArrayList<Component> components;
    private final transient Map<String, Component> namedComponents = new ConcurrentHashMap<>();

    private boolean isSerialize = true;
    private transient boolean isRemoved = false;
    private transient boolean isStarted = false;
    private transient boolean isDirty = false;

    private transient GameObject parent;
    private final transient LinkedHashSet<GameObject> children;

    private String parentUUID;
    private List<String> childrenUUIDs;

    public GameObject() {
        String name = GameObject2D.class.getSimpleName();
        this(name);
    }

    public GameObject(String name) {
        this.name = name;
        components = new CopyOnWriteArrayList<>();
        children = new LinkedHashSet<>();
        uuid = UUID.randomUUID().toString();
        cachedID = idCounter.newId();
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
        Scene scene = Window.getScene();
        for (Component component : components) {
            if (componentClass.isAssignableFrom(component.getClass())) {
                if (scene != null) scene.queueForComponentRemoval(component);
            }
        }
        components.removeIf(c -> componentClass.isAssignableFrom(c.getClass()));
    }

    /**
     * Remove a component from this Object.
     * @param component the component to be removed.
     * @return true if removed.
     * @param <T> Type of the component.
     */
    public <T extends Component> boolean removeComponent(T component) {
        boolean removed = components.remove(component);

        if (removed) {
            if (component.getComponentName() != null) namedComponents.remove(component.getComponentName());

            Scene scene = Window.getScene();
            if (scene != null) scene.queueForComponentRemoval(component);

            setDirty(true);

            return true;
        }

        return false;
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

        if (isStarted) c.start();

        if (!isDirty()) setDirty(true);
    }

    public void onComponentNameChanged(Component component, String name) {
        namedComponents.values().removeIf(c -> c == component);

        if (name != null && !name.isEmpty()) {
            namedComponents.put(name, component);
        }
    }

    public void editorUpdate(float dt) {
        additionalUpdateLogic(dt);

        for (Component component : components) {
            component.editorUpdate(dt);
        }
    }

    public void update(float dt) {
        additionalUpdateLogic(dt);

        for (Component component : components) {
            component.update(dt);
        }
    }

    protected void additionalUpdateLogic(float dt) {}

    public void start() {
        isStarted = true;

        for (Component component : components) {
            component.start();
        }

        isDirty = true;
    }

    public final void imgui() {
        name = ImEditorGui.inputText("Name", name, this);
        additionalImGuiLogic();
        ImGui.spacing();
        boolean openComponent = ImGui.collapsingHeader("Components##GO_Components_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openComponent) {
            ImGui.spacing();
            return;
        }
        ImGui.indent();
        for (Component c: components) {
            if (c instanceof NotSerializeComponent) continue;
            String uuid = c.getUUID();
            if (!ImGui.beginTable("##Component_Table_Header_" + uuid, 2, ImGuiTableFlags.SizingFixedFit)) continue;
            ImGui.tableSetupColumn("Component_Header_Column_" + uuid, ImGuiTableColumnFlags.WidthStretch);
            ImGui.tableSetupColumn("Component_Delete_Column_" + uuid, ImGuiTableColumnFlags.WidthFixed);

            ImGui.tableNextColumn();
            String name = c.getComponentName();
            String label = (name == null || name.isBlank()) ? c.getClass().getSimpleName() : name + "##" + uuid;
            ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
            boolean open = ImGui.collapsingHeader(label);
            ImGui.popStyleColor(1);
            if (ImGui.isItemClicked(GLFW_MOUSE_BUTTON_1)) BottomPanel.interacted(c);

            ImGui.tableNextColumn();
            boolean clicked = ImEditorGui.iconButton("Delete##Delete_Component_Button_" + uuid, EditorIcons.Icons.Delete, "Remove this component");
            ImGui.endTable();

            if (clicked) {
                removeComponent(c);
                continue;
            }

            if (!open) continue;
            ImGui.separator();
            c.imgui();
            ImGui.separator();
        }

        ImGui.unindent();
        ImGui.spacing();
    }

    protected void additionalImGuiLogic() {}

    public void destroy() {
        this.isRemoved = true;

        if (parent != null) parent.removeChild(this);

        List<GameObject> childrenCopy = new ArrayList<>(children);

        for (GameObject child : childrenCopy) {
            if (child == null) continue;
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

    public GameObject copy () {
        return copy(false);
    }

    public GameObject copy(boolean copyHierarchy) {
        GameObject copy = copySingleObject();

        boolean isChildrenEmpty = children.isEmpty() && (childrenUUIDs == null || childrenUUIDs.isEmpty());
        if (copyHierarchy && !isChildrenEmpty) copyDescendants(this, copy);

        return copy;
    }

    public boolean isRemoved() {
        return isRemoved;
    }

    public int getUID() {
        return cachedID;
    }

    // TODO: Investigate the effectiveness of regenerate uid
    public void regenerateUID() {
        idCounter.releaseId(cachedID);
        cachedID = idCounter.newId();
        setDirty(true);
    }

    public String getUUID() {
        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public List<Component> getComponents() {
        return components;
    }

    public void setNotSerialize() {
        isSerialize = false;
    }

    public void setSerialize(boolean isSerialized) {
        isSerialize = isSerialized;
    }

    public boolean isSerialize() {
        return isSerialize;
    }

    public GameObject getParent() {
        return parent;
    }

    public String getParentUUID() {
        return parentUUID;
    }

    public void setParentUUID(String uuid) {
        parentUUID = uuid;
    }

    public Set<GameObject> getChildren() {
        return children;
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
        if (parentUUID != null && isParentNotValid()) {
            GameObject parentGO = scene.getGameObject(parentUUID);
            if (parentGO != null) {
                parent = parentGO;
                parentGO.children.add(this);
            }
        }

        if (childrenUUIDs == null) return;
        children.clear();
        for (String childUUID : childrenUUIDs) {
            GameObject childGO = scene.getGameObject(childUUID);
            if (childGO == null) continue;
            children.add(childGO);
            if (childGO.isParentNotValid()) childGO.parent = this;
        }
    }

    private boolean isParentNotValid() {
        // Parent object not yet assigned
        if (parent == null) return true;

        // Parent uuid still match with the object reference
        return !this.parentUUID.equals(parent.uuid);
    }

    protected GameObject copySingleObject() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Component.class, new ComponentSerializer())
                .registerTypeHierarchyAdapter(GameObject.class, new GameObjectSerializer())
                .enableComplexMapKeySerialization()
                .create();

        String oJson = gson.toJson(this);
        GameObject obj = gson.fromJson(oJson, GameObject.class);

        obj.uuid = UUID.randomUUID().toString();
        obj.cachedID = idCounter.newId();

        obj.parent = null;
        obj.parentUUID = null;
        obj.children.clear();
        obj.childrenUUIDs = null;

        for (Component c : obj.getComponents()) {
            c.setUUID(UUID.randomUUID().toString());
            if (c.getComponentName() == null || c.getComponentName().isEmpty()) continue;
            obj.namedComponents.put(c.getComponentName(), c);
        }

        return obj;
    }

    protected void copyDescendants(GameObject source, GameObject target) {
        for (GameObject sourceChild : source.children) {
            GameObject copyChild = sourceChild.copySingleObject();

            target.addChild(copyChild);

            if (!sourceChild.children.isEmpty()) copyDescendants(sourceChild, copyChild);
        }
    }

    @Override
    public String toString() {
        return "Name: " + this.name +
                "\n  UUID: " + this.uuid +
                "\n  isSerialize: " + this.isSerialize +
                "\n  isGone: " + this.isRemoved;
    }

    public boolean isDirty() {
        return isDirty;
    }

    public void setDirty(boolean dirty) {
        isDirty = dirty;
    }

    public static void clear() {
        idCounter.reset();
    }
}