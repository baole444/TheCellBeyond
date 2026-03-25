package physic2d;

import TheCellBeyond.GameObject2D;
import imgui.ImGui;
import imgui.flag.ImGuiTreeNodeFlags;
import imgui.type.ImBoolean;
import org.jbox2d.dynamics.BodyDef;
import org.jbox2d.dynamics.BodyType;
import signal.Signal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Area2D is a region of 2D space defined by one or multiple {@link physic2d.collider.CollisionShape2D}.
 * It detects when other {@link CollisionObject2D} enter or exit it
 */
public class Area2D extends CollisionObject2D {
    public final Signal bodyEntered = new Signal(GameObject2D.class);
    public final Signal bodyExited = new Signal(GameObject2D.class);
    public final Signal areaEntered = new Signal(Area2D.class);
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

    public Area2D() {
        String name = Area2D.class.getSimpleName();
        super(name);
        isSensor = true;
    }

    public Area2D(String name) {
        if (invalidName(name)) name = Area2D.class.getSimpleName();
        super(name);
        isSensor = true;
    }

    /**
     * Disable toggling sensor flag.
     * @param ignore area 2D is always a sensor
     */
    @Override
    public void setSensor(boolean ignore) {}

    public Set<GameObject2D> overlappingBodies() {
        return Collections.unmodifiableSet(overlappingBodies);
    }

    public Set<Area2D> overlappingAreas() {
        return Collections.unmodifiableSet(overlappingAreas);
    }

    public boolean hasOverlappingBodies() {
        return !overlappingBodies.isEmpty();
    }

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

    @Override
    public void additionalImGuiLogic() {
        ImGui.spacing();
        boolean openArea = ImGui.collapsingHeader("Area2D##Area2D_Properties_Header_" + getUUID(), ImGuiTreeNodeFlags.DefaultOpen);
        if (!openArea) {
            super.additionalImGuiLogic();
            return;
        }
        ImBoolean tmp = new ImBoolean(monitoring);
        if (ImGui.checkbox("Monitoring##Area2D_Monitoring_Checkbox_" + getUUID(), tmp)) monitoring = tmp.get();
        tmp.set(monitorable);
        if (ImGui.checkbox("Monitorable##Area2D_Monitorable_Checkbox_" + getUUID(), tmp)) monitorable = tmp.get();
        super.additionalImGuiLogic();
    }
}
