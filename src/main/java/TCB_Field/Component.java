package TCB_Field;

import imgui.ImGui;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

public abstract class Component {

    public transient GameObject gameObject;

    public void start() {}

    public void update(float dt) {}

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
                    int[] isInt = {val};
                    if (ImGui.dragInt(name + ": ", isInt)) {
                        field.set(this, isInt[0]);
                    }
                } else if (type == float.class) {
                    float val = (float)value;
                    float[] isFloat = {val};
                    if (ImGui.dragFloat(name + ": ", isFloat)) {
                        field.set(this, isFloat[0]);
                    }
                } else if (type == boolean.class) {
                    boolean val = (boolean)value;
                    if (ImGui.checkbox(name + ": ", val)) {
                        val = !val;
                        field.set(this, !val);
                    }
                } else if (type == Vector3f.class) {
                    Vector3f val = (Vector3f)value;
                    float[] isVec = {val.x, val.y, val.z};
                    if (ImGui.dragFloat3(name + ": ", isVec)) {
                        val.set(isVec[0], isVec[1], isVec[2]);
                    }
                } else if (type == Vector4f.class) {
                    Vector4f val = (Vector4f)value;
                    float[] isVec = {val.x, val.y, val.z, val.w};
                    if (ImGui.dragFloat4(name + ": ", isVec)) {
                        val.set(isVec[0], isVec[1], isVec[2], isVec[3]);
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
}
