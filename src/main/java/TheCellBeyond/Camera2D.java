package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import org.joml.Vector2f;
import scene.Scene;

import java.util.List;

/**
 * Camera2D is the camera object for 2D scene.
 * It is an efficient way to set up scrollable scene instead of manually moving the scene's root object.
 * <p>
 * Cameras register themselves with their targeted custom viewport or scene's global viewport.
 * Only one camera can be active per viewport.
 * @apiNote
 * The position of the Camera2D's transform does not represent the actual position of the screen,
 * which may be differed due to smoothing or limits.
 */
public class Camera2D extends GameObject2D {
    /**
     * Control how the position of the camera is mapped to the viewport.
     */
    public enum AnchorMode {
        /**
         * The position of the camera is mapped to the bottom left corner of the viewport.
         */
        BottomLeftOrigin,
        /**
         * The position of the camera is mapped to the centre of the viewport.
         */
        CentralOrigin
    }

    /**
     * Control when the camera perform its update process in frame.
     */
    public enum UpdateProcess {
        /**
         * Camera updates happened during physic update calls.
         */
        PhysicFrame,
        /**
         * Camera updates happened during logic(idle) update calls.
         */
        LogicFrame,
    }

    /**
     * The anchor mode of this camera.
     */
    public AnchorMode anchorMode = AnchorMode.CentralOrigin;

    /**
     * The frame in which the camera will update in.
     */
    public UpdateProcess updateProcess = UpdateProcess.LogicFrame;

    /**
     * The viewport assigned to this camera, if {@code null}, use scene's viewport instead.
     */
    private Viewport customViewport = null;

    /**
     * The camera's zoom. Higher values zoom out and smaller value zoom in.
     * <p>
     * For example a zoom of (2.0,2.0) will be twice as zoom out on each axis (the view cover an area 4 times larger.)
     * In contrast, a zoom of (0.5,0.5) will be twice as zoomed in on each axis (the view cover an area 4 times smaller.)
     * <p>
     * The x and y component should be set to the same value unless stretch camera view is desired.
     */
    public final Vector2f zoom = new Vector2f(1.0f);

    /**
     * Should the camera only move when reaching the horizontal (left and right) drag margins or not.
     */
    public boolean enableHorizontalDrag = false;

    /**
     * Should the camera only move when reaching the vertical (top and bottom) drag margin or not.
     */
    public boolean enableVerticalDrag = false;

    /**
     * Left margin needed to drag the camera (0.0-1.0).
     * A value of 1 means the camera only moves when it hits the left edge of the screen.
     */
    public float leftDragMargin = 0.2f;

    /**
     * Right margin needed to drag the camera (0.0-1.0).
     * A value of 1 means the camera only moves when it hits the right edge of the screen.
     */
    public float rightDragMargin = 0.2f;

    /**
     * Top margin needed to drag the camera (0.0-1.0).
     * A value of 1 means the camera only moves when it hits the top edge of the screen.
     */
    public float topDragMargin = 0.2f;

    /**
     * Bottom margin needed to drag the camera (0.0-1.0).
     * A value of 1 means the camera only moves when it hits the bottom edge of the screen.
     */
    public float bottomDragMargin = 0.2f;

    /**
     * The relative horizontal drag offset of the camera between left (-1) and right (1) drag margins.
     * This value shift both edges toward a direction, can be used to set initial offset or force offset the camera.
     * @apiNote
     * This does not update automatically if drag margins changes or when {@link #enableHorizontalDrag} is true.
     */
    public float horizontalDragOffset  = 0.0f;

    /**
     * The relative vertical offset of the camera's left (-1) and right (1) drag margins.
     * This value shift both edges toward a direction, can be used to set initial offset or force offset the camera.
     * @apiNote
     * This does not update automatically if drag margins changed or when {@link #enableVerticalDrag} is true.
     */
    public float verticalDragOffset = 0.0f;

    /**
     * Should there be limits on how far can the camera scroll in 4 directions.
     */
    public boolean enableLimit = true;

    /**
     * Should the camera smoothly stops when reach its limits.
     * This requires {@link #enablePositionSmoothing} to be true.
     */
    public boolean enableLimitSmoothing = false;

