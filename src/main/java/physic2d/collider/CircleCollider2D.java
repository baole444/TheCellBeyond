package physic2d.collider;

import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;
import render.DebugDraw;

public class CircleCollider2D extends CollisionShape2D {
    private float radius = 0.16f;

    public CircleCollider2D() {
        String name = CircleCollider2D.class.getSimpleName();
        this(name);
    }

    public CircleCollider2D(String name) {
        if (invalidName(name)) name = CircleCollider2D.class.getSimpleName();
        super(name);
    }


    public float radius() {
        return radius;
    }

    public void radius(float radius) {
        this.radius = Math.max(radius, MinimumShapeDimension);
        setFixtureNeedReset();
    }

    public float effectiveRadius() {
        Vector2f scale = globalScale();
        float effectiveR = radius * ((scale.x + scale.y) / 2.0f);
        return Math.max(effectiveR, MinimumShapeDimension);
    }

    @Override
    public Shape createCollisionShape() {
        CircleShape shape = new CircleShape();
        float radius = effectiveRadius();
        shape.setRadius(radius);
        Vector2f localPos = position();
        shape.m_p.set(localPos.x, localPos.y);
        return shape;
    }

    @Override
    protected void drawDebugShape() {
        if (gameObject == null) return;
        float radius = effectiveRadius();
        DebugDraw.addCircle(globalPosition(), radius);
    }

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openCircle = ImGui.collapsingHeader("CircleCollider2D##CircleCollider2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openCircle) {
            super.additionalImGuiLogic();
            return;
        }
        ImGui.indent();
        float r = ImEditorGui.dragFloatCtrl("Radius", radius, 0.16f, this, MinimumShapeDimension);
        if (Float.compare(r, radius) != 0) radius(r);
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}
