package render.texture;

import TheCellBeyond.internal.ResourceID;
import org.joml.Vector2f;
import org.joml.Vector2i;
import render.Texture;
import scripting.API;
import utility.AssetManager;
import utility.AssetReference;
import utility.log.EngineLog;

import java.util.ArrayList;
import java.util.List;

/**
 * Spritesheet provides ability to define and cut sprites evenly from a source image.
 * <p>
 * Sprites are cut from left to right, top to bottom, at the same size, with additional spacing in between when specified.
 * </p>
 * The cut region of a sheet can be customised and limited, allow packing multiple sheets into a single image.
 * @see SingleSprite Present the whole image as one sprite
 */
@API
public class SpriteSheet {
    private static final EngineLog Logger = new EngineLog(SpriteSheet.class);
    private AssetReference textureReference;
    private int importSpriteCount;
    private final Vector2i spriteSize = new Vector2i();
    private final Vector2i startPosition = new Vector2i();
    private final Vector2i endPosition = new Vector2i();
    private final Vector2i spriteSpacing = new Vector2i();
    private final transient List<Sprite> sprites = new ArrayList<>();
    private transient boolean requireCompute = true;

    /**
     * Construct {@link SpriteSheet} back from serialized data. The sprite cut operation happens on first access.
     */
    private SpriteSheet() {}

    /**
     * Create a new {@link SpriteSheet} for the given source image.
     * @param imagePath the path to the image file
     * @param spriteWidth the width of one sprite, in pixels
     * @param spriteHeight the height of one sprite, in pixels
     * @param importSpriteCount the number of sprites to cut
     */
    public SpriteSheet(String imagePath, int spriteWidth, int spriteHeight, int importSpriteCount) {
        this(imagePath, new Vector2i(spriteWidth, spriteHeight), importSpriteCount, null, null, null);
    }

    /**
     * Create a new {@link SpriteSheet} for the given source image.
     * @param imagePath the path to the image file
     * @param spriteSize the size of one sprite, in pixels
     * @param importSpriteCount the number of sprites to cut
     * @param spriteSpacing the space between to sprites on each axis, in pixels
     * @apiNote
     * When spacing is null, it will default to {@code (0, 0)} spacing.
     */
    public SpriteSheet(String imagePath, Vector2i spriteSize, int importSpriteCount, Vector2i spriteSpacing) {
        this(imagePath, spriteSize, importSpriteCount, spriteSpacing, null, null);
    }

    /**
     * Create a new {@link SpriteSheet} for the given source image, with custom starting cut offset.
     * @param imagePath the path to the image file
     * @param spriteSize the size of one sprite, in pixels
     * @param importSpriteCount the number of sprites to cut
     * @param spriteSpacing the space between to sprites on each axis, in pixels
     * @param startPosition the top left boundary for the sheet's cutting region on the image, in pixels
     * @apiNote
     * When spacing / start position is null, it will default to {@code (0, 0)} spacing / start offset.
     */
    public SpriteSheet(String imagePath, Vector2i spriteSize, int importSpriteCount, Vector2i spriteSpacing, Vector2i startPosition) {
        this(imagePath, spriteSize, importSpriteCount, spriteSpacing, startPosition, null);
    }

    /**
     * Create a new {@link SpriteSheet} for the given source image, with custom cut region.
     * @param imagePath the path to the image file
     * @param spriteSize the size of one sprite, in pixels
     * @param importSpriteCount the number of sprites to cut
     * @param spriteSpacing the space between to sprites on each axis, in pixels
     * @param startPosition the top left boundary for the sheet's cutting region on the image
     * @param endPosition the bottom right boundary for the sheet's cutting region on the image
     * @apiNote
     * When spacing / start position is null, it will default to {@code (0, 0)} spacing / start offset.
     * <p>
     * When end position is null, it will default to the full image's extends.
     */
    public SpriteSheet(String imagePath, Vector2i spriteSize, int importSpriteCount, Vector2i spriteSpacing, Vector2i startPosition, Vector2i endPosition) {
        textureReference = imagePath != null ? new AssetReference(imagePath) : null;
        this.spriteSize.set(spriteSize);
        this.importSpriteCount = importSpriteCount;
        if (spriteSpacing != null) this.spriteSpacing.set(spriteSpacing);
        if (startPosition != null) this.startPosition.set(startPosition);
        if (endPosition != null) this.endPosition.set(endPosition);
    }

    /**
     * Create a new {@link SpriteSheet} for the given source image, obtain from the texture RID, with custom cut region.
     * @param textureRID the RID of the texture to use for the sheet
     * @param spriteSize the size of one sprite, in pixels
     * @param importSpriteCount the number of sprites to cut
     * @param spriteSpacing the space between to sprites on each axis, in pixels
     * @param startPosition the offset on where to cut the first sprite, from the top left corner of the image, in pixels
     * @param endPosition the limit on where to stop cutting sprite, in pixels
     * @apiNote
     * When spacing / start position is null, it will default to {@code (0, 0)} spacing / start offset.
     * <p>
     * When end position is null, it will default to the full image's extends.
     */
    public SpriteSheet(ResourceID textureRID, Vector2i spriteSize, int importSpriteCount, Vector2i spriteSpacing, Vector2i startPosition, Vector2i endPosition) {
        this(AssetManager.texturePath(textureRID), spriteSize, importSpriteCount, spriteSpacing, startPosition, endPosition);
    }

