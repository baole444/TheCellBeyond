package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import render.Texture;
import scripting.API;
import utility.AssetManager;
import utility.AssetReference;

/**
 * Sprite is a persisting value, allow describing a texture on disk, using texture UV coordinates and the size of the sprite.
 * <p>
 * Sprite mark itself dirty (volatile) when its parameters are updated, and will be cleared by its responsible SpriteRenderer.
 * </p>
 * A sprite that does not have a texture will appear transparent, while those that has a texture but failed to resolve, will appear with missing colour texture, magenta by default.
 */
@API
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

    /**
     * Get the {@link ResourceID} of this sprite's texture and load it if the texture is not loaded or had been unloaded.
     * @return the RID of the texture, or null if this sprite has no texture
     * @apiNote
     * A sprite that has a texture always resolve its RID, even if that texture is missing or failed to load.
     * This method return null exclusively means that this sprite has no texture, not that the texture could not be resolved.
     */
    public ResourceID textureRID() {
        if (textureCanonicalPath == null) return null;
        // Currently resolve using path, load if the texture is unloaded, don't revert to plain lookup.
        return AssetManager.loadTexture(textureCanonicalPath);
    }

    /**
     * Check if this sprite was given a texture ot not, regardless of the texture's state.
     * @return true if there is a texture assigned to this sprite
     */
    public boolean hasTexture() {
        return textureCanonicalPath != null;
    }

    public Texture getTexture() {
        if (textureCanonicalPath == null) return null;
        return AssetManager.getTexture(AssetManager.loadTexture(textureCanonicalPath));
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
        String canonPath = texture.canonicalPath();
        textureCanonicalPath = canonPath;
        if (canonPath != null) AssetManager.loadTexture(canonPath);
    }

    public void setTexture(String textureCanonicalPath) {
        dirty = true;
        if (textureCanonicalPath == null) {
            this.textureCanonicalPath = null;
            return;
        }
        AssetReference assetReference = new AssetReference(textureCanonicalPath);
        this.textureCanonicalPath = assetReference.canonicalPath();
        AssetManager.loadTexture(this.textureCanonicalPath);
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
        Texture texture = AssetManager.getTexture(AssetManager.loadTexture(textureCanonicalPath));
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
