package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import render.Texture;
import utility.AssetManager;
import utility.AssetReference;

/**
 * Sprite store the canonical path to the texture image, the texture UV coordinates and the size of the sprite.<br>
 * Sprite will mark itself dirty (volatile) when its parameters are updated,
 * which will be cleared by its responsible SpriteRenderer.
 */
public class Sprite {
    private float width, height;
    private String textureCanonicalPath = null;
    private Vector2f[] textureCoordinates = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
    };
    private volatile transient boolean dirty = true;

    public ResourceID textureRID() {
        if (textureCanonicalPath == null) return null;
        return AssetManager.get().loadTexture(textureCanonicalPath);
    }

    public Texture getTexture() {
        if (textureCanonicalPath == null) return null;
        return AssetManager.get().getTexture(AssetManager.get().loadTexture(textureCanonicalPath));
    }

    public Vector2f[] getTextureCoordinates() {
        return textureCoordinates;
    }

    public void setTexture(Texture texture) {
        dirty = true;
        if (texture == null) {
            textureCanonicalPath = null;
            return;
        }
        String canonPath = texture.getCanonicalPath();
        textureCanonicalPath = canonPath;
        if (canonPath != null) AssetManager.get().loadTexture(canonPath);
    }

    public void setTexture(String textureCanonicalPath) {
        dirty = true;
        if (textureCanonicalPath == null) {
            this.textureCanonicalPath = null;
            return;
        }
        AssetReference assetReference = new AssetReference(textureCanonicalPath);
        this.textureCanonicalPath = assetReference.canonicalPath();
        AssetManager.get().loadTexture(this.textureCanonicalPath);
    }

    public void setTextureCoordinates(Vector2f[] texCrd) {
        dirty = true;
        textureCoordinates = texCrd;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        dirty = true;
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        dirty = true;
        this.height = height;
    }

    public int getTextureID() {
        if (textureCanonicalPath == null) return -1;
        Texture texture = AssetManager.get().getTexture(AssetManager.get().loadTexture(textureCanonicalPath));
        return texture != null ? texture.getID() : -1;
    }

    /**
     * Remove the dirty flag for this Sprite as the Renderer already processed its latest update.<br>
     * This is called by SpriteRenderer when Renderer remove its sprite dirty flag.
     */
    public void rendererUpdated() {
        dirty = false;
    }

    /**
     * Check to see if the Sprite need to be processed by Renderer again.<br>
     * This flag is used by SpriteRenderer to trigger sprite dirty flag.
     * @return true if rendering data of this sprite was updated.
     */
    public boolean requestRendererUpdate() {
        return dirty;
    }
}
