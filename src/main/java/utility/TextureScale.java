package utility;

import org.joml.Math;
import org.joml.Vector2f;
import scripting.API;

@API
public class TextureScale {
    /**
     * Calculate texture's dimension that will fit in a rectangle region.
     * @param width original texture width
     * @param height original texture height
     * @param limitW rectangle limiting width
     * @param limitH rectangle limiting height
     * @return Vector of the new texture size with aspect ratio respected
     */
    public static Vector2f calculateFitDimension(float width, float height, float limitW, float limitH) {
        double scaleDiff = Math.min(limitW / width, limitH / height);

        float newW = (float) (width * scaleDiff);
        float newH = (float) (height * scaleDiff);

        return new Vector2f(newW, newH);
    }

    /**
     * Calculate texture's dimension that will fit in a square region.
     * @param width original texture width
     * @param height original texture height
     * @param squareLimit size of the square region to fit
     * @return Vector of the new texture size with aspect ratio respected
     */
    public static Vector2f calculateFitSquare(float width, float height, float squareLimit) {
        float aspectRatio = width / height;

        if (width >= height) {
            width = squareLimit;
            height = squareLimit / aspectRatio;
        } else {
            height = squareLimit;
            width = squareLimit * aspectRatio;
        }

        return new Vector2f(width, height);
    }
}
