package utility;

import org.joml.Math;
import org.joml.Vector2f;

public class TCBMath {

    public static void rotate(Vector2f vec, float degAngle, Vector2f origin) {
        float x = vec.x - origin.x;
        float y = vec.y - origin.y;

        float cos = (float)Math.cos(Math.toRadians(degAngle));
        float sin = (float)Math.sin(Math.toRadians(degAngle));

        float pX = (x * cos) - (y * sin);
        float pY = (x * sin) + (y * cos);

        pX += origin.x;
        pY += origin.y;

        vec.x = pX;
        vec.y = pY;
    }

    public static boolean compare(float x, float y, float epsilon) {
        return Math.abs(x - y) <= epsilon * Math.max(1.0f, Math.max(Math.abs(x), Math.abs(y)));
    }

    public static boolean compare(Vector2f v1, Vector2f v2, float epsilon) {
        return compare(v1.x, v2.x, epsilon) && compare(v1.y, v2.y, epsilon);
    }

    public static boolean compare(float x, float y) {
        return Math.abs(x - y) <= Float.MIN_VALUE * Math.max(1.0f, Math.max(Math.abs(x), Math.abs(y)));
    }

    public static boolean compare(Vector2f v1, Vector2f v2) {
        return compare(v1.x, v2.x) && compare(v1.y, v2.y);
    }
}
