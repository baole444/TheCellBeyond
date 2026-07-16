package utility;

import org.joml.Vector2f;
import scripting.API;

@API
public class WorldUnit {
    public static final float PixelsPerWorldUnit = 100.0f;
    public static final float WorldUnitsPerPixel = 1.0f / PixelsPerWorldUnit;

    private WorldUnit() {}

    public static float pixelToWorld(float pixel) {
        return pixel * WorldUnitsPerPixel;
    }

    public static Vector2f pixelToWorld(float x, float y) {
        return new Vector2f(x, y).mul(WorldUnitsPerPixel);
    }

    public static Vector2f pixelToWorld(Vector2f pixelVector) {
        return new Vector2f(pixelVector).mul(WorldUnitsPerPixel);
    }

    public static void pixelToWorld(Vector2f pixelValue, Vector2f target) {
        target.set(pixelValue).mul(WorldUnitsPerPixel);
    }

    public static float worldToPixel(float worldUnit) {
        return worldUnit * PixelsPerWorldUnit;
    }

    public static void worldToPixel(Vector2f worldValue, Vector2f target) {
        target.set(worldValue).mul(PixelsPerWorldUnit);
    }
}
