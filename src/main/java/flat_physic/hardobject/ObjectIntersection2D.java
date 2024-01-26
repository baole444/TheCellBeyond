package flat_physic.hardobject;

import flat_physic.primitive.AABB;
import flat_physic.primitive.Box2D;
import flat_physic.primitive.Circle;
import org.joml.Vector2f;
import render.Line2D;
import utility.TCBMath;

public class ObjectIntersection2D {

    // Check Point vs Primitive
    public static boolean ptOnLine(Vector2f point, Line2D line) {
        float dy = line.loadEnd_physic2D().y - line.loadStart_physic2D().y;
        float dx = line.loadEnd_physic2D().x - line.loadStart_physic2D().x;

        if (dx == 0) {
            return TCBMath.compare(point.x, line.loadStart_physic2D().x);
        }

        float m = dy / dx;

        float b = line.loadEnd_physic2D().y -(m * line.loadEnd_physic2D().x);

        // Check line
        return point.y == m * point.x + b;
    }

    public static boolean ptOnCircle(Vector2f point, Circle circle) {
        Vector2f centre = circle.loadCentre();
        Vector2f centreToPt = new Vector2f(point).sub(centre);

        return centreToPt.lengthSquared() <= circle.loadRadius() * circle.loadRadius();
    }

    public static boolean ptInAABB(Vector2f point, AABB box) {
        Vector2f min = box.loadMin();
        Vector2f max = box.loadMax();

        return point.x <= max.x && min.x <= point.x &&
                point.y <= max.y && min.y <= point.y;
    }

    public static boolean ptInBox2D(Vector2f point, Box2D box) {
        // Point -> local space
        Vector2f ptLcBoxSpace = new Vector2f(point);

        TCBMath.rotate(ptLcBoxSpace,
                box.loadHardObject().loadRotate(),
                box.loadHardObject().loadPos()
        );

        Vector2f min = box.loadMin();
        Vector2f max = box.loadMax();

        return ptLcBoxSpace.x <= max.x && min.x <= ptLcBoxSpace.x &&
                ptLcBoxSpace.y <= max.y && min.y <= ptLcBoxSpace.y;

    }

    // Check Line vs Primitive

    public static boolean lnAndCircle(Line2D line, Circle circle) {
        if (ptOnCircle(line.loadStart_physic2D(), circle) ||
                ptOnCircle(line.loadEnd_physic2D(), circle)
        )
        {
            return true;
        }

        Vector2f segment = new Vector2f(line.loadEnd_physic2D()).sub(line.loadStart_physic2D());

        // Project point to line segment
        // parameterized pos
        Vector2f centre = circle.loadCentre();
        Vector2f centreToLnStart = new Vector2f(centre).sub(line.loadStart_physic2D());
        float t = centreToLnStart.dot(segment) / segment.dot(segment);

        if (t < 0.0f || t > 1.0f) {
            return false;
        }

        // Closet point to line segment
        Vector2f closetPt = new Vector2f(line.loadStart_physic2D()).add(segment.mul(t));


        return ptOnCircle(closetPt, circle);
    }
}
