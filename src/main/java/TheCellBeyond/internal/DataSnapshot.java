package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import components.Component;
import org.joml.Vector2f;
import physic2d.Physic2D;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data snapshot of {@link scene.Scene}
 * @param viewport the viewport of the scene
 * @param cachedIDs the map of object shader ids mapped by the object uuid
 * @param gameObjectByUUIDs the map of the game objects in the scene
 * @param rootGameObjects the list of game objects at the root level
 * @param componentsByUUID the map of components in the scene
 * @param physic2D the physic world of the scene
 * @param updated the update state of this snapshot
 */
public record DataSnapshot(
        Viewport viewport,
        HashMap<Integer, UUID> cachedIDs,
        HashMap<UUID, GameObject> gameObjectByUUIDs,
        List<GameObject> rootGameObjects,
        Map<UUID, Component> componentsByUUID,
        Physic2D physic2D,
        AtomicBoolean updated
) {
    public DataSnapshot {
        if (viewport == null) viewport = new Viewport(new Vector2f());
        if (cachedIDs == null) cachedIDs = new HashMap<>();
        if (rootGameObjects == null) rootGameObjects = new ArrayList<>();
        if (componentsByUUID == null) componentsByUUID = new HashMap<>();
        if (physic2D == null) physic2D = new Physic2D();
        if (updated == null) updated = new AtomicBoolean(false);
    }

    public DataSnapshot() {
        this(new Viewport(new Vector2f()),
                new HashMap<>(), new HashMap<>(),
                new ArrayList<>(), new HashMap<>(),
                new Physic2D(),
                new AtomicBoolean(false)
        );
    }

    public DataSnapshot(DataSnapshot snapshot) {
        this(snapshot.viewport,
                new HashMap<>(snapshot.cachedIDs), new HashMap<>(snapshot.gameObjectByUUIDs),
                new ArrayList<>(snapshot.rootGameObjects), new HashMap<>(snapshot.componentsByUUID),
                snapshot.physic2D,
                new AtomicBoolean(snapshot.isUpdated())
        );
    }

    public boolean isUpdated() {
        return updated.get();
    }

    public RenderUpdateSnapshot extractRenderData() {
        if (!updated.get()) return null;
        List<GameObject> updateObject = gameObjectByUUIDs.values().stream()
                .filter(go -> go.isDirty() && !go.isRemoved())
                .toList();
        if (updateObject.isEmpty()) return null;
        RenderUpdateSnapshot snapshot = new RenderUpdateSnapshot(updateObject);
        for (GameObject go : updateObject) {
            go.setDirty(false);
        }
        return snapshot;
    }
}
