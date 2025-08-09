package TheCellBeyond;

import components.Component;
import editor.ImEditorGui;
import org.joml.Vector2f;
import utility.Settings;

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
        this.position = new Vector2f(from.position);
        this.scale = new Vector2f(from.scale);
        this.rotation = from.rotation;
        this.zIndex = from.zIndex;
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
        gameObject.name = ImEditorGui.inputText("Name: ", gameObject.name);
        ImEditorGui.drawVec2Ctrl("Position", this.position, 0.0f);
        ImEditorGui.spriteKeyTransform("Sprite move", this.position, Settings.GRID_WIDTH);
        ImEditorGui.drawVec2Ctrl("Scale", this.scale, 1.0f);
        this.rotation = ImEditorGui.dragFloatCtrl("Rotation", this.rotation);
        this.zIndex = ImEditorGui.dragIntCtrl("Z-Index", this.zIndex);
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
