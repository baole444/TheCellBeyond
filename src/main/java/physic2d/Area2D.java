package physic2d;

import TheCellBeyond.GameObject2D;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import signal.Signal;

import java.util.*;

/**
 * Area2D is a region of 2D space defined by one or multiple {@link physic2d.collider.CollisionShape2D}.
 * It detects when other {@link CollisionObject2D} enter or exit it
 */
public class Area2D extends CollisionObject2D {
    /**
     * Signal that emits when the received body enters the area, requires {@link #monitoring} to be true.
     */
    public final Signal bodyEntered = new Signal(GameObject2D.class);
    /**
     * Signal that emits when the received body exit the area.
     */
    public final Signal bodyExited = new Signal(GameObject2D.class);
    /**
     * Signal that emits when the received area enters this area, requires {@link #monitoring} to be true.
     */
    public final Signal areaEntered = new Signal(Area2D.class);
    /**
     * Signal that emits when the received area exits this area, requires {@link #monitoring} to be true.
     */
    public final Signal areaExited = new Signal(Area2D.class);
    /**
     * Should this area detects bodies and other areas that enter and exit it.
     */
    public boolean monitoring = true;
    /**
     * Should other monitoring areas be able to detect this area.
     */
    public boolean monitorable = true;

    private final transient Set<GameObject2D> overlappingBodies = new LinkedHashSet<>();
    private final transient Set<Area2D> overlappingAreas = new LinkedHashSet<>();

    /**
     * Create a new {@link Area2D}.
     */
    public Area2D() {
        String name = Area2D.class.getSimpleName();
        super(name);
        isSensor = true;
    }

    /**
     * Create a new {@link Area2D} with the given name.
     * @param name the new name for the area
     */
    public Area2D(String name) {
        if (invalidName(name)) name = Area2D.class.getSimpleName();
        super(name);
        isSensor = true;
    }

    /**
     * Sensor flag toggling is disabled for Area2D.
     * @param ignore Area2D is always a sensor
     */
    @Override
    public final void setSensor(boolean ignore) {}

    /**
     * Return an unmodifiable set of intersecting bodies. The overlapping body's collision layer must be part of
     * this area's collision mask in order to be detected.
     * @return the set of overlapping bodies
     */
    public Set<GameObject2D> overlappingBodies() {
        return Collections.unmodifiableSet(overlappingBodies);
    }

    /**
     * Return an unmodifiable set of intersecting areas. The overlapping area's collision layer must be part of
     * this area's collision mask in order to be detected.
     * @return the set of overlapping areas
     */
    public Set<Area2D> overlappingAreas() {
        return Collections.unmodifiableSet(overlappingAreas);
    }

    /**
     * Check if this area has any overlapping body or not.
     * @return true if there is at least 1 body intersecting
     */
    public boolean hasOverlappingBodies() {
        return !overlappingBodies.isEmpty();
    }

    /**
     * Check if this area has any overlapping area or not.
     * @return true if there is at least 1 area intersecting
     */
    public boolean hasOverlappingAreas() {
        return !overlappingAreas.isEmpty();
    }

    @Override
    public BodyType bodyType() {
        return BodyType.KINEMATIC;
    }

    @Override
    public void configureBodyDef(BodyDef bodyDef) {}


    @Override
    public void configurePhysicBodyRef() {}

    @Override
    public Area2D copy() {
        return copy(false);
    }

    @Override
    public Area2D copy(boolean copyHierarchy) {
        Area2D copy = (Area2D) copySingleObject();
        if (copyHierarchy && !getChildren().isEmpty()) copyDescendants(this, copy);
        return copy;
    }

    void trackBodyEnter(GameObject2D body) {
        if (body == null) return;
        overlappingBodies.add(body);
    }

    void trackBodyExit(GameObject2D body) {
        if (body == null) return;;
        overlappingBodies.remove(body);
    }

    void trackAreaEnter(Area2D area) {
        if (area == null) return;
        overlappingAreas.add(area);
    }

    void trackAreaExit(Area2D area) {
        if (area == null) return;
        overlappingAreas.remove(area);
    }
}