    /**
     * Get the sprite at the given index.
     * @param index the index of the sprite in this sheet
     * @return the sprite, or null on empty sheet or index out of bound
     */
    public Sprite spriteIndex(int index) {
        computeSprites();
        if (sprites.isEmpty()) {
            Logger.warning(String.format("Sprite index %d does not exist, sheet is empty", index));
            return null;
        }
        if (index >= 0 && index < sprites.size()) return sprites.get(index);
        Logger.error(String.format("Sprite index %d out of bound, last valid index: %d for %d sprite(s)", index, sprites.size() - 1, sprites.size()));
        return null;
    }

    /**
     * Get the reference to the source image of this sheet.
     * @return the {@link AssetReference} of the image, or null when the sheet has no image
     */
    public AssetReference textureReference() {
        return textureReference;
    }

    /**
     * Get the {@link ResourceID} of this sheet's texture and load it if unloaded.
     * @return the RID of the texture, or null when this sheet has no image
     */
    public ResourceID textureRID() {
        return AssetManager.loadTexture(textureReference);
    }

    /**
     * Set the source image of this sheet to the one on the given path. All cut parameters preserved.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param imagePath the path to the image file
     */
    public void texture(String imagePath) {
        sprites.clear();
        requireCompute = true;
        textureReference = imagePath != null ? new AssetReference(imagePath) : null;
    }

    /**
     * Get the amount of cut sprites from this sheet.
     * <p>
     * The value may be differed from the specified import count due to the source image's lack of space to accommodate the cutting dimensions.
     * @return the number of available sprites
     */
    public int availableSpriteCount() {
        computeSprites();
        return sprites.size();
    }

    /**
     * Get the specified number of sprite to cut from the sheet.
     * <p>
     * The value may be differed from the available sprite count due to the source image's lack of space to accommodate the cutting dimensions.
     * @return the number of requested cut
     */
    public int importSpriteCount() {
        return importSpriteCount;
    }

    /**
     * Set the number of sprite to cut from the sheet.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param importSpriteCount the new number of sprites to cut
     */
    public void importSpriteCount(int importSpriteCount) {
        if (this.importSpriteCount == importSpriteCount) return;
        this.importSpriteCount = importSpriteCount;
        requireCompute = true;
    }

    /**
     * Get the size of this sheet's cutting sprites.
     * @return the sprite size vector {@code (width, height)}, in pixels
     * @see #requireCompute() Manually request cutting sprites again
     */
    public Vector2i spriteSize() {
        return spriteSize;
    }

    /**
     * Set the new cut size for sprites on this sheet.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param spriteSize the new size vector {@code (width, height)}, in pixels
     */
    public void spriteSize(Vector2i spriteSize) {
        if (spriteSize == null) return;
        spriteSize(spriteSize.x, spriteSize.y);
    }

    /**
     * Set the new cut size for all sprites on this sheet.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param width the new width of cut sprites, in pixels
     * @param height the new height of cut sprites, in pixels
     */
    public void spriteSize(int width, int height) {
        if (spriteSize.equals(width, height)) return;
        spriteSize.set(width, height);
        requireCompute = true;
    }

    /**
     * Get the top left bound position of this sheet's cutting area.
     * @return the bounding point vector, in pixels
     * @see #requireCompute() Manually request cutting sprites again
     */
    public Vector2i startPosition() {
        return startPosition;
    }

    /**
     * Set the top left bound for the cutting area of the sheet. Any sprite that have its region ahead this point won't be cut.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param startPosition the new bounding point vector, in pixels
     * @apiNote {@code (0, 0)} is the top left corner of the image
     */
    public void startPosition(Vector2i startPosition) {
        if (startPosition == null) return;
        startPosition(startPosition.x, startPosition.y);
    }

    /**
     * Set the top left bound for the cutting area of the sheet. Any sprite that have its region ahead this point won't be cut.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param x the bound on the x-axis, in pixels
     * @param y the bound on the y-axis, in pixels
     * @apiNote {@code (0, 0)} is the top left corner of the image
     */
    public void startPosition(int x, int y) {
        if (startPosition.equals(x, y)) return;
        startPosition.set(x, y);
        requireCompute = true;
    }

    /**
     * Get the bottom right bound position of this sheet's cutting area.
     * @return the bounding point vector, in pixels
     * @see #requireCompute() Manually request cutting sprites again
     */
    public Vector2i endPosition() {
        return endPosition;
    }

    /**
     * Set the bottom right bound for the cutting area of the sheet. Any sprites that have its region pass this point will not be cut.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param endPosition the new bounding point vector, in pixels
     * @apiNote {@code (0, 0)} will default to the bottom right corner (full extends) of the image
     */
    public void endPosition(Vector2i endPosition) {
        if (endPosition == null) return;
        endPosition(endPosition.x, endPosition.y);
    }

