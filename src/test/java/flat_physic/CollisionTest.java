package flat_physic;

import flat_physic.hardobject.HardObject;
import flat_physic.hardobject.ObjectIntersection2D;
import flat_physic.primitive.Circle;
import flat_physic.primitive.Ray2D;
import flat_physic.primitive.ReturnRayCollision;
import org.joml.Vector2f;
import org.junit.jupiter.api.Test;
import render.Line2D;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CollisionTest {
    private final float EPSILON = 0.000001f;
    private float radius = 3.0f;
    HardObject hardObject = new HardObject();
    Circle circle = new Circle();

    @Test
    public void point_on_line_should_be_true() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(12, 4));
        Vector2f point = new Vector2f(0, 0);

        assertTrue(ObjectIntersection2D.ptOnLine(point, line));
    }

    @Test
    public void point_on_line_should_be_true_2() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(12, 4));
        Vector2f point = new Vector2f(12, 4);

        assertTrue(ObjectIntersection2D.ptOnLine(point, line));
    }

    @Test
    public void point_on_vertical_line_should_be_true() {
        Line2D line = new Line2D(new Vector2f(0, 0), new Vector2f(0, 10));
        Vector2f point = new Vector2f(0, 5);

        boolean testResult = ObjectIntersection2D.ptOnLine(point, line);
        assertTrue(testResult);
    }

    @Test
    public void ray_cast_to_circle_should_be_true() {
        Ray2D ray2D = new Ray2D(new Vector2f(1, 1), new Vector2f(8.0f/10.0f, 6.0f/10.0f));
        ReturnRayCollision result = new ReturnRayCollision();
        hardObject.setPos(new Vector2f(5,6));
        circle.setRadius(3.0f);
        boolean testResult = ObjectIntersection2D.rayCast(circle, ray2D, result);
        assertTrue(testResult);
    }
}
