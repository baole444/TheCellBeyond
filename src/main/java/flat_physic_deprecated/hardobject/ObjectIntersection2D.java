package flat_physic_deprecated.hardobject;

import flat_physic_deprecated.primitive.*;
import org.joml.Math;
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

    public static boolean lnAndAABB(Line2D line,AABB box) {
        if (ptInAABB(line.loadStart_physic2D(), box) || ptInAABB(line.loadEnd_physic2D(), box)) {
            return true;
        }

        Vector2f unitVec = new Vector2f(line.loadEnd_physic2D()).sub(line.loadStart_physic2D());
        unitVec.normalize();

        //Handling infinity
        unitVec.x = (unitVec.x != 0) ? 1.0f / unitVec.x : 0f;
        unitVec.y = (unitVec.y != 0) ? 1.0f / unitVec.y : 0f;

        Vector2f min = box.loadMin();
        min.sub(line.loadStart_physic2D()).mul(unitVec);
        Vector2f max = box.loadMax();
        max.sub(line.loadStart_physic2D()).mul(unitVec);

        float tmin = Math.max(Math.min(min.x, max.y), Math.min(min.y, max.y));
        float tmax = Math.min(Math.max(min.x, max.x), Math.max(min.y, max.y));
        if (tmax < 0 || tmin > tmax) {
            return false;
        }

        float t = (tmin < 0f) ? tmax : tmin;
        return t > 0f && t * t < line.lengthSqr();
    }

    public static boolean lnAndBox2D(Line2D line, Box2D box) {
        float theta = -box.loadHardObject().loadRotate();

        Vector2f centre = box.loadHardObject().loadPos();
        Vector2f lcStart = new Vector2f(line.loadStart_physic2D());
        Vector2f lcEnd = new Vector2f(line.loadEnd_physic2D());

        TCBMath.rotate(lcStart, theta, centre);
        TCBMath.rotate(lcEnd, theta, centre);

        Line2D lcLine = new Line2D(lcStart, lcEnd);
        AABB aabb = new AABB(box.loadMin(), box.loadMax());

        return lnAndAABB(lcLine, aabb);
    }

    // Ray casting vs Primitive

    public static boolean rayCast(Circle circle, Ray2D ray, ReturnRayCollision result) {
        ReturnRayCollision.reset(result);

        Vector2f originToCentre = new Vector2f(circle.loadCentre()).sub(ray.loadOrigin());
        float radiusSqr = circle.loadRadius() * circle.loadRadius();
        float orgToCrLgthSqr = originToCentre.lengthSquared();

        // Project vector from ray origin to direction
        float oTd = originToCentre.dot(ray.loadHead());
        float orgToPjPt = orgToCrLgthSqr - (oTd * oTd);

        if (radiusSqr - orgToPjPt < 0.0f) {
            return false;
        }

        float rcSqRoot = (float)Math.sqrt(radiusSqr - orgToPjPt);
        float t = 0;
        if (orgToCrLgthSqr < radiusSqr) {
            t = oTd + rcSqRoot;
        } else {
            t = oTd - rcSqRoot;
        }

        if (result != null) {
            Vector2f pt = new Vector2f(ray.loadOrigin()).add(
                    new Vector2f(ray.loadHead().mul(t))
            );
            Vector2f normal = new Vector2f(pt).sub(circle.loadCentre());
            normal.normalize();

            result.init(pt, normal, t, true);
        }
        return true;
    }

    public static boolean rayCast(AABB box, Ray2D ray, ReturnRayCollision result) {
        ReturnRayCollision.reset(result);

        Vector2f unitVec = ray.loadHead();
        unitVec.normalize();

        //Handling infinity
        unitVec.x = (unitVec.x != 0) ? 1.0f / unitVec.x : 0f;
        unitVec.y = (unitVec.y != 0) ? 1.0f / unitVec.y : 0f;

        Vector2f min = box.loadMin();
        min.sub(ray.loadOrigin()).mul(unitVec);
        Vector2f max = box.loadMax();
        max.sub(ray.loadOrigin()).mul(unitVec);

        float tmin = Math.max(Math.min(min.x, max.y), Math.min(min.y, max.y));
        float tmax = Math.min(Math.max(min.x, max.x), Math.max(min.y, max.y));
        if (tmax < 0 || tmin > tmax) {
            return false;
        }

        float t = (tmin < 0f) ? tmax : tmin;
        boolean hit = t > 0.0f; //&& t * t < ray.loadMax();
        if (!hit) {
            return false;
        }

        if (result != null) {
            Vector2f pt = new Vector2f(ray.loadOrigin()).add(
                    new Vector2f(ray.loadHead().mul(t))
            );
            Vector2f normal = new Vector2f(ray.loadOrigin()).sub(pt);
            normal.normalize();

            result.init(pt, normal, t, true);
        }
        return true;
    }

    public static boolean rayCast(Box2D box, Ray2D ray, ReturnRayCollision result) {
        ReturnRayCollision.reset(result);

        Vector2f size = box.loadHalfSize();

        Vector2f xA = new Vector2f(1, 0);
        Vector2f yA = new Vector2f(0, 1);
        TCBMath.rotate(xA, -box.loadHardObject().loadRotate(), new Vector2f(0, 0));
        TCBMath.rotate(yA, -box.loadHardObject().loadRotate(), new Vector2f(0, 0));

        Vector2f p = new Vector2f(box.loadHardObject().loadPos()).sub(ray.loadOrigin());

        // Project ray head to axis of box
        Vector2f hPa = new Vector2f(
                xA.dot(ray.loadHead()),
                yA.dot(ray.loadHead())
        );

        // Project hPa to axis of box

        Vector2f pjPa = new Vector2f(
                xA.dot(p),
                yA.dot(p)
        );
        //    min x, max x, min y max y
        float[] tA = {0, 0, 0, 0};
        for (int i = 0; i < 2; i++) {
            if (TCBMath.compare(hPa.get(i), 0)) {
                // Check ray if parallel to current axis and origin -> no hit
                if (-pjPa.get(i) - size.get(i) > 0 || -pjPa.get(i) + size.get(i) < 0) {
                    return false;
                }

                hPa.setComponent(i, 0.00001f); // Make sure that we don't divide by 0
            }

            tA[i  * 2 + 0] = (pjPa.get(i) + size.get(i)) / hPa.get(i); // get tmax on axis
            tA[i * 2 + 1] = (pjPa.get(i) - size.get(i)) / hPa.get(i); // get tmin on axis
        }

        float tmin = Math.max(Math.min(tA[0], tA[1]), Math.min(tA[2], tA[3]));
        float tmax = Math.min(Math.max(tA[0], tA[1]), Math.max(tA[2], tA[3]));

        float t = (tmin < 0f) ? tmax : tmin;
        boolean hit = t > 0.0f; //&& t * t < ray.loadMax();
        if (!hit) {
            return false;
        }

        if (result != null) {
            Vector2f pt = new Vector2f(ray.loadOrigin()).add(
                    new Vector2f(ray.loadHead().mul(t))
            );
            Vector2f normal = new Vector2f(ray.loadOrigin()).sub(pt);
            normal.normalize();

            result.init(pt, normal, t, true);
        }
        return true;
    }
}
