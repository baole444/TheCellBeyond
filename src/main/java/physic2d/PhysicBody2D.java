package physic2d;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.enums.PhysicBodyType;

public abstract class PhysicBody2D extends GameObject2D {
    protected PhysicBodyType physicBodyType;

    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);

    protected float friction = 0.0f;
    protected boolean isSensor = false;
    protected boolean isActive = true;
    protected transient Body physicBodyRef = null;

    private transient boolean needFixtureUpdate = false;

    public PhysicBody2D(PhysicBodyType bodyType) {
        String name = PhysicBody2D.class.getSimpleName();
        this(name, bodyType);
    }

    public PhysicBody2D(String name, PhysicBodyType bodyType) {
        super(name);
        physicBodyType = bodyType;
    }

    @Override
    public void update(float dt) {
        if (physicBodyRef != null) {
            Vector2f physicPos = new Vector2f(physicBodyRef.getPosition().x, physicBodyRef.getPosition().y);
            float physicRot = Math.toDegrees(physicBodyRef.getAngle());

            position(physicPos);
            rotation(physicRot);
        }

        super.update(dt);

        if (needFixtureUpdate) updateFixtureFilter();
        physicUpdate(dt);
    }

    protected void physicUpdate(float dt) {}

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public boolean isSensor() {
        return isSensor;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setSensor(boolean sensor) {
        this.isSensor = sensor;
        if (physicBodyRef != null) {
            LogicServer.physic2D().setIsSensor(this, sensor);
        }
    }

    public void setActive(boolean active) {
        isActive = active;
        if (physicBodyRef != null) physicBodyRef.setActive(active);
    }

    public PhysicBodyType getPhysicBodyType() {
        return physicBodyType;
    }

    public void setPhysicBodyType(PhysicBodyType physicBodyType) {
        this.physicBodyType = physicBodyType;
    }

    public Body getPhysicBodyRef() {
        return physicBodyRef;
    }

    public void setPhysicBodyRef(Body physicBodyRef) {
        this.physicBodyRef = physicBodyRef;

        if (physicBodyRef == null) return;
        Vector2f currentPos = globalPosition();
        float currentRot = globalRotation();

        this.physicBodyRef.setTransform(new Vec2(currentPos.x, currentPos.y), Math.toRadians(currentRot));
        this.physicBodyRef.setActive(isActive);
    }

    public int getCollisionLayer() {
        return collisionLayer;
    }

    public void addCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.addLayerToMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    public void removeCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.removeLayerFromMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    public int getCollisionMask() {
        return collisionMask;
    }

    public void addCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.addLayerToMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    public void removeCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.removeLayerFromMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    public void setCollisionMask(int newMasks) {
        if (newMasks == collisionMask || !PhysicLayer.isMaskValid(newMasks)) return;
        collisionMask = newMasks;
        needFixtureUpdate = true;
    }

    public void setCollisionLayer(int newMasks) {
        if (newMasks == collisionLayer || !PhysicLayer.isMaskValid(newMasks)) return;
        collisionLayer = newMasks;
        needFixtureUpdate = true;
    }

    private void updateFixtureFilter() {
        if (!needFixtureUpdate) return;

        Physic2D physic2D = LogicServer.physic2D();
        if (physic2D == null || physic2D.isLock()) return;
        if (physicBodyRef == null) {
            needFixtureUpdate = false;
            return;
        }

        physic2D.updateBodyFilters(this);
        needFixtureUpdate = false;
    }

    public abstract void configureBodyDef(BodyDef bodyDef);

    public abstract void configureBody();

    /**
     * Add movement velocity to this physic body using the given vector.
     * Depends on the body type, the values might be processed differently.
     * <p>
     * <b>Note:</b> various built-in APIs treat this as adding values on top of existing movement values.
     * @param velocity the velocity vector to add (unit: m/s)
     */
    public void addMovement(Vector2f velocity) {}

    @Override
    protected void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openPhysic = ImGui.collapsingHeader("PhysicBody2D##PhysicBody2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openPhysic) {
            super.additionalImGuiLogic();
            return;
        }
        ImGui.indent();
        float friction = ImEditorGui.dragFloatCtrl("Friction", this.friction, this);
        if (friction != this.friction) setFriction(friction);

        ImBoolean sensor = new ImBoolean(isSensor);
        if (ImGui.checkbox("Sensor Mode##PhysicBody2D_isSensor_" + getUUID(), sensor)) setSensor(sensor.get());

        ImBoolean active = new ImBoolean(isActive);
        if (ImGui.checkbox("Active##PhysicBody2D_isActive_" + getUUID(), active)) setActive(active.get());

        ImGui.indent();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader("Physic Layers##Physic_Body_Physic_Layers_" + getUUID());
        ImGui.popStyleColor(1);
        if (open) {
            ImGui.separator();
            int collisionLayer = ImEditorGui.physicLayerSelectable("Collision Layer", this.collisionLayer, this);
            ImGui.spacing();
            int collisionMask = ImEditorGui.physicLayerSelectable("Collision Mask", this.collisionMask, this);
            setCollisionLayer(collisionLayer);
            setCollisionMask(collisionMask);
            ImGui.separator();
            ImGui.spacing();
        }
        ImGui.unindent();
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}
