package components;

import TCB_Field.GameObject;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.type.ImInt;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class Component {
    private static int ID_COUNTER = 0;
    private int uID = -1;
    public transient GameObject gameObject;

    public void start() {}

    public void update(float dt) {}

    public void updateEditor (float dt) {}

    public void imgui() {
        try {
            Field[] fields = this.getClass().getDeclaredFields();
            for (Field field : fields) {
                boolean isTransient = Modifier.isTransient(field.getModifiers());
                if (isTransient) {
                    continue;
                }

                boolean isPrivate = Modifier.isPrivate(field.getModifiers());
                if (isPrivate) {
                    field.setAccessible(true);
                }

                Class type = field.getType();
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
                    String enumType = ((Enum)value).name();
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
            e.printStackTrace();
        }
    }

    public void genId() {
        if (this.uID == -1) {
            this.uID = ID_COUNTER++;
        }
    }

    private <T extends Enum<T>> String[] loadEnumVal(Class<T> enumType) {
        String[] enumVal = new String[enumType.getEnumConstants().length];
        int i = 0;
        for (T enumIntVal : enumType.getEnumConstants()) {
            enumVal[i] = enumIntVal.name();
            i++;
        }
        return enumVal;
    }

    private int indexOf(String str, String[] a) {
        for (int i = 0; i <a.length; i++) {
            if (str.equals(a[i])) {
                return i;
            }
        }
        return  -1;
    }

    public void destroy() {

    }

    public int loadUID() {
        return this.uID;
    }

    public static void init(int maxID) {
        ID_COUNTER = maxID;
    }
}
