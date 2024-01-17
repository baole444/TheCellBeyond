package TCB_Field;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class Viewport {
    private Matrix4f projectMatrix, viewMatrix;
    public Vector2f position;

    public Viewport(Vector2f position) {
        this.position = position;
        this.projectMatrix = new Matrix4f();
        this.viewMatrix = new Matrix4f();
        adjustProjection();
    }

    public void adjustProjection() {
        projectMatrix.identity();
        projectMatrix.ortho(0.0f, 640.0f, 0.0f, 480.0f, 0.0f, 100.0f);
    }

    public Matrix4f getViewMatrix() {
        Vector3f Front = new Vector3f(0.0f, 0.0f, -1.0f);
        Vector3f Up = new Vector3f(0.0f, 1.0f, 0.0f);
        this.viewMatrix.identity();
        viewMatrix.lookAt(new Vector3f(position.x, position.y, 20.0f),
                                            Front.add(position.x, position.y, 0.0f),Up);
        return this.viewMatrix;
    }

    public Matrix4f getProjectMatrix() {

        return this.projectMatrix;
    }
}
