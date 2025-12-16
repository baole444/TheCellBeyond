package physic2d.collider;

import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.collision.shapes.PolygonShape;
import org.jbox2d.collision.shapes.Shape;
import org.jbox2d.common.Settings;
import org.jbox2d.common.Vec2;
import org.joml.Vector2f;
import render.DebugDraw;

public class BoxCollider2D extends CollisionShape2D {
    private final Vector2f halfSize = new Vector2f(0.32f);

    public Vector2f getHalfSize() {
        return new Vector2f(halfSize);
    }

    public void setHalfSize(Vector2f halfSize) {
        float x = Math.max(MinimumShapeDimension, halfSize.x);
        float y = Math.max(MinimumShapeDimension, halfSize.y);
        this.halfSize.set(x, y);
        setFixtureNeedReset();
    }

    public Vector2f getEffectiveHalfSize() {
        Vector2f effectiveHalfSize = new Vector2f(halfSize).mul(getScale());
        effectiveHalfSize.x = Math.max(effectiveHalfSize.x, MinimumShapeDimension);
        effectiveHalfSize.y = Math.max(effectiveHalfSize.y, MinimumShapeDimension);
        return effectiveHalfSize;
    }

    @Override
    public Shape createCollisionShape() {
        PolygonShape shape = new PolygonShape();
        Vector2f effectiveHalfSize = getEffectiveHalfSize();
        Vector2f localPos = getLocalPosition();
        shape.setAsBox(effectiveHalfSize.x, effectiveHalfSize.y, new Vec2(localPos.x, localPos.y), (float) Math.toRadians(getLocalRotation()));
        return shape;
    }

    @Override
    protected void drawDebugShape() {
        if (gameObject == null) return;

        DebugDraw.addBox2(getPosition(), getEffectiveHalfSize().mul(2.0f), getRotation());
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openBox = ImGui.collapsingHeader("BoxCollier2D##BoxCollider2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openBox) {
            super.additionalImGuiLogic();
            return;
        }

        ImGui.indent();
        Vector2f half = new Vector2f(halfSize);
        if (ImEditorGui.dragVec2Ctrl("Half Size", half, 0.32f, this)) setHalfSize(half);
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}
