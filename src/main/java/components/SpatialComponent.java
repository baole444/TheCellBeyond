package components;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import imgui.ImGui;
import org.joml.Vector2f;

public abstract class SpatialComponent extends Component implements Transformation {
    protected final Transform2D localTransform2D = new Transform2D();
    private transient Transform2D effectiveTransform2D = null;
    private transient boolean isTransformDirty = true;

    @Override
    public void start() {
        super.start();
        setTransformDirty();
    }

    @Override
    public void editorStart() {
        super.editorStart();
        setTransformDirty();
    }

    @Override
    public Transform2D getLocalTransform() {
        return localTransform2D;
    }

    @Override
    public Transform2D getEffectiveTransform() {
        updateEffectiveTransform();

        return effectiveTransform2D;
    }

    @Override
    public void setLocalTransform(Transform2D transform2D) {
        this.localTransform2D.copy(transform2D);
        setTransformDirty();
    }

    @Override
    public Vector2f getObjectWorldPosition() {
        if (gameObject instanceof GameObject2D go2D) return go2D.globalPosition();

        return new Vector2f(localTransform2D.position);
    }

    @Override
    public void imgui() {
        super.imgui();
        Transform2D editing = new Transform2D(localTransform2D);
        ImGui.indent();
        localTransform2D.imgui();
        ImGui.unindent();
        if (!editing.equals(localTransform2D)) setTransformDirty();
    }

    // Get effective (final) transform
    public Vector2f globalPosition() {
        return getEffectiveTransform().position;
    }

    public Vector2f globalScale() {
        return getEffectiveTransform().scale;
    }

    public float globalRotation() {
        return getEffectiveTransform().rotation;
    }

    public int globalZIndex() {
        return getEffectiveTransform().zIndex;
    }

    // Get local (offset) transform
    public Vector2f position() {
        return localTransform2D.position;
    }

    public Vector2f scale() {
        return localTransform2D.scale;
    }

    public float rotation() {
        return localTransform2D.rotation;
    }

    public int zIndex() {
        return localTransform2D.zIndex;
    }

    // Set local (offset) transform
    public void position(Vector2f position) {
        if (position == null) return;
        position(position.x, position.y);
    }

    public void position(float x, float y) {
        localTransform2D.position.set(x, y);
        setTransformDirty();
    }

    public void scale(Vector2f scale) {
        if (scale == null) return;
        scale(scale.x, scale.y);
    }

    public void scale(float x, float y) {
        localTransform2D.scale.set(x, y);
        setTransformDirty();
    }

    public void rotation(float rotation) {
        localTransform2D.rotation = rotation;
        setTransformDirty();
    }

    public void zIndex(int zIndex) {
        localTransform2D.zIndex = zIndex;
        setTransformDirty();
    }

    // Set world transform (Update local transform base on world transform and object transform)
    public void setWorldPosition(Vector2f worldPosition) {
        if (gameObject instanceof GameObject2D go2D) {
            Vector2f goPosition = go2D.globalPosition();
            localTransform2D.position.set(worldPosition).sub(goPosition);
        } else {
            localTransform2D.position.set(worldPosition);
        }

        setTransformDirty();
    }

    public void setWorldScale(Vector2f worldScale) {
        if (gameObject instanceof GameObject2D go2D) {
            Vector2f goScale = go2D.globalScale();

            if (goScale.x != 0 && goScale.y != 0) {
                localTransform2D.scale.set(worldScale).div(goScale);
            } else {
                localTransform2D.scale.set(worldScale);
            }
        } else {
            localTransform2D.scale.set(worldScale);
        }

        setTransformDirty();
    }

    public void setWorldRotation(float worldRotation) {
        if (gameObject instanceof GameObject2D go2D) {
            float goRotation = go2D.globalRotation();
            localTransform2D.rotation = worldRotation - goRotation;
        } else {
            localTransform2D.rotation = worldRotation;
        }

        setTransformDirty();
    }

    public void setTransformDirty() {
        isTransformDirty = true;
        additionalDirtyFlagLogic();
    }

    protected void additionalDirtyFlagLogic() {}

    private void updateEffectiveTransform() {
        if (!isTransformDirty && effectiveTransform2D != null) return;

        if (effectiveTransform2D == null) effectiveTransform2D = new Transform2D();

        if (gameObject instanceof GameObject2D go2D) {
            if (go2D.isTransformUpdating()) return;

            Transform2D goTransform2D = go2D.globalTransform();
            effectiveTransform2D.copy(goTransform2D);

            addTransforms(effectiveTransform2D, localTransform2D);
        } else {
            effectiveTransform2D.copy(localTransform2D);
        }

        isTransformDirty = false;
    }

    private void addTransforms(Transform2D target, Transform2D offset) {
        target.position.add(offset.position);
        target.rotation += offset.rotation;
        target.scale.mul(offset.scale);
        target.relativeZIndex = offset.relativeZIndex;

        if (offset.relativeZIndex) {
            target.zIndex += offset.zIndex;
            return;
        }
        target.zIndex = offset.zIndex;
    }
}
