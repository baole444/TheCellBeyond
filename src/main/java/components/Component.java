package components;

import TheCellBeyond.GameObject;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.type.ImInt;
import org.jbox2d.dynamics.contacts.Contact;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.UUID;

/**
 * An abstraction of components for {@link GameObject} and some editor's components.
 * Also handle Editor's properties related functions.
 */
public abstract class Component {
    private String uuid;

    public transient GameObject gameObject;

    private String componentName;

    public Component() {
        this.uuid = UUID.randomUUID().toString();
    }

    public void start() {}

    public void editorUpdate(float dt) {}

    public void update(float dt) {}

    public void startCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void endCollision(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void preSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void postSolve(GameObject targetObj, Contact contact, Vector2f hitNormalization) {}

    public void destroy() {}

    protected <T extends Component> T getSibling(Class<T> componentClass) {
        if (gameObject == null) return null;
        return gameObject.getFirstComponent(componentClass);
    }

    protected <T extends Component> T getFromParent(Class<T> componentClass) {
        if (gameObject == null || gameObject.getParent() == null) return null;
        return gameObject.getParent().getFirstComponent(componentClass);
    }

    protected <T extends Component> T getFromChild(String childName, Class<T> componentClass) {
        if (gameObject == null) return null;
        GameObject child = gameObject.getChild(childName);
        if (child == null) return null;
        return child.getFirstComponent(componentClass);
    }

    protected Component findComponentByName(String name) {
        if (gameObject == null) return null;
        GameObject root = gameObject.getRoot();
        return root.findComponentByName(name);
    }

    protected Component getComponentByPath(String path) {
        if (gameObject == null || path == null) return null;
        return gameObject.resolveComponentPath(path);
    }

    public void imgui() {
        try {
            if (componentName != null) {
                String name = ImEditorGui.inputText("Component Name", componentName);
                if (!name.equals(componentName)) {
                    setComponentName(name);
                }
            }

            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                boolean isTransient = Modifier.isTransient(field.getModifiers());
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if (isTransient || isStatic) {
                    continue;
                }

                boolean isPrivate = Modifier.isPrivate(field.getModifiers());
                if (isPrivate) {
                    field.setAccessible(true);
                }

                Class<?> type = field.getType();
                Object value = field.get(this);
                String name = field.getName();

                if (type == int.class) {
                    int val = (int)value;

                    field.set(this, ImEditorGui.dragIntCtrl(name, val));

                } else if (type == float.class) {
                    float val = (float)value;

                    field.set(this, ImEditorGui.dragFloatCtrl(name, val));

                } else if (type == boolean.class) {
                    boolean val = (boolean)value;
                    if (ImGui.checkbox(name, val)) {
                        val = !val;
                        field.set(this, !val);
                    }
                } else if (type == Vector2f.class) {
                    Vector2f val = (Vector2f)value;

                    ImEditorGui.drawVec2Ctrl(name, val);

                } else if (type == Vector3f.class) {
                    Vector3f val = (Vector3f)value;
                    float[] isVec = {val.x, val.y, val.z};
                    if (ImGui.dragFloat3(name, isVec)) {
                        val.set(isVec[0], isVec[1], isVec[2]);
                    }
                } else if (type == Vector4f.class) {
                    Vector4f val = (Vector4f)value;
                    float[] isVec = {val.x, val.y, val.z, val.w};
                    if (ImGui.dragFloat4(name, isVec)) {
                        val.set(isVec[0], isVec[1], isVec[2], isVec[3]);
                    }
                } else if (type.isEnum()) {
                    String[] enumVal = loadEnumVal(type);
                    String enumType = ((Enum<?>) value).name();
                    ImInt index = new ImInt(indexOf(enumType, enumVal));

                    if (ImGui.combo(field.getName(), index, enumVal, enumVal.length)) {
                        field.set(this, type.getEnumConstants()[index.get()]);
                    }
                }

                if (isPrivate) {
                    field.setAccessible(false);
                }
            }
        } catch (IllegalAccessException e) {
            System.err.println("Failed to access component with error: " + e.getMessage());
        }
    }

    public String getUUID() {
        if (uuid == null) {
            uuid = UUID.randomUUID().toString();
        }

        return uuid;
    }

    public void setUUID(String uuid) {
        this.uuid = uuid;
    }

    public String getComponentName() {
        return componentName;
    }

    public void setComponentName(String name) {
        componentName = name;
        if (gameObject != null) {
            gameObject.onComponentNameChanged(this, name);
        }
    }

    @SuppressWarnings("unchecked")
    private String[] loadEnumVal(Class<?> enumType) {
        if (!enumType.isEnum()) {
            throw new IllegalArgumentException("Class '" + enumType.getName() + "' is not of enum type!");
        } else {
            Class<? extends Enum<?>> isEnum = (Class<? extends Enum<?>>) enumType;

            return Arrays.stream(isEnum.getEnumConstants())
                    .map(Enum::name)
                    .toArray(String[]::new);
        }
    }

    // Loop to find match string and return its index.
    private int indexOf(String str, String[] a) {
        for (int i = 0; i <a.length; i++) {
            if (str.equals(a[i])) {
                return i;
            }
        }
        return  -1;
    }
}