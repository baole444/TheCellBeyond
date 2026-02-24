package physic2d.collider;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import org.jbox2d.collision.shapes.CircleShape;
import org.jbox2d.collision.shapes.Shape;
import org.joml.Vector2f;
import render.DebugDraw;

/**
 * CircleCollider2D is a circular 2D collision shape.
 */
public class CircleCollider2D extends CollisionShape2D {
    private float radius = 0.16f;

    /**
     * Create a new {@link CircleCollider2D} component.
     */
    public CircleCollider2D() {
        String name = CircleCollider2D.class.getSimpleName();
        this(name);
    }

    /**
     * Create a new {@link CircleCollider2D} component with the given name.
     * @param name the new name for the component
     */
    public CircleCollider2D(String name) {
        if (invalidName(name)) name = CircleCollider2D.class.getSimpleName();
        super(name);
    }

    /**
     * Get the radius of the circle collision shape.
     * @return the radius value in world units
     */
    public float radius() {
        return radius;
    }

    /**
     * Set the radius for the circle collision shape.
     * @param radius the new radius in world units
     */
    public void radius(float radius) {
        this.radius = Math.max(radius, MinimumShapeDimension);
        setFixtureNeedReset();
    }

    /**
     * Get the effective radius, which is the final size used for creating the collision shape.
     * This is a combination of the base radius and the component's global scale's vector component average.
     * @return the effective radius in world units
     */
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
        float r = EditorWidget.dragFloatCtrl("Radius", radius, 0.16f, this, MinimumShapeDimension);
        if (Float.compare(r, radius) != 0) radius(r);
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}
