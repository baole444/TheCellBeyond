package flat_physic;

import flat_physic.hardobject.ObjectIntersection2D;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import render.Line2D;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollisionTest {
    private final float EPSILON = 0.000001f;
    @Test
    public void ptOnLineShouldBeTrue() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(12, 4));
        Vector2f point = new Vector2f(0, 0);

        assertTrue(ObjectIntersection2D.ptOnLine(point, line));
    }

    @Test
    public void ptOnLineShouldBeTrue_Two() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(12, 4));
        Vector2f point = new Vector2f(12, 4);

        assertTrue(ObjectIntersection2D.ptOnLine(point, line));
    }

    @Test
    public void ptOnVerticalShouldBeTrue() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(0, 10));
        Vector2f point = new Vector2f(0, 5);

        boolean result = ObjectIntersection2D.ptOnLine(point, line);
        assertTrue(result);
    }
}
