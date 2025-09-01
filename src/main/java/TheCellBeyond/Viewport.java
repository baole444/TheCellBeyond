package TheCellBeyond;

import editor.project.Project;
import eventviewer.EngineEventListener;
import eventviewer.EngineEventCallback;
import eventviewer.event.Event;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Viewport implements EngineEventListener {
    public Vector2f position;
    private final Matrix4f projectionMatrix;
    private final Matrix4f viewMatrix;
    private final Matrix4f inverseProjectionMatrix;
    private final Matrix4f inverseViewMatrix;

    private final float sceneScale = 5.0f;
    private float aspectRatio;
    private Vector2f projectionSize;
    private float zoom = 1.0f;

    private boolean isDynamic = true;

    public Viewport(Vector2f position) {
        this.position = position;
        projectionMatrix = new Matrix4f();
        viewMatrix = new Matrix4f();
        inverseProjectionMatrix = new Matrix4f();
        inverseViewMatrix = new Matrix4f();

        aspectRatio = (float) Window.getWidth() / Window.getHeight();
        projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
        adjustProjection();
        EngineEventCallback.register(this);
    }

    public void updateAspectRatio(float width, float height) {
        if (isDynamic && width > 0 && height > 0) {
            aspectRatio = width / height;
            projectionSize = new Vector2f(aspectRatio * sceneScale, sceneScale);
            adjustProjection();
        }
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

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(0.0f, projectionSize.x * zoom,
                0.0f, projectionSize.y * zoom,
                -16.0f, 1024.0f
        );
        projectionMatrix.invert(inverseProjectionMatrix);
    }

    public Matrix4f getViewMatrix() {
        Vector3f front = new Vector3f(0.0f, 0.0f, -1.0f).add(position.x, position.y, 0.0f);
        Vector3f up = new Vector3f(0.0f, 1.0f, 0.0f);
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

    public float getZoom() {
        return zoom;
    }

    public void setZoom(float zoom) {
        this.zoom = zoom;
    }

    public void addZoom(float val) {
        this.zoom += val;
    }

    @Override
    public void onEventEmit(Object object, Event event) {
        switch (event.type) {
            case ENGINE_START -> {
                lockToGameAspectRatio();
            }
            case ENGINE_END -> {
                unlockAspectRatio();
                updateAspectRatio(Window.getWidth(), Window.getHeight());
            }
        }
    }
}
