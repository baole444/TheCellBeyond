package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import components.Component;
import org.joml.Vector2f;
import physic2d.Physic2D;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public record DataSnapshot(
        Viewport viewport,
        HashMap<Integer, UUID> cachedIDs,
        HashMap<UUID, GameObject> gameObjectByUUIDs,
        List<GameObject> rootGameObjects,
        Map<UUID, Component> componentsByUUID,
        Physic2D physic2D,
        AtomicBoolean updated,
        List<GameObject> pendingObjectRemove,
        List<Component> pendingComponentRemove
) {
    public DataSnapshot() {
        this(new Viewport(new Vector2f(0.0f)),
                new HashMap<>(), new HashMap<>(),
                new ArrayList<>(), new HashMap<>(),
                new Physic2D(),
                new AtomicBoolean(false),
                new ArrayList<>(),
                new ArrayList<>()
        );
    }

    public DataSnapshot(DataSnapshot snapshot) {
        this(snapshot.viewport,
                new HashMap<>(snapshot.cachedIDs), new HashMap<>(snapshot.gameObjectByUUIDs),
                new ArrayList<>(snapshot.rootGameObjects), new HashMap<>(snapshot.componentsByUUID),
                snapshot.physic2D,
                new AtomicBoolean(snapshot.isUpdated()),
                new ArrayList<>(snapshot.pendingObjectRemove),
                new ArrayList<>(snapshot.pendingComponentRemove)
        );
    }

    public boolean isUpdated() {
        return updated.get();
    }

    public void markObjectForRemove(GameObject go) {
        if (go == null || pendingObjectRemove.contains(go)) return;

        pendingObjectRemove.add(go);
    }

    public void markComponentForRemove(Component component) {
        if (component == null || pendingComponentRemove.contains(component)) return;

        pendingComponentRemove.add(component);
    }

    public RenderingSnapshot extractRenderData() {
        if (!updated.get()) return null;

        List<GameObject> removeObject = new ArrayList<>(pendingObjectRemove);
        List<Component> removeComponent = new ArrayList<>(pendingComponentRemove);

        List<GameObject> updateObject = gameObjectByUUIDs.values().stream()
                .filter(go -> go.isDirty() && !go.isRemoved())
                .toList();

        if (updateObject.isEmpty() && removeObject.isEmpty() && removeComponent.isEmpty()) return null;

        RenderingSnapshot snapshot = new RenderingSnapshot(updateObject, removeObject, removeComponent);

        pendingObjectRemove.clear();
        pendingComponentRemove.clear();

        for (GameObject go : updateObject) {
            go.setDirty(false);
        }

        return snapshot;
    }
}
