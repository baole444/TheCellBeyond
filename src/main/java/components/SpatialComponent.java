package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform;
import editor.ImEditorGui;
import imgui.ImGui;
import org.joml.Vector2f;

public abstract class SpatialComponent extends Component implements Transformation {
    protected final Transform localTransform = new Transform();
    private transient Transform effectiveTransform = null;
    private transient boolean isTransformDirty = true;

    @Override
    public void start() {
        super.start();
        setTransformDirty();
    }

    @Override
    public Transform getLocalTransform() {
        return localTransform;
    }

    @Override
    public Transform getEffectiveTransform() {
        updateEffectiveTransform();

        return effectiveTransform;
    }

    @Override
    public void setLocalTransform(Transform transform) {
        this.localTransform.copyFrom(transform);
        setTransformDirty();
    }

    @Override
    public Vector2f getWorldPosition() {
        if (gameObject instanceof GameObject2D go2D) return go2D.getGlobalPosition();

        return new Vector2f(localTransform.position);
    }

    @Override
    public void imgui() {
        super.imgui();
        ImGui.text("Transform offset");
        ImEditorGui.drawVec2Ctrl("Position", localTransform.position, 0.0f);
        ImEditorGui.drawVec2Ctrl("Scale", localTransform.scale, 1.0f);
        localTransform.rotation = ImEditorGui.dragFloatCtrl("Rotation", localTransform.rotation, this);
        localTransform.zIndex = ImEditorGui.dragIntCtrl("Z-Index", localTransform.zIndex, this);
    }

    // Get effective (final) transform

    public Vector2f getPosition() {
        return new Vector2f(getEffectiveTransform().position);
    }

    public Vector2f getScale() {
        return new Vector2f(getEffectiveTransform().scale);
    }

    public float getRotation() {
        return getEffectiveTransform().rotation;
    }

    public int getzIndex() {
        return getEffectiveTransform().zIndex;
    }

    // Get local (offset) transform

    public Vector2f getLocalPosition() {
        return new Vector2f(localTransform.position);
    }

    public Vector2f getLocalScale() {
        return new Vector2f(localTransform.scale);
    }

    public float getLocalRotation() {
        return localTransform.rotation;
    }

    public int getLocalzIndex() {
        return localTransform.zIndex;
    }

    // Set local (offset) transform

    public void setLocalPosition(Vector2f position) {
        localTransform.position.set(position);
        setTransformDirty();
    }

    public void setLocalScale(Vector2f scale) {
        localTransform.scale.set(scale);
        setTransformDirty();
    }

    public void setLocalRotation(float rotation) {
        localTransform.rotation = rotation;
        setTransformDirty();
    }

    public void setLocalzIndex(int zIndex) {
        localTransform.zIndex = zIndex;
        setTransformDirty();
    }

    // Set world transform (Update local transform base on world transform and object transform)

    public void setWorldPosition(Vector2f worldPosition) {
        if (gameObject instanceof GameObject2D go2D) {
            Vector2f goPosition = go2D.getGlobalPosition();
            localTransform.position.set(worldPosition).sub(goPosition);
        } else {
            localTransform.position.set(worldPosition);
        }

        setTransformDirty();
    }

    public void setWorldScale(Vector2f worldScale) {
        if (gameObject instanceof GameObject2D go2D) {
            Vector2f goScale = go2D.getGlobalScale();

            if (goScale.x != 0 && goScale.y != 0) {
                localTransform.scale.set(worldScale).div(goScale);
            } else {
                localTransform.scale.set(worldScale);
            }
        } else {
            localTransform.scale.set(worldScale);
        }

        setTransformDirty();
    }

    public void setWorldRotation(float worldRotation) {
        if (gameObject instanceof GameObject2D go2D) {
            float goRotation = go2D.getGlobalRotation();
            localTransform.rotation = worldRotation - goRotation;
        } else {
            localTransform.rotation = worldRotation;
        }

        setTransformDirty();
    }

    public void setTransformDirty() {
        isTransformDirty = true;
        additionalDirtyFlagLogic();
    }

    protected void additionalDirtyFlagLogic() {}

    private void updateEffectiveTransform() {
        if (!isTransformDirty && effectiveTransform != null) return;

        if (effectiveTransform == null) effectiveTransform = new Transform();

        if (gameObject instanceof GameObject2D go2D) {
            if (go2D.isTransformUpdating()) return;

            Transform goTransform = go2D.getGlobalTransform();
            effectiveTransform.copyFrom(goTransform);

            addTransforms(effectiveTransform, localTransform);
        } else {
            effectiveTransform.copyFrom(localTransform);
        }

        isTransformDirty = false;
    }

    private void addTransforms(Transform target, Transform offset) {
        target.position.add(offset.position);

        target.rotation += offset.rotation;

        target.scale.mul(offset.scale);

        // zIndex is absolute
        if (offset.zIndex != 0) target.zIndex = offset.zIndex;
    }
}
