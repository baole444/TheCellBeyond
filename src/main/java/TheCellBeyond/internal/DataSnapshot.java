package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import components.Component;
import org.joml.Vector2f;
import physic2d.Physic2D;

import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Data snapshot of {@link scene.Scene}
 * @param viewport the viewport of the scene
 * @param gameObjects the list of game object in the scene (ordered)
 * @param cachedIDs the cache of object shader ids mapped by the object uuid
 * @param cachedObjectsByUUID the map of the game objects in the scene
 * @param physic2D the physic world of the scene
 * @param updated the update state of this snapshot
 */
public record DataSnapshot(
        Viewport viewport,
        List<GameObject> gameObjects,
        HashMap<Integer, UUID> cachedIDs,
        HashMap<UUID, GameObject> cachedObjectsByUUID,
        Physic2D physic2D,
        AtomicBoolean updated
) {
    /**
     * Compact constructure ensure data is never null.
     * @param viewport the viewport of the scene
     * @param gameObjects the list of game object in the scene (ordered)
     * @param cachedIDs the cache of object shader ids mapped by the object uuid
     * @param cachedObjectsByUUID the map of the game objects in the scene
     * @param physic2D the physic world of the scene
     * @param updated the update state of this snapshot
     */
    public DataSnapshot {
        if (viewport == null) viewport = new Viewport(new Vector2f());
        if (gameObjects == null) gameObjects = new CopyOnWriteArrayList<>();
        if (cachedIDs == null) cachedIDs = new HashMap<>();
        if (physic2D == null) physic2D = new Physic2D();
        if (updated == null) updated = new AtomicBoolean(false);
    }

    /**
     * Create a blank data snapshot.
     */
    public DataSnapshot() {
        this(new Viewport(new Vector2f()), new ArrayList<>(),
                new HashMap<>(), new HashMap<>(),
                new Physic2D(), new AtomicBoolean(false)
        );
    }

    /**
     * Check if the data snapshot is updated or not.
     * @return true if updated
     */
    public boolean isUpdated() {
        return updated.get();
    }

    /**
     * Extract dirty object from this snapshot for rendering.
     * @return a rendering snapshot
     */
    public RenderUpdateSnapshot extractRenderData() {
        if (!updated.get()) return null;
        List<GameObject> updateObject = cachedObjectsByUUID.values().stream()
                .filter(go -> go.isDirty() && !go.isRemoved())
                .toList();
        if (updateObject.isEmpty()) return null;
        RenderUpdateSnapshot snapshot = new RenderUpdateSnapshot(updateObject);
        updateObject.forEach(go -> go.setDirty(false));
        return snapshot;
    }

    /**
     * Remove a game object from the data snapshot.
     * @param go the object to remove
     */
    public void removeObject(GameObject go) {
        if (go == null) return;
        cachedIDs.remove(go.getUID());
        cachedObjectsByUUID.remove(go.getUUID());
        gameObjects.remove(go);
        physic2D.destroyObject(go);
    }

    /**
     * Clear the data inside this snapshot.
     */
    public void clear() {
        gameObjects.clear();
        cachedIDs.clear();
        cachedObjectsByUUID.clear();
    }
}