    /**
     * Set the bottom right bound for the cutting area of the sheet. Any sprites that have its region pass this point will not be cut.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param x the bound on the x-axis, in pixels
     * @param y the bound on the y-axis, in pixels
     * @apiNote {@code (0, 0)} will default to the bottom right corner (full extends) of the image
     */
    public void endPosition(int x, int y) {
        if (endPosition.equals(x, y)) return;
        endPosition.set(x, y);
        requireCompute = true;
    }

    /**
     * Get the spacing gap between cut sprites of this sheet.
     * This is the distance for where to start cutting the next sprite from previous one.
     * @return the spacing distance vector, in pixels
     */
    public Vector2i spriteSpacing() {
        return spriteSpacing;
    }

    /**
     * Set the spacing gap between cut sprites.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param spriteSpacing the new spacing distance vector, in pixels
     */
    public void spriteSpacing(Vector2i spriteSpacing) {
        if (spriteSpacing == null) return;
        spriteSpacing(spriteSpacing.x, spriteSpacing.y);
    }

    /**
     * Set the spacing gap between cut sprites.
     * <p>
     * This will require the sheet to cut again if value changed.
     * @param horizontalGap the new horizontal gap, in pixels
     * @param verticalGap the new vertical gap, in pixels
     */
    public void spriteSpacing(int horizontalGap, int verticalGap) {
        if (spriteSpacing.equals(horizontalGap, verticalGap)) return;
        spriteSpacing.set(horizontalGap, verticalGap);
        requireCompute = true;
    }

    /**
     * Clear all the cut sprites from this sheet, all the consumers that used those sprite are not affected.
     * <p>
     * The sheet will be cut again on the next sprite request.
     */
    public void dispose() {
        sprites.clear();
        requireCompute = true;
    }

    /**
     * Manually mark this sheet and request the sprites to be cut again on next access.
     * <p>
     * This can be used for updating the live parameter objects directly then request a recut after.
     */
    public void requireCompute() {
        requireCompute = true;
    }

    /**
     * Calculate and cut the sheet into sprites if required.
     */
    private void computeSprites() {
        if (!requireCompute) return;
        requireCompute = false;
        sprites.clear();
        if (spriteSize.x <= 0 || spriteSize.y <= 0) {
            Logger.warning(String.format("Cannot cut sprites from '%s': sprite size %s is not positive", textureReference.canonicalPath(), spriteSize));
            return;
        }
        Texture texture = TextureManager.get().texture(textureRID());
        if (texture == null) return;
        int textureWidth = texture.width();
        int textureHeight = texture.height();
        if (textureWidth <= 0 || textureHeight <= 0) {
            Logger.warning(String.format("Cannot cut sprites from '%s': texture size (%d, %d) is not positive", textureReference.canonicalPath(), textureWidth, textureHeight));
            return;
        }
        int regionLeft = Math.max(0, startPosition.x);
        int regionTop = Math.max(0, startPosition.y);
        int regionRight = endPosition.x > 0 ? Math.min(endPosition.x, textureWidth) : textureWidth;
        int regionBottom = endPosition.y > 0 ? Math.min(endPosition.y, textureHeight) : textureHeight;
        if (regionLeft + spriteSize.x > regionRight || regionTop + spriteSize.y > regionBottom) {
            Logger.debug(String.format("Cannot cut sprites from '%s': no sprite of size %s can fit in the region from (%d, %d) to (%d, %d)", textureReference.canonicalPath(), spriteSize, regionLeft, regionTop, regionRight, regionBottom));
            return;
        }
        int lowestRow = textureHeight - regionBottom;
        int x = regionLeft;
        int y = textureHeight - regionTop - spriteSize.y;
        for (int i = 0; i < importSpriteCount; i++) {
            if (x + spriteSize.x > regionRight) {
                x = regionLeft;
                y -= spriteSize.y + spriteSpacing.y;
            }
            if (y < lowestRow) {
                Logger.debug(String.format("Stop extracting sprite %d and onward from '%s': sprite out of bound%nExtracted %d/%d sprite(s)", i, textureReference.canonicalPath(), sprites.size(), importSpriteCount));
                break;
            }
            sprites.add(cutSprite(y, x, textureWidth, textureHeight));
            x += spriteSize.x + spriteSpacing.x;
        }
    }

    private Sprite cutSprite(int y, int x, int textureWidth, int textureHeight) {
        float topY = (float) (y + spriteSize.y) / textureHeight;
        float rightX = (float) (x + spriteSize.x) / textureWidth;
        float leftX = (float) x / textureWidth;
        float bottomY = (float) y / textureHeight;
        Vector2f[] textureCoordinates = {
                new Vector2f(rightX, topY),
                new Vector2f(rightX, bottomY),
                new Vector2f(leftX, bottomY),
                new Vector2f(leftX, topY)
        };
        Sprite sprite = new Sprite();
        sprite.texture(textureReference.canonicalPath());
        sprite.textureCoordinates(textureCoordinates);
        sprite.width(spriteSize.x);
        sprite.height(spriteSize.y);
        return sprite;
    }
}