    /**
     * The left scroll limit in world units. The camera stops moving left when reaching this value,
     * but {@link #offset} can push the view pass this point.
     */
    public float leftLimit = -1024.0f;

    /**
     * The right scroll limit in world units. The camera stops moving right when reaching this value,
     * but {@link #offset} can push the view pass this point.
     */
    public float rightLimit = 1024.0f;

    /**
     * The top scroll limit in world units. The camera stops moving up when reaching this value,
     * but {@link #offset} can push the view pass this point.
     */
    public float topLimit = 1024.0f;

    /**
     * The bottom scroll limit in world units. The camera stops moving down when reaching this value,
     * but {@link #offset} can push the view pass this point.
     */
    public float bottomLimit = -1024.0f;

    /**
     * The camera's relative offset. Can be used to look around or shake animations.
     * This offset allows the camera to go pass the 4 limits defined in {@link #leftLimit}, {@link #rightLimit}, {@link #topLimit} and {@link #bottomLimit}.
     */
    public final transient Vector2f offset = new Vector2f();

    /**
     * Should the camera's view smoothly moves to its targeted position at {@link #positionSmoothingSpeed} rate.
     */
    public boolean enablePositionSmoothing = false;

    /**
     * Speed in pixels/s of the camera's smoothing effect when {@link #enablePositionSmoothing} is true.
     */
    public float positionSmoothingSpeed = 4.0f;

    /**
     * Should the camera's view rendering ignore the rotation value of {@link GameObject2D}.
     */
    public boolean ignoreRotation = true;

    /**
     * Should the camera's view smoothly rotate to its targeted rotation,
     * using asymptotic smoothing, at {@link #rotationSmoothingSpeed} rate.
     */
    public boolean enableRotationSmoothing = false;

    /**
     * The asymptotic speed of the camera's rotation smoothing when {@link #enableRotationSmoothing} is true.
     */
    public float rotationSmoothingSpeed = 4.0f;

    /**
     * Should this camera be active or not. If the camera is active on entering scene, it can become the main camera
     * if there is no other active {@link Camera2D} on the scene tree.
     * <p>
     * If the camera is current active and enabled is set to false, the next enabled camera in scene will become active.
     */
    private boolean enabled = true;

    private transient boolean currentlyActive = false;
    private transient boolean initialized = false;
    private transient final Vector2f cameraPosition = new Vector2f();
    private transient float cameraRotationDegrees = 0.0f;

    /**
     * Create a new {@link Camera2D} object.
     */
    public Camera2D() {
        String name = Camera2D.class.getSimpleName();
        super(name);
    }

    /**
     * Create a new {@link Camera2D} object with the given name.
     * @param name the new name for the object
     */
    public Camera2D(String name) {
        if (invalidName(name)) name = Camera2D.class.getSimpleName();
        super(name);
    }

    /**
     * Create a new {@link Camera2D} object and assign it to a custom viewport.
     * @param customViewport the custom viewport to target
     */
    public Camera2D(Viewport customViewport) {
        String name = Camera2D.class.getSimpleName();
        super(name);
        this.customViewport = customViewport;
    }

    /**
     * Create a new {@link Camera2D} object with the given name and assign it to a custom viewport.
     * @param name the new name for the object
     * @param customViewport the custom viewport to target
     */
    public Camera2D(String name, Viewport customViewport) {
        if (invalidName(name)) name = Camera2D.class.getSimpleName();
        super(name);
        this.customViewport = customViewport;
    }

    @Override
    protected void onStart() {
        if (!enabled) return;
        if (!viewportHasActiveCamera(resolveViewport())) makeCurrent();
    }

    @Override
    protected void onDestroy() {
        if (!currentlyActive) return;
        currentlyActive = false;
        makeNextCameraActive(resolveViewport());
    }

    @Override
    protected void onUpdate(float dt) {
        if (updateProcess == UpdateProcess.LogicFrame) updateCamera(dt);
    }

    @Override
    protected void onPhysicUpdate(float dt) {
        if (updateProcess == UpdateProcess.PhysicFrame) updateCamera(dt);
    }

