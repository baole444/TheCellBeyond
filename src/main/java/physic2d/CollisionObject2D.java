package physic2d;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.Transform2D;
import TheCellBeyond.internal.LogicServer;
import org.jbox2d.common.Vec2;
import org.jbox2d.dynamics.Body;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import org.joml.Math;
import org.joml.Vector2f;

/**
 * CollisionObject2D is an abstract base class for 2D physics objects,
 * it can hold any number of {@link physic2d.collider.CollisionShape2D}s for collision.
 */
public abstract class CollisionObject2D extends GameObject2D {
    private int collisionLayer = PhysicLayer.layerToBit(0);
    private int collisionMask = PhysicLayer.layerToBit(0);
    /**
     * Is this collision object a sensor or not. A sensor object does not participate in collision,
     * it only detects collisions.
     */
    protected boolean isSensor = false;
    /**
     * Is the physic body active in the physic world for this collision object.
     */
    protected boolean isActive = true;
    /**
     * The physic body reference of this collision object in the physic world.
     */
    protected transient Body physicBodyRef = null;
    private transient boolean needFixtureUpdate = false;
    private transient boolean dynamicChildWarned = false;
    private transient boolean staticChildWarned = false;

    /**
     * Create a new {@link CollisionObject2D}.
     */
    public CollisionObject2D() {
        String name = CollisionObject2D.class.getSimpleName();
        super(name);
    }

    /**
     * Create a new {@link CollisionObject2D} with the given name.
     * @param name the new name for the collision object
     */
    public CollisionObject2D(String name) {
        if (invalidName(name)) name = CollisionObject2D.class.getSimpleName();
        super(name);
    }

    @Override
    protected void internalUpdate(float dt) {
        if (needFixtureUpdate) updateFixtureFilter();
    }

    @Override
    public boolean interpolated() {
        return true;
    }

    /**
     * Get the nearest ancestor of type {@link CollisionObject2D} or its subclasses.
     * <p>
     * For example, consider this hierarchy tree:
     * {@snippet lang="TEXT":
     *    A (CollisionObject2D)
     *    |_B (GameObject2D)
     *      |_C (GameObject)
     *        |_This object
     * }
     * <br>
     * This method will return 2D object {@code A}, object {@code B}, and {@code C} were skipped
     * since they are not the correct {@link CollisionObject2D} type or its subclasses.
     * @return the nearest ancestor of type {@link CollisionObject2D} or its subclasses
     */
    public final CollisionObject2D getParentCollision2D() {
        GameObject parent = getParent();
        while (parent != null) {
            if (parent instanceof CollisionObject2D parentCollision2D) return parentCollision2D;
            parent = parent.getParent();
        }
        return null;
    }

    /**
     * Get the highest ancestor of type {@link CollisionObject2D}, or its subclasses, by walking up
     * the hierarchy tree of this collision object.]
     * <p>
     * if this object have no collision ancestor, then it is the highest in its tree.
     * @return the top collision ancestor in the current hierarchy tree
     */
    public final CollisionObject2D getCollisionRoot() {
        CollisionObject2D current = this;
        CollisionObject2D ancestor = getParentCollision2D();
        while (ancestor != null) {
            current = ancestor;
            ancestor = ancestor.getParentCollision2D();
        }
        return current;
    }

    /**
     * Check if this collision object and the given object belong to the same physic hierarchy tree.
     * This means that they share the same collision root object.
     * <p>
     * When the collision objects share the same hierarchy tree, they will be seen as a single physic entity.
     * @param other the other collision object to check against
     * @return true if belong on the same physic hierarchy tree
     */
    public final boolean sharePhysicHierarchy(CollisionObject2D other) {
        if (other == null) return false;
        return getCollisionRoot() == other.getCollisionRoot();
    }

    /**
     * Update the tracking previous transform for interpolation.
     * Must be called once before each physic step.
     */
    public void updatePreviousTransform () {
        if (physicBodyRef == null) return;
        Transform2D.copy(globalTransform(), previousTransform2D);
        hasPreviousTransform = true;
        notifyComponent2DHasPreviousTransform();
        renderDirty = true;
    }

