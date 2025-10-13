package TheCellBeyond;

import components.Component;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import org.joml.Vector2f;

public class Transform extends Component {
    public Vector2f position;
    public Vector2f scale;
    public float rotation = 0.0f;
    public int zIndex;

    public Transform() {
        init(new Vector2f(), new Vector2f(1.0f, 1.0f));
    }

    public Transform(Vector2f position) {
        init(position, new Vector2f(1.0f, 1.0f));
    }

    public Transform(Vector2f position, Vector2f scale) {
        init(position, scale);
    }

    public Transform(Transform from) {
        position = new Vector2f(from.position);
        scale = new Vector2f(from.scale);
        rotation = from.rotation;
        zIndex = from.zIndex;
    }

    public void init(Vector2f position, Vector2f scale) {
        this.position = position;
        this.scale = scale;
        this.zIndex = 0;
    }

    public void copyFrom(Component target) {
        if (target instanceof Transform targetTransform) {
            this.position.set(targetTransform.position);
            this.scale.set(targetTransform.scale);
            this.rotation = targetTransform.rotation;
            this.zIndex = targetTransform.zIndex;
        }
    }

    public Transform copy() {
        return new Transform(this);
    }

    public void copyTo(Transform to) {
        to.position.set(this.position);
        to.scale.set(this.scale);
    }

    @Override
    public void imgui() {
        String compositeID = "Transform##" + getUUID();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader(compositeID);
        ImGui.popStyleColor(1);
        if (open) {
            ImGui.separator();
            ImEditorGui.dragVec2PixelToWorld("Position", position, 0.0f, this);
            ImEditorGui.dragVec2Ctrl("Scale", scale, 1.0f, this);
            this.rotation = ImEditorGui.dragFloatCtrl("Rotation", rotation, this);
            this.zIndex = ImEditorGui.dragIntCtrl("Z-Index", zIndex, this);
            ImGui.separator();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null) return false;
        if (!(o instanceof Transform t)) return false;

        return t.position.equals(this.position) &&
                t.scale.equals(this.scale) &&
                t.rotation == this.rotation &&
                t.zIndex == this.zIndex;
    }
}
