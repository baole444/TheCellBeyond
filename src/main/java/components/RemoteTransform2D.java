package components;

import TheCellBeyond.GameObject;
import TheCellBeyond.GameObject2D;
import scripting.API;
import utility.HierarchyPath;
import utility.HierarchyPaths;

/**
 * RemoteTransform2D allows pushing its {@link TheCellBeyond.Transform2D} to another {@link GameObject2D} or its subclasses.
 * <p>
 * It can be set to push update on position, rotation and scale to the targeted 2D object.
 * It can use either local or global transform.
 * <p>
 * RemoteTransform2D cannot know if an object is added back to the scene,
 * {@link #resolveTarget()} can be called to update the cache in this case.
 *
 * @apiNote RemoteTransform2D use its global transform as update source regardless of {@link #useGlobalTransform}.
 */
@API
public class RemoteTransform2D extends Component2D {
    private String targetPath = "";

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

    /**
     * Cached target
     */
    private transient GameObject2D target;

    public RemoteTransform2D() {
        String name = RemoteTransform2D.class.getSimpleName();
        this(name);
    }

    public RemoteTransform2D(String name) {
        if (invalidName(name)) name = RemoteTransform2D.class.getSimpleName();
        super(name);
    }

    @Override
    protected void internalStart() {
        super.internalStart();
        resolveTarget();
    }

    @Override
    protected void internalEditorStart() {
        super.internalEditorStart();
        resolveTarget();
    }

    @Override
    protected void internalUpdate(float dt) {
        if (!validTarget()) return;
        pushTransform();
    }

    /**
     * Get the hierarchy path to the targeted object.
     * @return the hierarchy path
     */
    public String targetPath() {
        return targetPath;
    }

    /**
     * Set the targeted object using the hierarchy path leading to it.
     * @param path the hierarchy path to target
     */
    public void targetPath(String path) {
        if (path == null) path = "";
        path = path.trim();
        if (path.equals(targetPath)) return;
        targetPath = path.trim();
        resolveTarget();
    }

    /**
     * Get the targeted object that this component is pushing transform update to.
     * @return the targeted {@link GameObject2D} or its subclasses, null if there is none
     */
    public GameObject2D target() {
        return target;
    }

    /**
     * Check if the target of this remote transform is valid or not.
     * @return true if the object is of type GameObject2D or it's subclasses and is not removed
     */
    public boolean validTarget() {
        if (target == null) return false;
        if (target.isDestroyed()) {
            target = null;
            return false;
        }
        return true;
    }

    /**
     * Resolve and update the remote transform's cached target.
     */
    public void resolveTarget() {
        target = null;
        if (targetPath == null || targetPath.isBlank()) return;
        HierarchyPath path = new HierarchyPath(targetPath);
        GameObject resolved = HierarchyPaths.toGameObject(path, gameObject);

        if (!(resolved instanceof GameObject2D go2D) || gameObject.isDescendantOf(go2D)) return;
        target = go2D;
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
