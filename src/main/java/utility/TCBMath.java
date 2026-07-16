package utility;

import org.joml.Math;
import org.joml.Vector2f;
import scripting.API;

/**
 * TCBMath is a collection of static method for some custom math operations.
 */
@API
public final class TCBMath {
    private TCBMath() {}

    public static void rotate(Vector2f vec, float degAngle, Vector2f origin) {
        float x = vec.x - origin.x;
        float y = vec.y - origin.y;
        float cos = Math.cos(Math.toRadians(degAngle));
        float sin = Math.sin(Math.toRadians(degAngle));
        float pX = (x * cos) - (y * sin);
        float pY = (x * sin) + (y * cos);
        pX += origin.x;
        pY += origin.y;
        vec.x = pX;
        vec.y = pY;
    }

    public static boolean compare(float a, float b, float epsilon) {
        return Math.abs(a - b) <= epsilon * Math.max(1.0f, Math.max(Math.abs(a), Math.abs(b)));
    }

    public static boolean compare(Vector2f v1, Vector2f v2, float epsilon) {
        return compare(v1.x, v2.x, epsilon) && compare(v1.y, v2.y, epsilon);
    }

    public static boolean compare(float a, float b) {
        return Math.abs(a - b) <= Float.MIN_VALUE * Math.max(1.0f, Math.max(Math.abs(a), Math.abs(b)));
    }

    public static boolean compare(Vector2f v1, Vector2f v2) {
        return compare(v1.x, v2.x) && compare(v1.y, v2.y);
    }

    private static int cornerIndex(int x, int y) {
        if (x > 0 && y > 0) return 0;
        if (x > 0) return 1;
        if (y < 0) return 2;
        return 3;
    }

    private static int[] invertMap(int[] map) {
        int[] inverse = new int[map.length];
        for (int i = 0; i < map.length; i++) inverse[map[i]] = i;
        return inverse;
    }
}
