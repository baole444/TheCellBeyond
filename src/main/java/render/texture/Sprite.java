package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import render.Texture;
import scripting.API;
import utility.AssetManager;
import utility.AssetReference;
import utility.UnifiedPaths;

/**
 * Sprite is a persisting value, allow describing a texture on disk, using texture UV coordinates and the size of the sprite.
 * <p>
 * Sprite mark itself dirty (volatile) when its parameters are updated. Holders can use this for their internal update mechanic, and clear the flag where appropriate.
 * </p>
 * A sprite that does not have a texture will appear transparent, while those that has a texture but failed to resolve, will appear with missing colour texture, magenta by default.
 * <p>
 * Sprite's texture coordinates are stored as an array of 4 vectors, ordered by (right, top), (right, bottom), (left, bottom), (left, top):
 * {@snippet lang="java":
 *        Vector2f[] textureCoordinates = {
 *                 new Vector2f(rightX, topY),
 *                 new Vector2f(rightX, bottomY),
 *                 new Vector2f(leftX, bottomY),
 *                 new Vector2f(leftX, topY)
 *         };
 * }
 */
@API
public class Sprite {
    private float width, height;
    private AssetReference textureReference = null;
    private Vector2f[] textureCoordinates = {
                new Vector2f(1, 1),
                new Vector2f(1, 0),
                new Vector2f(0, 0),
                new Vector2f(0, 1)
    };
    private volatile transient boolean dirty = true;

    /**
     * Create a new blank {@link Sprite}.
     */
    public Sprite() {}

    /**
     * Create a new {@link Sprite} covering the entire region of the given image.
     * @param imagePath the path to the image file
     */
    public Sprite(String imagePath) {
        texture(imagePath);
        sizeToTexture();
    }

    /**
     * Get the {@link ResourceID} of this sprite's texture and load it if the texture is not loaded or had been unloaded.
     * @return the RID of the texture, or null if this sprite has no texture
     * @apiNote
     * A sprite that has a texture always resolve its RID, even if that texture is missing or failed to load.
     * This method return null exclusively means that this sprite has no texture, not that the texture could not be resolved.
     */
    public ResourceID textureRID() {
        if (textureReference == null) return null;
        return AssetManager.loadTexture(textureReference);
    }

    /**
     * Get the asset reference of this sprite's image.
     * @return the {@link AssetReference} of the texture, or null if this sprite has no texture
     */
    public AssetReference textureReference() {
        return textureReference;
    }

    /**
     * Check if this sprite was given a texture ot not, regardless of the texture's state.
     * @return true if there is a texture assigned to this sprite
     */
    public boolean hasTexture() {
        return textureReference != null;
    }

    /**
     * Set the texture of this sprite to the given image.
     * @param imagePath the path to the image file
     */
    public void texture(String imagePath) {
        dirty = true;
        if (imagePath == null) {
            textureReference = null;
            return;
        }
        textureReference = new AssetReference(UnifiedPaths.stripMetadata(imagePath));
        AssetManager.loadTexture(textureReference);
    }

    /**
     * Get the texture coordinates of this sprite.
     * @return the coordinate array
     */
    public Vector2f[] textureCoordinates() {
        return textureCoordinates;
    }

    /**
     * Set the texture coordinate of this sprite to the given array.
     * <p>
     * The new array should follow {@link Sprite}'s specification for texture coordinates.
     * @param texCrd the array of coordinates
     */
    public void textureCoordinates(Vector2f[] texCrd) {
        dirty = true;
        textureCoordinates = texCrd;
    }

    /**
     * Get the sprite's rendering width.
     * @return the width value, in pixels
     */
    public float width() {
        return width;
    }

    /**
     * Set the sprite's rendering width.
     * This will mark the sprite dirty if value changed.
     * @param width the new width, in pixels
     */
    public void width(float width) {
        if (this.width == width) return;
        dirty = true;
        this.width = width;
    }

    /**
     * Get the sprite's rendering height.
     * @return the height value, in pixels
     */
    public float height() {
        return height;
    }

    /**
     * Set the sprite's rendering height.
     * This will mark the sprite dirty if value changed.
     * @param height the new height, in pixels
     */
    public void height(float height) {
        if (this.height == height) return;
        dirty = true;
        this.height = height;
    }

    /**
     * Get the ID of this sprite's texture on the GPU.
     * @return the texture's ID, or -1 when there is no texture, or it is not loaded on the GPU yet
     */
    public int textureID() {
        return AssetManager.textureID(textureRID());
    }

    /**
     * Get the texture region covered by this sprite base on its texture coordinates.
     * @return a new {@link SpriteRect} describing the region, or null when the sprite has no texture, or the image can't be read
     */
    public SpriteRect rect() {
        Texture texture = TextureManager.get().texture(textureRID());
        if (texture == null) return null;
        int w = texture.width();
        int h = texture.height();
        if (w <= 0 || h <= 0) return null;
        return rect(textureCoordinates, w, h);
    }

    /**
     * Remove the dirty flag for this sprite as the Renderer already processed its latest update.
     * <p>
     * Holders are responsible for updating this flag via their own internal tracking.
     */
    public void markClean() {
        dirty = false;
    }

    /**
     * Check to see if the sprite need to be processed by Renderer again.
     * <p>
     * Holders are responsible for updating this flag via their own internal tracking.
     * @return true if rendering data of this sprite was updated.
     */
    public boolean dirtied() {
        return dirty;
    }

    /**
     * Get the sprite covering region base on the given texture coordinates and size.
     * @param textureCoordinates the 4 corner coordinates
     * @param textureWidth the width of the image, in pixels
     * @param textureHeight the height of the image, in pixels
     * @return a new {@link SpriteRect} describing the region, or null when the there are less than 4 coordinates
     * @apiNote As the coordinate's y-axis start bottom up, y is inverted for top-down pixel position.
     */
    public static SpriteRect rect(Vector2f[] textureCoordinates, int textureWidth, int textureHeight) {
        if (textureCoordinates == null || textureCoordinates.length < 4) return null;
        float leftX = textureCoordinates[2].x;
        float rightX = textureCoordinates[0].x;
        float topY = textureCoordinates[0].y;
        float bottomY = textureCoordinates[1].y;
        return new SpriteRect(
                Math.round(leftX * textureWidth),
                Math.round(textureHeight - topY * textureHeight),
                Math.round(rightX * textureWidth),
                Math.round(textureHeight - bottomY * textureHeight)
        );
    }

    /**
     * Adjust the sprite rendering dimension to match its texture's image.
     */
    void sizeToTexture() {
        Texture texture = TextureManager.get().texture(textureRID());
        if (texture == null) return;
        int w = texture.width();
        int h = texture.height();
        if (w <= 0 || h <= 0 || (width == w && height == h)) return;
        dirty = true;
        width = w;
        height = h;
    }
}
