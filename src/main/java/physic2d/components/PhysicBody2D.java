package physic2d.components;

import TheCellBeyond.Window;
import components.SpatialComponent;
import editor.ImEditorGui;
import imgui.ImGui;
import imgui.flag.ImGuiCol;
import imgui.type.ImBoolean;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.joml.Math;
import org.joml.Vector2f;
import physic2d.Physic2D;
import physic2d.PhysicLayer;
import physic2d.enums.PhysicBodyType;

public abstract class PhysicBody2D extends SpatialComponent {
    protected PhysicBodyType physicBodyType;
    protected float friction = 0.0f;
    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);

    protected boolean isSensor = false;
    protected transient Body physicBodyRef = null;

    private transient boolean needFixtureUpdate = false;

    public PhysicBody2D(PhysicBodyType bodyType) {
        this.physicBodyType = bodyType;
    }

    @Override
    public void update(float dt) {
        if (physicBodyRef == null) {
            additionalUpdateLogic(dt);
            return;
        }

        Vector2f physicPos = new Vector2f(physicBodyRef.getPosition().x, physicBodyRef.getPosition().y);
        float physicRot = Math.toDegrees(physicBodyRef.getAngle());

        setWorldPosition(physicPos);
        setWorldRotation(physicRot);

        if (needFixtureUpdate) updateFixtureFilter();

        additionalUpdateLogic(dt);
    }

    @Override
    protected void additionalImGuiLogic() {
        float friction = ImEditorGui.dragFloatCtrl("Friction", this.friction, this);
        if (friction != this.friction) setFriction(friction);

        ImBoolean isSensor = new ImBoolean(this.isSensor);
        if (ImGui.checkbox("Sensor Mode##PhysicBody_isSensor_" + getUUID(), isSensor)) setSensor(isSensor.get());
        ImGui.indent();
        ImGui.pushStyleColor(ImGuiCol.Header, 0.0f, 0.0f, 0.0f, 0.0f);
        boolean open = ImGui.collapsingHeader("Physic Layers##Physic_Layers_" + getUUID());
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
    }

    public float getFriction() {
        return friction;
    }

    public void setFriction(float friction) {
        this.friction = friction;
    }

    public boolean isSensor() {
        return isSensor;
    }

    public void setSensor(boolean sensor) {
        this.isSensor = sensor;
        if (physicBodyRef != null) {
            Window.getPhysic2D().setIsSensor(this, sensor);
        }
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

        if (physicBodyRef != null) {
            Vector2f currentPos = getObjectWorldPosition();
            float currentRot = getRotation();

            this.physicBodyRef.setTransform(new Vec2(currentPos.x, currentPos.y), Math.toRadians(currentRot));
        }
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

        Physic2D physic2D = Window.getPhysic2D();
        if (physic2D == null || physic2D.isLock()) return;
        if (physicBodyRef == null) {
            needFixtureUpdate = false;
            return;
        }

        physic2D.updateBodyFilters(this);
        needFixtureUpdate = false;
    }

    public abstract void configureBodyDef(BodyDef bodyDef);
}