    /**
     * Sync this collision body's spatial transform with its physical transform.
     * Must be called once after each physic step.
     */
    public void syncTransformFromPhysic() {
        if (physicBodyRef == null) return;
        Vec2 physicPos = physicBodyRef.getPosition();
        float physicRot = Math.toDegrees(physicBodyRef.getAngle());
        if (getParentCollision2D() == null) {
            Vector2f currentPos = position();
            boolean posDif = currentPos.x != physicPos.x || currentPos.y != physicPos.y;
            boolean rotDif = rotation() != physicRot;
            if (!posDif && !rotDif) return;
            if (posDif) position(physicPos.x, physicPos.y);
            if (rotDif) rotation(physicRot);
            return;
        }
        Vector2f currentGlobalPos = globalPosition();
        float currentGlobalRot = globalRotation();
        boolean postDif = currentGlobalPos.x != physicPos.x || currentGlobalPos.y != physicPos.y;
        boolean rotDif = currentGlobalRot != physicRot;
        if (!postDif && !rotDif) return;
        if (postDif) globalPosition(physicPos.x, physicPos.y);
        if (rotDif) globalRotation(physicRot);
    }

    /**
     * Move this body to follow the nearest {@link CollisionObject2D} ancestor's physic body.
     * <p>
     * Called once per physic step, after every physic update pass and before the world physic step.
     * This ensured the ancestor body already moved.
     * The local offset is calculated in the ancestor's frame.
     * </p>
     * Dynamic and static body type are skipped (static body can't move).
     * For dynamic body, it is suggested to use joints instead.
     * @apiNote Scale is not applied into physic calculation.
     */
    public final void applyParentFollow() {
        if (physicBodyRef == null) return;
        CollisionObject2D ancestor = getParentCollision2D();
        if (ancestor == null) return;
        Body ancestorBody = ancestor.getPhysicBodyRef();
        if (ancestorBody == null) return;
        BodyType type = physicBodyRef.getType();
        if (type == BodyType.DYNAMIC) {
            if (dynamicChildWarned) return;
            Logger.warning(String.format("Dynamic body '%s' nested under '%s' is not supported, used joints instead", name(), ancestor.name()));
            dynamicChildWarned = true;
            return;
        }
        if (type == BodyType.STATIC) {
            if (staticChildWarned) return;
            Logger.warning(String.format("Static body '%s' nested under '%s' is not moveable", name(), ancestor.name()));
            staticChildWarned = true;
            return;
        }
        Vector2f ancestorLocalOffset = ancestor.toLocal(globalPosition());
        float offsetRad = Math.toRadians(globalRotation() - ancestor.globalRotation());
        Vec2 ancestorPosition = ancestorBody.getPosition();
        float ancestorAngle = ancestorBody.getAngle();
        float cos = Math.cos(ancestorAngle);
        float sin = Math.sin(ancestorAngle);
        float worldX = ancestorPosition.x + cos * ancestorLocalOffset.x - sin * ancestorLocalOffset.y;
        float worldY = ancestorPosition.y + sin * ancestorLocalOffset.x + cos * ancestorLocalOffset.y;
        physicBodyRef.setTransform(new Vec2(worldX, worldY), ancestorAngle + offsetRad);
    }

    /**
     * Check if the collision object is a sensor or not.
     * @return true if is a sensor
     */
    public boolean isSensor() {
        return isSensor;
    }

    /**
     * Check if physic collision is active in the physic world for this collision object or not.
     * @return true if is active
     */
    public boolean isActive() {
        return isActive;
    }

    /**
     * Set the sensor mode for this collision object.
     * When an object is a sensor, it will only detect collision, not participate in it.
     * @param sensor true to set as sensor
     */
    public void setSensor(boolean sensor) {
        this.isSensor = sensor;
        if (physicBodyRef == null) return;
        Physic2D physic2D = LogicServer.currentScenePhysic2D();
        if (physic2D == null) return;
        physic2D.setIsSensor(this, sensor);
    }

