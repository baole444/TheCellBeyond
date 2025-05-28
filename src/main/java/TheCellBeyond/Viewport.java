package TheCellBeyond;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Viewport {
    private Matrix4f projectionMatrix, viewMatrix, inversedProjectionMatrix, inversedViewMatrix;
    public Vector2f position;

    private float sceneScale = 1.0f;
    private Vector2f aspectRatio = new Vector2f(4.0f, 3.0f);
    private Vector2f projectionSize = new Vector2f(aspectRatio.x * sceneScale, aspectRatio.y * sceneScale);
    private float zoom = 1.0f;

    public Viewport(Vector2f position) {
        this.position = position;
        this.projectionMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        this.inversedProjectionMatrix = new Matrix4f();
        this.inversedViewMatrix = new Matrix4f();
        adjustProjection();
    }

    public void adjustProjection() {
        projectionMatrix.identity();
        projectionMatrix.ortho(0.0f, projectionSize.x * this.zoom, 0.0f, projectionSize.y * this.zoom, -16.0f, 1024.0f);
        projectionMatrix.invert(inversedProjectionMatrix);
    }

    public Matrix4f getViewMatrix() {
        Vector3f Front = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f Up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.viewMatrix.identity();
        viewMatrix.lookAt(new Vector3f(position.x, position.y, 20.0f),
                                            Front.add(position.x, position.y, 0.0f),Up);

        this.viewMatrix.invert(inversedViewMatrix);

        return this.viewMatrix;
    }

    public Matrix4f getProjectionMatrix() {

        return this.projectionMatrix;
    }

    public Matrix4f getInversedProjectionMatrix() {
        return this.inversedProjectionMatrix;
    }

    public Matrix4f getInversedViewMatrix() {
        return this.inversedViewMatrix;
    }

    public Vector2f getProjectionSize() {
        return this.projectionSize;
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
}
