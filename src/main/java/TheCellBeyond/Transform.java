package TheCellBeyond;

import components.Component;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import org.joml.Vector2f;
import utility.WorldUnit;

public class Transform extends Component {
    public Vector2f position;
    public Vector2f scale;
    public float rotation = 0.0f;
    public int zIndex;
    public boolean relativeZIndex = true;

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
        relativeZIndex = from.relativeZIndex;
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
            this.relativeZIndex = targetTransform.relativeZIndex;
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
            ImEditorGui.dragVec2Ctrl("Position", position, 0.0f, WorldUnit.getWorldUnitsPerPixel(), this);
            ImEditorGui.dragVec2Ctrl("Scale", scale, 1.0f, this);
            rotation = ImEditorGui.dragFloatCtrl("Rotation", rotation, this);
            zIndex = ImEditorGui.dragIntCtrl("Z-Index", zIndex, this);
            ImBoolean rZIndex = new ImBoolean(relativeZIndex);
            if (ImGui.checkbox("Z-Index as Relative##Relative_Transform_ZIndex_" + getUUID(), rZIndex)) {
                relativeZIndex = rZIndex.get();
            }
            if (ImGui.isItemHovered()) {
                ImGui.beginTooltip();
                ImGui.text("relativeZIndex = " + (relativeZIndex ? "true" : "false"));
                ImGui.spacing();
                ImGui.text("If \"true\", the final z-Index of this transform is relative to the parent/owning object.");
                ImGui.text("For example, if this transform's z-Index is 2 and final z-Index of the parent/owning object is 3, this transform's effective z-Index is 2 + 3 = 5");
                ImGui.endTooltip();
            }
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
                t.zIndex == this.zIndex &&
                t.relativeZIndex == this.relativeZIndex;
    }
}
