package components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import TheCellBeyond.internal.LogicServer;
import scene.Scene;

import java.util.UUID;

// TODO: Convert to hierarchy path once that is developed.

/**
 * RemoteTransform2D allows pushing its {@link TheCellBeyond.Transform2D} to another {@link GameObject2D} or its subclasses.
 * <p>
 * It can be set to push update on position, rotation and scale to the targeted 2D object.
 * It can use either local or global transform.
 *
 * @apiNote RemoteTransform2D use its global transform as update source regardless of {@link #useGlobalTransform}.
 */
public class RemoteTransform2D extends SpatialComponent {
    private UUID targetUUID;

    /**
     * Should the remote 2D object's position be updated.
     */
    public boolean updatePosition = true;

    /**
     * Should the remote 2D object's rotation be updated.
     */
    public boolean updateRotation = true;

    /**
     * Should the remote 2D object's scale be updated.
     */
    public boolean updateScale = true;

    /**
     * Should the global transform or local transform be targeted.
     */
    public boolean useGlobalTransform = true;

    private transient GameObject2D target;

    @Override
    protected void onStart() {
        resolveTarget();
    }

    @Override
    protected void onUpdate(float dt) {
        if (!validTarget()) return;
        pushTransform();
    }

    /**
     * Get the unique identifier of the target's object.
     * @return the targeted {@link UUID}
     */
    public UUID targetUUID() {
        return targetUUID;
    }

    /**
     * Set the targeted object using its unique identifier.
     * @param uuid the {@link UUID} of the target
     */
    public void targetUUID(UUID uuid) {
        targetUUID = uuid;
        resolveTarget();
    }

    /**
     * Get the targeted object that this component is pushing transform update to.
     * @return the targeted {@link GameObject2D} or its subclasses, null if there is none
     */
    public GameObject2D target() {
        return target;
    }

    private void resolveTarget() {
        target = null;
        if (targetUUID == null) return;
        Scene scene = LogicServer.currentScene();
        if (scene == null) return;
        GameObject go = scene.getGameObject(targetUUID);
        if (go instanceof GameObject2D go2D) {
            if (!gameObject.isAncestor(go2D)) target = go2D;
        }
    }

    private boolean validTarget() {
        if (target == null) return false;
        if (target.isRemoved()) {
            target = null;
            return false;
        }

        return true;
    }

    private void pushTransform() {
        if (useGlobalTransform) pushGlobalTransform();
        else pushLocalTransform();
    }

    private void pushGlobalTransform() {
        if (updatePosition) target.globalPosition(globalPosition());
        if (updateRotation) target.globalRotation(globalRotation());
        if (updateScale) target.globalScale(globalScale());
    }

    private void pushLocalTransform() {
        if (updatePosition) target.position(globalPosition());
        if (updateRotation) target.rotation(globalRotation());
        if (updateScale) target.scale(globalScale());
    }
}
