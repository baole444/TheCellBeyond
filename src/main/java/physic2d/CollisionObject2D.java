package physic2d;

import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import editor.EditorWidget;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.joml.Math;
import org.joml.Vector2f;

public abstract class CollisionObject2D extends GameObject2D {
    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);
    protected boolean isSensor = false;
    protected boolean isActive = true;
    protected transient Body physicBodyRef = null;
    private transient boolean needFixtureUpdate = false;

    public CollisionObject2D() {
        String name = CollisionObject2D.class.getSimpleName();
        super(name);
    }

    public CollisionObject2D(String name) {
        if (invalidName(name)) name = CollisionObject2D.class.getSimpleName();
        super(name);
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (needFixtureUpdate) updateFixtureFilter();
    }

    /**
     * Sync this collision body's spatial transform with its physical transform.
     */
    public void syncTransformFromPhysic() {
        if (physicBodyRef == null) return;
        Vec2 physicPos = physicBodyRef.getPosition();
        float physicRot = Math.toDegrees(physicBodyRef.getAngle());
        position(physicPos.x, physicPos.y);
        rotation(physicRot);
    }

    public boolean isSensor() {
        return isSensor;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setSensor(boolean sensor) {
        this.isSensor = sensor;
        if (physicBodyRef == null) return;
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return;
        physic2D.setIsSensor(this, sensor);
    }

    public void setActive(boolean active) {
        isActive = active;
        if (physicBodyRef != null) physicBodyRef.setActive(active);
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

        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null || physic2D.isLock()) return;
        if (physicBodyRef == null) {
            needFixtureUpdate = false;
            return;
        }

        physic2D.updateBodyFilters(this);
        needFixtureUpdate = false;
    }

    public float friction() {
        return 0.0f;
    }

    public abstract BodyType bodyType();

    /**
     * Configure the physic body definition base on what is required by the physic body type and implement.
     * @param bodyDef the physic body definition to configure
     */
    public abstract void configureBodyDef(BodyDef bodyDef);

    /**
     * Configure the physic body reference base on what is required by the physic body type and implement.
     * <p>
     * This is called after the physic body reference is assigned to the physic object.
     */
    public abstract void configurePhysicBodyRef();

    @Override
    public void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openCollision = ImGui.collapsingHeader("CollisionObject2D##CollisionObject2D_Properties_Header_" + getUUID());
        if (!openCollision) {
            super.additionalImGuiLogic();
            return;
        }
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
            int collisionLayer = EditorWidget.physicLayerSelectable("Collision Layer", this.collisionLayer, this);
            ImGui.spacing();
            int collisionMask = EditorWidget.physicLayerSelectable("Collision Mask", this.collisionMask, this);
            setCollisionLayer(collisionLayer);
            setCollisionMask(collisionMask);
            ImGui.separator();
            ImGui.spacing();
        }
        ImGui.unindent();
        super.additionalImGuiLogic();
    }
}
