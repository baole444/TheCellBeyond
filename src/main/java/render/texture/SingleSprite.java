package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2i;
import scripting.API;
import utility.AssetManager;
import utility.AssetReference;
import utility.log.EngineLog;

/**
 * SingleSprite presents the source image as one sprite at the original or custom size.
 * @see SpriteSheet Cut multiple sprites from one image
 */
@API
public class SingleSprite {
    private static final EngineLog Logger = new EngineLog(SingleSprite.class);
    private AssetReference textureReference;
    private final Vector2i spriteSize = new Vector2i();
    private transient Sprite sprite;
    private transient boolean requireCompute = true;

    /**
     * Construct {@link SingleSprite} back from serialized data. The sprite is created on first access.
     */
    private SingleSprite() {}

    /**
     * Create a new {@link SingleSprite} for the given source image.
     * @param imagePath the path to the image file
     */
    public SingleSprite(String imagePath) {
        textureReference = imagePath != null ? new AssetReference(imagePath) : null;
        Vector2i imageSize = ImageHeader.size(textureReference);
        if (imageSize != null) spriteSize.set(imageSize);
    }

    /**
     * Create a new {@link SingleSprite} for the given source image at a custom size.
     * @param imagePath the path to the image file
     * @param width the width for the sprite, in pixels
     * @param height the height for the sprite, in pixels
     */
    public SingleSprite(String imagePath, int width, int height) {
        textureReference = imagePath != null ? new AssetReference(imagePath) : null;
        spriteSize.set(width, height);
    }

    /**
     * Create a new {@link SingleSprite} for the given source image, obtain from the texture RID.
     * @param textureRID the RID of the texture to use for the sprite
     */
    public SingleSprite(ResourceID textureRID) {
        this(AssetManager.texturePath(textureRID));
    }

    /**
     * Create a new {@link SingleSprite} for the given source image, obtain from the texture RID, with custom size.
     * @param textureRID the RID  of the texture to use for the sprite
     * @param width the width for the sprite, in pixels
     * @param height the height for the sprite, in pixels
     */
    public SingleSprite(ResourceID textureRID, int width, int height) {
        this(AssetManager.texturePath(textureRID), width, height);
    }

    /**
     * Get the reference to the source image of this single sprite.
     * @return the {@link AssetReference} of the image, or null when the sprite has no image
     */
    public AssetReference textureReference() {
        return textureReference;
    }

    /**
     * Get the {@link ResourceID} of this single sprite's texture and load it if unloaded.
     * @return the RID of the texture, or null when this sprite has no image
     */
    public ResourceID textureRID() {
        return AssetManager.loadTexture(textureReference);
    }

    /**
     * Set the source image of this single sprite to the one on the given path. Size parameter preserved.
     * @param imagePath the path to the image file
     */
    public void texture(String imagePath) {
        textureReference = imagePath != null ? new AssetReference(imagePath) : null;
        requireCompute = true;
    }

    /**
     * Set the source image of this single sprite to the one on the given path, with custom size.
     * @param imagePath the path to the image file
     * @param width the width for the sprite, in pixels
     * @param height the height for the sprite, in pixels
     */
    public void texture(String imagePath, int width, int height) {
        spriteSize.set(width, height);
        texture(imagePath);
    }

    /**
     * Get the size of this single sprite cutting sprite.
     * @return the sprite size vector {@code (width, height)}, in pixels
     * @see #requireCompute() Manually request cutting sprite again
     */
    public Vector2i spriteSize() {
        return spriteSize;
    }

    /**
     * Set the new cut size for this single sprite.
     * <p>
     * This will require the sprite to cut again if value changed.
     * @param spriteSize the new size vector {@code (width, height)}, in pixels
     */
    public void spriteSize(Vector2i spriteSize) {
        if (spriteSize == null) return;
        spriteSize(spriteSize.x, spriteSize.y);
    }

    /**
     * Set the new cut size for this single sprite.
     * <p>
     * This will require the sprite to cut again if value changed.
     * @param width the new width of cut sprite, in pixels
     * @param height the new height of cut sprite, in pixels
     */
    public void spriteSize(int width, int height) {
        if (spriteSize.equals(width, height)) return;
        spriteSize.set(width, height);
        requireCompute = true;
    }

    /**
     * Get the cut sprite of this single sprite.
     * @return the sprite, or null on empty single sprite
     */
    public Sprite sprite() {
        computeSprite();
        return sprite;
    }

    /**
     * Manually mark this single sprite and request the sprite to be cut again on next access.
     * <p>
     * This can be used for updating the live parameter objects directly then request a recut after.
     */
    public void requireCompute() {
        requireCompute = true;
    }

    private void computeSprite() {
        if (!requireCompute) return;
        requireCompute = false;
        Sprite sprite = new Sprite();
        if (textureReference != null) sprite.texture(textureReference.canonicalPath());
        if (spriteSize.x > 0 && spriteSize.y > 0) {
            sprite.width(spriteSize.x);
            sprite.height(spriteSize.y);
        }
        this.sprite = sprite;
    }
}
