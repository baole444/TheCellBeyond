package utility;

import org.joml.Vector2f;

public class WorldUnit {
    private static final float PIXELS_PER_WORLD_UNIT = 100.0f;

    private static final float WORLD_UNITS_PER_PIXEL = 1.0f / PIXELS_PER_WORLD_UNIT;

    private WorldUnit() {}

    public static float pixelToWorld(float pixel) {
        return pixel * WORLD_UNITS_PER_PIXEL;
    }

    public static Vector2f pixelToWorld(Vector2f pixelVector) {
        return new Vector2f(pixelVector).mul(WORLD_UNITS_PER_PIXEL);
    }

    public static void pixelToWorld(Vector2f pixelValue, Vector2f target) {
        target.set(pixelValue).mul(WORLD_UNITS_PER_PIXEL);
    }

    public static float worldToPixel(float worldUnit) {
        return worldUnit * PIXELS_PER_WORLD_UNIT;
    }

    public static void worldToPixel(Vector2f worldValue, Vector2f target) {
        target.set(worldValue).mul(PIXELS_PER_WORLD_UNIT);
    }

    public static float getPixelsPerWorldUnit () {
        return PIXELS_PER_WORLD_UNIT;
    }

    public static float getWorldUnitsPerPixel() {
        return WORLD_UNITS_PER_PIXEL;
    }
}
