package project;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.joml.Vector4f;

public record ClearColor(float r, float g, float b, float a) {
    public ClearColor {
        r = Math.max(0.0f, Math.min(r, 1.0f));
        g = Math.max(0.0f, Math.min(g, 1.0f));
        b = Math.max(0.0f, Math.min(b, 1.0f));
        a = Math.max(0.0f, Math.min(a, 1.0f));
    }

    @JsonIgnore
    public ClearColor() {
        this(0.027f, 0.122f, 0.067f, 1.0f);
    }

    @JsonIgnore
    public ClearColor(Vector4f color) {
        this(color.x, color.y, color.z, color.w);
    }

    @JsonIgnore
    public ClearColor(ClearColor other) {
        this(other.r, other.g, other.b, other.a);
    }

    @JsonIgnore
    public Vector4f toVector() {
        return new Vector4f(r, g, b, a);
    }
}