    /**
     * Set the active state for the physic collision in the physic world for the collision object.
     * @param active true to enable physic collision
     */
    public void setActive(boolean active) {
        isActive = active;
        if (physicBodyRef != null) physicBodyRef.setActive(active);
    }

    /**
     * Get the physic world reference of this collision object.
     * @return the physic body reference
     */
    public Body getPhysicBodyRef() {
        return physicBodyRef;
    }

    /**
     * Set the physic body reference in the physic world for this collision object.
     * @param physicBodyRef the physic body reference to assigned with
     */
    public void setPhysicBodyRef(Body physicBodyRef) {
        this.physicBodyRef = physicBodyRef;
        if (physicBodyRef == null) return;
        Vector2f currentPos = globalPosition();
        float currentRot = globalRotation();
        this.physicBodyRef.setTransform(new Vec2(currentPos.x, currentPos.y), Math.toRadians(currentRot));
        this.physicBodyRef.setActive(isActive);
    }

    /**
     * Get the collision layer bit mask of this collision object.
     * This is the mask that define the physic layers that this collision object is on.
     * @return the collision layer mask value
     */
    public int getCollisionLayer() {
        return collisionLayer;
    }

    /**
     * Add a physic layer to the collision layer mask of this collision object.
     * This will request fixture request if the mask changed.
     * @param layerIndex the index of the physic layer to add
     */
    public void addCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.addLayerToMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    /**
     * Remove a physic layer from the collision layer mask of this collision object.
     * This will request fixture reset if the mask changed.
     * @param layerIndex the index of the physic layer to remove
     */
    public void removeCollisionLayer(int layerIndex) {
        int pastVal = collisionLayer;
        collisionLayer = PhysicLayer.removeLayerFromMask(collisionLayer, layerIndex);
        if (pastVal != collisionLayer) needFixtureUpdate = true;
    }

    /**
     * Get the collision mask of this collision object.
     * This is the mask that define the physic layers that this collision object is scanning.
     * @return the collision mask value
     */
    public int getCollisionMask() {
        return collisionMask;
    }

    /**
     * Add a physic layer to the collision mask of this collision object.
     * This will request fixture reset if the mask changed.
     * @param layerIndex the index of the physic layer to add
     */
    public void addCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.addLayerToMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    /**
     * Remove a physic layer from the collision mask of this collision object.
     * This will request fixture reset if the mask changed.
     * @param layerIndex the index of the physic layer to remove
     */
    public void removeCollisionMask(int layerIndex) {
        int pastVal = collisionMask;
        collisionMask = PhysicLayer.removeLayerFromMask(collisionMask, layerIndex);
        if (pastVal != collisionMask) needFixtureUpdate = true;
    }

    /**
     * Directly set the bit values for the collision mask of this collision object, if the new mask is valid.
     * This will request fixture reset if the mask changed.
     * @param newMasks the bit values for the collision mask
     */
    public void setCollisionMask(int newMasks) {
        if (newMasks == collisionMask || !PhysicLayer.isMaskValid(newMasks)) return;
        collisionMask = newMasks;
        needFixtureUpdate = true;
    }

    /**
     * Directly set the bit values for the collision layer mask of this collision object, if the new mask is valid.
     * This will request fixture reset if the mask changed.
     * @param newMasks the bit values for the collision layer mask
     */
    public void setCollisionLayer(int newMasks) {
        if (newMasks == collisionLayer || !PhysicLayer.isMaskValid(newMasks)) return;
        collisionLayer = newMasks;
        needFixtureUpdate = true;
    }

    /**
     * Get the friction ratio for this physic body.
     * @return the friction ration
     */
    public float friction() {
        return 0.0f;
    }

    /**
     * Get the body type of the collision object, which will be used to create the physic body reference in the physic world.
     * @return the body type for the physic world
     */
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
}
