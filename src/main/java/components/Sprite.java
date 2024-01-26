package components;

import org.joml.Vector2f;
import render.Texture;

public class Sprite {
    private float width, height;
    private Texture texture = null;
    private Vector2f[] texCoord = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
        };

    public Texture loadTex() {
        return this.texture;
    }

    public Vector2f[] loadTexCrd() {
        return this.texCoord;
    }

    public void setTex(Texture tex) {
        this.texture = tex;
    }

    public void setTexCrd(Vector2f[] texCrd) {
        this.texCoord = texCrd;
    }

    public float loadWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float loadHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public int loadTexId() {
        return texture == null ? -1 : texture.loadID();
    }
}
