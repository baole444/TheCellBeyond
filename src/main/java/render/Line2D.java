package render;

import org.joml.Vector2f;
import org.joml.Vector4f;

public class Line2D {
    private final Vector2f start;
    private final Vector2f end;
    private Vector4f color;
    private int alive;

    public Line2D(Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
    }

    public Line2D(Vector2f start, Vector2f end, Vector4f color, int alive) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.alive = alive;
    }

    public int startFrame() {
        this.alive--;
        return this.alive;
    }

    public Vector2f getStart() {
        return start;
    }

    public Vector2f getEnd() {
        return end;
    }

    public Vector4f color() {
        return color;
    }

    public float lengthSqr() {
        return new Vector2f(end).sub(start).lengthSquared();
    }

}