    /**
     * Get the custom viewport that this camera is targeting.
     * @return the custom viewport or null if this camera is targeting the scene's global viewport
     */
    public Viewport customViewport() {
        return customViewport;
    }

    /**
     * Set the custom viewport that this camera will target.
     * This camera will make the next enabled camera active if it is currently active.
     * <p>
     * If the new target viewport doesn't have an active camera in it yet,
     * this camera will attempt to make itself active for that viewport.
     * @param target the designated viewport or null to target scene's global viewport
     */
    public void customViewport(Viewport target) {
        if (customViewport == target) return;
        Viewport oldTarget = resolveViewport();
        customViewport = target;
        Viewport newTarget = resolveViewport();
        if (!currentlyActive) return;
        currentlyActive = false;
        initialized = false;
        makeNextCameraActive(oldTarget);
        if (!viewportHasActiveCamera(newTarget)) makeCurrent();
    }

    /**
     * Make this camera the active camera (main camera) for its targeted viewport.
     * This will either be te scene's global viewport or the {@link #customViewport} of this camera.
     */
    public void makeCurrent() {
        Viewport target = resolveViewport();
        if (target == null) return;
        getCameraInScene(LogicServer.currentScene(), this).stream()
                .filter(camera -> camera.currentlyActive && camera.resolveViewport() == target)
                .forEach(camera -> {
                    camera.currentlyActive = false;
                    camera.initialized = false;
                });
        currentlyActive = true;
        initialized = false;
    }

    /**
     * Check if this camera is active or not.
     * @return true if active.
     */
    public boolean currentlyActive() {
        return currentlyActive;
    }

    /**
     * Check if this camera is active for the targeted viewport or not.
     * @param target the target viewport to check against
     * @return true if this camera is active and targeting the same viewport
     */
    public boolean currentlyActive(Viewport target) {
        return currentlyActive && resolveViewport() == target;
    }

    /**
     * Check if this camera is enabled or not.
     * @return true if the camera is enabled
     */
    public boolean enabled() {
        return enabled;
    }

    /**
     * Set the enabled state of the camera.
     * <p>
     * If set to true, this camera will attempt to make itself the main camera for its targeting viewport.
     * </p>
     * If set to false, this camera will attempt to make the next camera targeting the same viewport the main camera.
     * @param enable the new enabled state
     */
    public void enabled(boolean enable) {
        if (enabled == enable) return;
        enabled = enable;
        if (enable) {
            if (!viewportHasActiveCamera(resolveViewport())) makeCurrent();
            return;
        }
        if (currentlyActive) {
            currentlyActive = false;
            initialized = false;
            makeNextCameraActive(resolveViewport());
        }
    }

    @Override
    public Camera2D copy() {
        return copy(false);
    }

    @Override
    public Camera2D copy(boolean copyHierarchy) {
        Camera2D copy = (Camera2D) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }

    private void updateCamera(float dt) {
        if (!currentlyActive) return;
        Viewport viewport = resolveViewport();
        if (viewport == null) return;
        Vector2f visibleSize = new Vector2f(viewport.getProjectionSize()).mul(zoom);
        if (!initialized) {
            cameraPosition.set(calculateBaseTarget(visibleSize));
            cameraRotationDegrees = ignoreRotation ? 0.0f : globalRotation();
            initialized = true;
        }
        updatePosition(dt, visibleSize);
        updateRotation(dt);
        viewport.position.set(cameraPosition).add(offset);
        viewport.setZoom(zoom);
        viewport.setRotation(cameraRotationDegrees);
        viewport.adjustProjection();
    }

    private void updatePosition(float dt, Vector2f visibleSize) {
        Vector2f baseTarget = calculateBaseTarget(visibleSize);
        Vector2f target = applyDrag(baseTarget, visibleSize);
        if (enableLimit) clampToLimits(target, visibleSize);
        if (!enablePositionSmoothing) {
            cameraPosition.set(target);
            return;
        }
        float alpha = 1.0f - (float) Math.exp(-positionSmoothingSpeed * dt);
        cameraPosition.lerp(target, alpha);
        if (enableLimit && !enableLimitSmoothing) clampToLimits(cameraPosition, visibleSize);
    }

