package TheCellBeyond.internal;

import TheCellBeyond.GameObject;
import TheCellBeyond.Viewport;
import components.Component;
import org.joml.Vector2f;
import physic2d.Physic2D;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

public record DataSnapshot(
        Viewport viewport,
        HashMap<Integer, String> cachedIDs,
        HashMap<String, GameObject> gameObjectByUUIDs,
        List<GameObject> rootGameObjects,
        Map<String, Component> componentsByUUID,
        Physic2D physic2D,
        AtomicBoolean updated
) {
    public DataSnapshot() {
        this(new Viewport(new Vector2f(0.0f)),
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

    /*
    public RenderingSnapshot extractRenderData() {
        if (!updated.get()) {

        }
    }
     */
}
