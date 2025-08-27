package utility;

import org.joml.Vector2f;
import org.joml.Math;

public class TextureScale {
    /**
     * Calculate texture's dimension that will fit in
     * sprite list's display size limit.
     * @param width image's width (in pixel) cast as float.
     * @param height image's height (in pixel) cast as float.
     * @return {@link Vector2f} {@code x} for width and {@code y} for height.
     */
    public static Vector2f calculateFitDimension(float width, float height) {
        double scaleDiff = Math.min(width / Settings.BOX_H, height / Settings.BOX_W);

        float newW = (float) (width * scaleDiff);
        float newH = (float) (height * scaleDiff);

        return new Vector2f(newW, newH);
    }

    /**
     * Calculate texture's dimension that will fit in the user's given limit.
     * @param width image's width (in pixel.
     * @param height image's height (in pixel).
     * @param limitW user's width limit (in pixel).
     * @param limitH user's height limit (in pixel).
     * @return {@link Vector2f} {@code x} for width and {@code y} for height.
     */
    public static Vector2f calculateFitDimension(float width, float height, float limitW, float limitH) {
        double scaleDiff = Math.min(limitW / width, limitH / height);

        float newW = (float) (width * scaleDiff);
        float newH = (float) (height * scaleDiff);

        return new Vector2f(newW, newH);
    }

    /**
     * Calculate texture's new dimension base on original
     * size and given scale factor.
     * @param width image's width (in pixel) cast as float.
     * @param height image's height (in pixel) cast as float.
     * @param scale user's scale factor, negative value will flip the image.
     * @return {@link Vector2f} {@code x} for width and {@code y} for height.
     */
    public static Vector2f textureScale(float width, float height, float scale) {
        float newW = width * scale;
        float newH = height * scale;

        return new Vector2f(newW, newH);
    }
}
