package TheCellBeyond;

import TheCellBeyond.internal.LogicServer;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import project.Project;
import project.WindowResizeMode;
import render.FrameBuffer;
import utility.WorldUnit;

public class Viewport {
    public static final int NearZIndex = -16;
    public static final int FarZIndex = 1024;
    private static float globalScale;
    public final Vector2f position = new Vector2f();
    private final Matrix4f projectionMatrix = new Matrix4f();
    private final Matrix4f viewMatrix = new Matrix4f();
    private final Matrix4f inverseProjectionMatrix = new Matrix4f();
    private final Matrix4f inverseViewMatrix = new Matrix4f();
    private float sceneScale;
    private float aspectRatio;
    private Vector2f projectionSize;
    private final Vector2f zoom = new Vector2f(1.0f);
    private float rotation = 0.0f;
    /**
     * Player's option lock/unlock API.
     */
    private boolean isDynamic = true;
    private boolean maintainAspectRatio = false;
    private WindowResizeMode resizeMode = WindowResizeMode.Expand;

    public Viewport() {
        globalScale = Project.preference().textureGlobalScale();
        aspectRatio = (float) Window.getWidth() / Window.getHeight();
        sceneScale = WorldUnit.pixelToWorld(Window.getHeight());
        if (LogicServer.runtimeMode()) {
            resizeMode = Project.preference().resizeMode();
            maintainAspectRatio = Project.preference().maintainAspectRatio();
            if (resizeMode == WindowResizeMode.Scale) {
                aspectRatio = Window.getTargetAspectRatio();
                sceneScale = WorldUnit.pixelToWorld(Project.preference().gameWindowHeight());
            }
        }
        projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
        adjustProjection();
    }

    public Viewport(Vector2f position) {
        this();
        this.position.set(position);
    }

    public void updateAspectRatio(float width, float height) {
        if (width <= 0.0f || height <= 0.0f) return;
        aspectRatio = width / height;
        projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
        adjustProjection();
    }

    public void lockToGameAspectRatio() {
        isDynamic = false;
        aspectRatio = Project.getGameAspectRatio();
        projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
        adjustProjection();
    }

    public void unlockAspectRatio() {
        isDynamic = true;
    }

    public void adjustSceneScale(float availHeight) {
        FrameBuffer fb = Window.getFrameBuffer();
        float fbHWorldUnit = WorldUnit.pixelToWorld(fb.getHeight());
        float scale = availHeight / fb.getHeight();
        sceneScale = fbHWorldUnit * scale;
        projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
    }

    public void adjustProjection() {
        globalScale = Project.preference().textureGlobalScale();
        projectionMatrix.identity();
        projectionMatrix.ortho(
                0.0f, projectionSize.x * (zoom.x / globalScale),
                0.0f, projectionSize.y * (zoom.y / globalScale),
                NearZIndex, FarZIndex
        );
        projectionMatrix.invert(inverseProjectionMatrix);
    }

    public Matrix4f getViewMatrix() {
        float radians = (float) Math.toRadians(rotation);
        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        Vector3f front = new Vector3f(position.x, position.y, -1.0f);
        Vector3f up = new Vector3f(-sin, cos, 0.0f);
        Vector3f cameraPos = new Vector3f(position.x, position.y, 20.0f);
        viewMatrix.identity().lookAt(cameraPos, front, up);
        viewMatrix.invert(inverseViewMatrix);
        return viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }

    public Matrix4f getInverseProjectionMatrix() {
        return inverseProjectionMatrix;
    }

    public Matrix4f getInverseViewMatrix() {
        return inverseViewMatrix;
    }

    public Vector2f getProjectionSize() {
        return projectionSize;
    }

    public float getAspectRatio() {
        return aspectRatio;
    }

    public Vector2f getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom.set(zoom);
    }

    public void setZoom(Vector2f zoom) {
        if (zoom == null) return;
        this.zoom.set(zoom);
    }

    public void addZoom(float value) {
        this.zoom.add(value, value);
    }

    public float getRotation() {
        return rotation;
    }

    public void setRotation(float degrees) {
        rotation = degrees;
    }

    public float sceneScale() {
        return sceneScale;
    }

    public boolean isDynamic() {
        return isDynamic;
    }

    public WindowResizeMode resizeMode() {
        return resizeMode;
    }

    public boolean maintainAspectRatio() {
        return maintainAspectRatio;
    }
}
