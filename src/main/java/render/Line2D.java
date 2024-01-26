package render;

import org.joml.Vector2f;
import org.joml.Vector3f;

public class Line2D {
    private Vector2f start;
    private Vector2f end;
    private Vector3f color;
    private int alive;

    public Line2D(Vector2f start, Vector2f end) {
        this.start = start;
        this.end = end;
    }

    public Line2D(Vector2f start, Vector2f end, Vector3f color, int alive) {
        this.start = start;
        this.end = end;
        this.color = color;
        this.alive = alive;
    }

    public int startFrame() {
        this.alive--;
        return this.alive;
    }

    public Vector2f loadStart() {
        return start;
    }

    public Vector2f loadEnd() {
        return end;
    }

    public Vector2f loadStart_physic2D() {
        return this.start;
    }

    public Vector2f loadEnd_physic2D() {
        return this.end;
    }

    public Vector3f loadColor() {
        return color;
    }

}