    private void updateRotation(float dt) {
        float targetRotation = ignoreRotation ? 0.0f : globalRotation();
        if (ignoreRotation || !enableRotationSmoothing) {
            cameraRotationDegrees = targetRotation;
            return;
        }
        float alpha = 1.0f - (float) Math.exp(-rotationSmoothingSpeed * dt);
        cameraRotationDegrees += (targetRotation - cameraRotationDegrees) * alpha;
    }

    private Vector2f calculateBaseTarget(Vector2f visibleSize) {
        Vector2f objectPosition = new Vector2f(globalPosition());
        if (anchorMode != AnchorMode.CentralOrigin) return objectPosition;
        Vector2f centreVisible = new Vector2f(visibleSize).mul(0.5f);
        return objectPosition.sub(centreVisible);
    }

    private Vector2f applyDrag(Vector2f baseTarget, Vector2f visibleSize) {
        Vector2f result = new Vector2f(baseTarget);
        Vector2f objectPosition = globalPosition();
        applyHorizontalDrag(objectPosition, visibleSize, result);
        applyVerticalDrag(objectPosition, visibleSize, result);
        return result;
    }

    private void applyHorizontalDrag(Vector2f objectPosition, Vector2f visibleSize, Vector2f result) {
        if (!enableHorizontalDrag) return;
        float shift = horizontalDragOffset * visibleSize.x;
        float leftBound = cameraPosition.x + visibleSize.x * leftDragMargin + shift;
        float rightBound = cameraPosition.x + visibleSize.x * (1.0f - rightDragMargin) + shift;
        if (objectPosition.x < leftBound) {
            result.x = objectPosition.x - visibleSize.x * leftDragMargin - shift;
            return;
        }
        if (objectPosition.x > rightBound) {
            result.x = objectPosition.x - visibleSize.x * (1.0f - rightDragMargin) - shift;
            return;
        }
        result.x = cameraPosition.x;
    }

    private void applyVerticalDrag(Vector2f objectPosition, Vector2f visibleSize, Vector2f result) {
        if (!enableVerticalDrag) return;
        float shift = verticalDragOffset * visibleSize.y;
        float bottomBound = cameraPosition.y + visibleSize.y * bottomDragMargin + shift;
        float topBound = cameraPosition.y + visibleSize.y * (1.0f - topDragMargin) + shift;
        if (objectPosition.y < bottomBound) {
            result.y = objectPosition.y - visibleSize.y * bottomDragMargin - shift;
            return;
        }
        if (objectPosition.y > topBound) {
            result.y = objectPosition.y - visibleSize.y * (1.0f - topDragMargin) - shift;
            return;
        }
        result.y = cameraPosition.y;
    }

    private void clampToLimits(Vector2f position, Vector2f visibleSize) {
        position.x = Math.min(Math.max(position.x, leftLimit), rightLimit - visibleSize.x);
        position.y = Math.min(Math.max(position.y, bottomLimit), topLimit - visibleSize.y);
    }

    private Viewport resolveViewport() {
        return customViewport != null ? customViewport : LogicServer.currentSceneViewport();
    }

    private boolean viewportHasActiveCamera(Viewport target) {
        if (target == null) return false;
        return getCameraInScene(LogicServer.currentScene(), this).stream()
                .anyMatch(camera -> camera.currentlyActive && camera.resolveViewport() == target);
    }

    private void makeNextCameraActive(Viewport target) {
        if (target == null) return;
        Viewport sceneViewport = LogicServer.currentSceneViewport();
        boolean targetIsSceneViewport = target == sceneViewport;
        getCameraInScene(LogicServer.currentScene(), this).stream()
                .filter(camera -> camera.enabled)
                .filter(camera -> targetIsSceneViewport ? camera.customViewport == null : camera.customViewport == target)
                .findFirst()
                .ifPresent(Camera2D::makeCurrent);
    }

    private static List<Camera2D> getCameraInScene(Scene scene, Camera2D context) {
        if (scene == null || context == null) return List.of();
        return scene.getGameObjects().stream()
                .filter(go -> go instanceof Camera2D && go != context && !go.isRemoved())
                .map(go -> (Camera2D) go)
                .toList();
    }
}
