package render.texture;

import org.joml.Vector2f;
import render.Texture;

/**
 * A class dedicated to processing a sprite's texture orientation and dimension.
 */
public class Sprite {
    private float width, height;
    private Texture texture = null;
    private Vector2f[] textureCoordinates = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
        };

    public Texture getTexture() {
        return this.texture;
    }

    public Vector2f[] getTextureCoordinates() {
        return this.textureCoordinates;
    }

    public void setTex(Texture tex) {
        this.texture = tex;
    }

    public void setTextureCoordinates(Vector2f[] texCrd) {
        this.textureCoordinates = texCrd;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public int getTextureID() {
        return texture == null ? -1 : texture.getID();
    }
}
