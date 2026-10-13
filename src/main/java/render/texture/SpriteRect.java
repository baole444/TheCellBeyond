package render.texture;

import org.joml.Vector2i;
import scripting.API;

/**
 * SpriteRect is the region of an image covered by a sprite, measured from the image's top left corner, in pixels.
 * @param minX the left edge of the region
 * @param minY the top edge of the region
 * @param maxX the right edge of the region
 * @param maxY the bottom edge of the region
 */
@API
public record SpriteRect(int minX, int minY, int maxX, int maxY) {
    /**
     * Get the starting corner of the region.
     * @return the top left corner, in pixels from the image's top left
     */
    public Vector2i origin() {
        return new Vector2i(minX, minY);
    }

    /**
     * Get the ending corner of the region.
     * @return the bottom right corner, in pixels from the image's top left
     */
    public Vector2i max() {
        return new Vector2i(maxX, maxY);
    }

    /**
     * Get the width of the region.
     * @return the width in pixels
     */
    public int width() {
        return maxX - minX;
    }

    /**
     * Get the height of the region.
     * @return the height in pixels
     */
    public int height() {
        return maxY - minY;
    }
}
