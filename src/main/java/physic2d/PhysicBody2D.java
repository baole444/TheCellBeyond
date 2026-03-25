package physic2d;

import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.jbox2d.dynamics.BodyType;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public abstract class PhysicBody2D extends CollisionObject2D {
    protected PhysicBodyType physicBodyType;
    protected float friction = 0.0f;

    public PhysicBody2D(PhysicBodyType bodyType) {
        String name = PhysicBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    public PhysicBody2D(String name, PhysicBodyType bodyType) {
        if (invalidName(name)) name = PhysicBody2D.class.getSimpleName();
        super(name);
        physicBodyType = bodyType;
    }

    @Override
    public float friction() {
        return friction;
    }

    @Override
    public BodyType bodyType() {
        return switch (physicBodyType) {
            case Kinematic -> BodyType.KINEMATIC;
            case Static -> BodyType.STATIC;
            case Dynamic -> BodyType.DYNAMIC;
        };
    }

    public void friction(float friction) {
        this.friction = friction;
    }

    public PhysicBodyType getPhysicBodyType() {
        return physicBodyType;
    }

    public void setPhysicBodyType(PhysicBodyType physicBodyType) {
        this.physicBodyType = physicBodyType;
    }

    /**
     * Add movement velocity to this physic body using the given vector.
     * Depends on the body type, the values might be processed differently.
     * <p>
     * <b>Note:</b> various built-in APIs treat this as adding values on top of existing movement values.
     * @param velocity the velocity vector to add (unit: m/s)
     */
    public void addMovement(Vector2f velocity) {}

    @Override
    public void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openPhysic = ImGui.collapsingHeader("PhysicBody2D##PhysicBody2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openPhysic) {
            super.additionalImGuiLogic();
            return;
        }
        float friction = EditorWidget.dragFloatCtrl("Friction", this.friction, this);
        if (friction != this.friction) friction(friction);
        ImBoolean sensor = new ImBoolean(isSensor);
        if (ImGui.checkbox("Sensor Mode##PhysicBody2D_isSensor_" + getUUID(), sensor)) setSensor(sensor.get());
        super.additionalImGuiLogic();
    }
}
