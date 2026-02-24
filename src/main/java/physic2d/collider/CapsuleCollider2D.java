package physic2d.collider;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;

/**
 * CapsuleCollider2D is a combination of 2 circle collider and a box collider forming a 2D capsule/pillbox collision shape.
 */
public class CapsuleCollider2D extends CollisionShape2D {
    private final transient CircleCollider2D headCircle = new CircleCollider2D();
    private final transient CircleCollider2D footCircle = new CircleCollider2D();
    private final transient BoxCollider2D bodyBox = new BoxCollider2D();
    private float width = 0.32f;
    private float height = 0.64f;

    /**
     * Create a new {@link CapsuleCollider2D} component.
     */
    public CapsuleCollider2D() {
        String name = CapsuleCollider2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link CapsuleCollider2D} with the given name.
     * @param name the new name for the component
     */
    public CapsuleCollider2D(String name) {
        if (invalidName(name)) name = CapsuleCollider2D.class.getSimpleName();
        super(name);
    }

    @Override
    protected void onStart() {
        headCircle.gameObject = this.gameObject;
        footCircle.gameObject = this.gameObject;
        bodyBox.gameObject = this.gameObject;
        headCircle.start();
        footCircle.start();
        bodyBox.start();
        calculateCollider();
    }

    @Override
    protected void onEditorStart() {
        headCircle.gameObject = this.gameObject;
        footCircle.gameObject = this.gameObject;
        bodyBox.gameObject = this.gameObject;
        headCircle.editorStart();
        footCircle.editorStart();
        bodyBox.editorStart();
        calculateCollider();
    }

    @Override
    protected void onUpdate(float dt) {
        updateNestColliderTransform();
        headCircle.update(dt);
        footCircle.update(dt);
        bodyBox.update(dt);
        super.onUpdate(dt);
    }

    @Override
    protected void onEditorUpdate(float dt) {
        updateNestColliderTransform();
        headCircle.editorUpdate(dt);
        footCircle.editorUpdate(dt);
        bodyBox.editorUpdate(dt);
        super.onEditorUpdate(dt);
    }

    @Override
    protected void onTransformDirty() {
        headCircle.setTransformDirty();
        bodyBox.setTransformDirty();
        footCircle.setTransformDirty();
        super.onTransformDirty();
    }

    private void updateNestColliderTransform() {
        headCircle.getEffectiveTransform();
        footCircle.getEffectiveTransform();
        bodyBox.getEffectiveTransform();
    }

    /**
     * Get the width of the capsule collision shape.
     * @return the capsule's width in world units
     */
    public float width() {
        return width;
    }

    /**
     * Set the width of the capsule collision shape.
     * @param width the new width for the capsule in world units
     */
    public void width(float width) {
        this.width = Math.max(width, MinimumShapeDimension);
        calculateCollider();
        setFixtureNeedReset();
    }

    /**
     * Get the height of the capsule collision shape.
     * @return the capsule's height in world units
     */
    public float height() {
        return height;
    }

    /**
     * Set the height of the capsule collision shape.
     * @param height the new height for the capsule in world units
     */
    public void height(float height) {
        this.height = Math.max(height, MinimumShapeDimension);
        calculateCollider();
        setFixtureNeedReset();
    }

    private void calculateCollider() {
        float radius = width / 2.0f;
        float boxH = height - (2.0f * radius);
        headCircle.radius(radius);
        footCircle.radius(radius);
        headCircle.position(new Vector2f(0.0f, boxH / 2.0f));
        footCircle.position(new Vector2f(0.0f, -boxH / 2.0f));
        bodyBox.halfSize(new Vector2f(width / 2.0f, boxH / 2.0f));
        bodyBox.position(new Vector2f());
    }

    /**
     * Get the circle collider that used to make the top of the capsule collision shape.
     * @return the top {@link CircleCollider2D} reference
     */
    public CircleCollider2D headCircle() {
        return headCircle;
    }

    /**
     * Get the circle collider that used to make the bottom of the capsule collision shape.
     * @return the bottom {@link CircleCollider2D} reference
     */
    public CircleCollider2D footCircle() {
        return footCircle;
    }

    /**
     * Get the box collider that used to make the mid-section of the capsule collision shape.
     * @return the mid {@link BoxCollider2D} reference
     */
    public BoxCollider2D bodyBox() {
        return bodyBox;
    }

    @Override
    public Shape createCollisionShape() {
        return null;
    }

    @Override
    protected void drawDebugShape() {}

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openCapsule = ImGui.collapsingHeader("CapsuleCollier2D##CapsuleCollider2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCapsule) {
            super.additionalImGuiLogic();
            return;
        }
        ImGui.indent();
        float w = EditorWidget.dragFloatCtrl("Width", width, 0.32f, this, MinimumShapeDimension);
        float h = EditorWidget.dragFloatCtrl("Height", height, 0.64f, this, MinimumShapeDimension);
        if (Float.compare(w, width) != 0) width(w);
        if (Float.compare(h, height) != 0) height(h);
        ImGui.unindent();
    }
}
