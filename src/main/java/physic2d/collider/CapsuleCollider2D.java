package physic2d.collider;

import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;

/**
 * A combination of circle colliders forming the cap
 * of the collider body with one or more box colliders
 * in the mid-section.<br>
 * This forms a pill-shaped collider, reduce chance of
 * edge catching between collision bodies of other objects.
 */
public class CapsuleCollider2D extends CollisionShape2D {
    private final transient CircleCollider2D headCircle = new CircleCollider2D();
    private final transient CircleCollider2D footCircle = new CircleCollider2D();
    private final transient BoxCollider2D bodyBox = new BoxCollider2D();

    private float width = 0.32f;
    private float height = 0.64f;

    @Override
    protected void additionalStartLogic() {
        super.additionalStartLogic();
        headCircle.gameObject = this.gameObject;
        footCircle.gameObject = this.gameObject;
        bodyBox.gameObject = this.gameObject;

        headCircle.start();
        footCircle.start();
        bodyBox.start();

        calculateCollider();
    }

    @Override
    public void editorUpdate(float dt) {
        headCircle.editorUpdate(dt);
        footCircle.editorUpdate(dt);
        bodyBox.editorUpdate(dt);

        if (needsFixtureReset) resetFixture();
    }

    public float width() {
        return width;
    }

    public void setWidth(float width) {
        this.width = Math.max(width, MinimumShapeDimension);
        calculateCollider();
        setFixtureNeedReset();
    }

    public float height() {
        return height;
    }

    public void setHeight(float height) {
        this.height = Math.max(height, MinimumShapeDimension);
        calculateCollider();
        setFixtureNeedReset();
    }

    private void calculateCollider() {
        float radius = width / 2.0f;
        float boxH = height - (2.0f * radius);

        headCircle.setRadius(radius);
        footCircle.setRadius(radius);

        headCircle.setLocalPosition(new Vector2f(0.0f, boxH / 2.0f));
        footCircle.setLocalPosition(new Vector2f(0.0f, -boxH / 2.0f));

        bodyBox.setHalfSize(new Vector2f(width / 2.0f, boxH / 2.0f));
        bodyBox.setLocalPosition(new Vector2f());
    }

    public CircleCollider2D headCircle() {
        return headCircle;
    }

    public CircleCollider2D footCircle() {
        return footCircle;
    }

    public BoxCollider2D bodyBox() {
        return bodyBox;
    }

    @Override
    public Shape createCollisionShape() {
        return null;
    }

    @Override
    protected void drawDebugShape() {

    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openCapsule = ImGui.collapsingHeader("CapsuleCollier2D##CapsuleCollider2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCapsule) {
            super.additionalImGuiLogic();
            return;
        }
        ImGui.indent();
        float w = ImEditorGui.dragFloatCtrl("Width", width, 0.32f, this, MinimumShapeDimension);
        float h = ImEditorGui.dragFloatCtrl("Height", height, 0.64f, this, MinimumShapeDimension);
        if (Float.compare(w, width) != 0) setWidth(w);
        if (Float.compare(h, height) != 0) setHeight(h);
        ImGui.unindent();
    }
}
